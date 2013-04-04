/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.history.internal;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamException;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.io.Closeables;

/**
 *
 */
public class HistoryDownloader {

    private final long initialChangeset;

    private final long finalChangeset;

    private long currChangeset;

    private final ChangesetDownloader downloader;

    private boolean started;

    private final boolean preserveFiles;

    /**
     * @param osmAPIUrl api url, e.g. {@code http://api.openstreetmap.org/api/0.6},
     *        {@code file:/path/to/downloaded/changesets}
     * @param initialChangeset initial changeset id
     * @param finalChangeset final changeset id
     * @param preserveFiles
     */
    public HistoryDownloader(final String osmAPIUrl, final File downloadFolder,
            long initialChangeset, long finalChangeset, ExecutorService executor,
            boolean preserveFiles) {

        checkArgument(initialChangeset > 0 && initialChangeset <= finalChangeset);

        this.initialChangeset = initialChangeset;
        this.finalChangeset = finalChangeset;
        this.preserveFiles = preserveFiles;
        this.downloader = new ChangesetDownloader(osmAPIUrl, downloadFolder, executor);
        currChangeset = this.initialChangeset;
    }

    private BlockingQueue<Supplier<Optional<Changeset>>> changesetsQueue = new ArrayBlockingQueue<Supplier<Optional<Changeset>>>(
            100);

    /**
    *
    */
    private class ChangeSetSupplier implements Supplier<Optional<Changeset>> {

        private Supplier<Optional<File>> changesetFile;

        private Supplier<Optional<File>> changesFile;

        /**
         * @param changesetFile
         * @param changesFile
         */
        public ChangeSetSupplier(Supplier<Optional<File>> changesetFile,
                Supplier<Optional<File>> changesFile) {
            this.changesetFile = changesetFile;
            this.changesFile = changesFile;
        }

        @Override
        public Optional<Changeset> get() {
            Optional<Changeset> changeset = parseChangeset(changesetFile);

            if (changeset.isPresent()) {
                Changeset actual = changeset.get();
                Supplier<Iterator<Change>> changes = new ChangesSupplier(changesFile);
                actual.setChanges(changes);
            }
            return changeset;
        }
    }

    /**
    *
    */
    private class ChangesSupplier implements Supplier<Iterator<Change>> {

        private Supplier<Optional<File>> changesFile;

        /**
         * @param changesFile2
         */
        public ChangesSupplier(Supplier<Optional<File>> changesFile) {
            this.changesFile = changesFile;
        }

        @Override
        public Iterator<Change> get() {
            return parseChanges(changesFile);
        }

    }

    /**
     * @return the next available changeset, or absent if reached the last one
     * @throws IOException
     * @throws InterruptedException
     */
    public Optional<Changeset> fetchNextChangeset() {
        if (!started) {
            started = true;
            Thread runner = new Thread() {
                @Override
                public void run() {
                    for (long changeset = initialChangeset; changeset <= finalChangeset; changeset++) {
                        Supplier<Optional<File>> changesetFile = downloader
                                .fetchChangeset(changeset);
                        Supplier<Optional<File>> changesFile = downloader.fetchChanges(changeset);

                        Supplier<Optional<Changeset>> supplier;
                        supplier = new ChangeSetSupplier(changesetFile, changesFile);
                        try {
                            // put the element on the queue, blocking until space is available if
                            // necessary
                            changesetsQueue.put(supplier);
                        } catch (InterruptedException e) {
                            System.out.println(Thread.currentThread().getName()
                                    + " interrupted. Exiting gracefully. "
                                    + "No more changes will be queued.");
                        }
                    }
                }
            };
            runner.setName("OSM History download consumer");
            runner.setDaemon(true);
            runner.start();
        }

        Optional<Changeset> next = Optional.absent();

        while (currChangeset <= finalChangeset && !next.isPresent()) {
            Supplier<Optional<Changeset>> cs;
            try {
                cs = changesetsQueue.poll(30, TimeUnit.SECONDS);
                if (cs == null) {
                    String msg = "Waited for next changeset for 30 seconds, aborting";
                    System.err.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
            currChangeset++;
            try {
                next = cs.get();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof FileNotFoundException) {
                    continue;
                }
                throw Throwables.propagate(e.getCause());
            }
        }

        return next;
    }

    private Optional<Changeset> parseChangeset(Supplier<Optional<File>> file) {

        Optional<File> changesetFile;
        try {
            changesetFile = file.get();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                Optional.absent();
            }
            throw Throwables.propagate(e.getCause());
        }

        if (!changesetFile.isPresent()) {
            Optional.absent();
        }
        Changeset changeset = null;

        InputStream stream = null;
        try {
            final File actualFile = changesetFile.get();
            stream = new BufferedInputStream(new FileInputStream(actualFile), 4096);
            Optional<Changeset> cs;
            try {
                cs = new ChangesetScanner().parse(stream);
            } catch (XMLStreamException e) {
                throw Throwables.propagate(e);
            }
            if (cs.isPresent()) {
                changeset = cs.get();
                if (!preserveFiles) {
                    actualFile.delete();
                }
            }
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        } finally {
            Closeables.closeQuietly(stream);
        }

        return Optional.fromNullable(changeset);
    }

    private Iterator<Change> parseChanges(Supplier<Optional<File>> file) {

        ChangesetContentsScanner scanner = new ChangesetContentsScanner();

        final Optional<File> changesFile;
        try {
            changesFile = file.get();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FileNotFoundException) {
                return Iterators.emptyIterator();
            }
            throw Throwables.propagate(e);
        }
        if (!changesFile.isPresent()) {
            return Iterators.emptyIterator();
        }
        final File actualFile = changesFile.get();
        final InputStream stream = openStream(actualFile);
        final Iterator<Change> changes;
        try {
            changes = scanner.parse(stream);
        } catch (XMLStreamException e) {
            throw Throwables.propagate(e);
        }

        return new AbstractIterator<Change>() {
            @Override
            protected Change computeNext() {
                if (!changes.hasNext()) {
                    Closeables.closeQuietly(stream);
                    if (!preserveFiles) {
                        actualFile.delete();
                        actualFile.getParentFile().delete();
                    }
                    return super.endOfData();
                }
                return changes.next();
            }
        };
    }

    private InputStream openStream(File file) {
        InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(file), 4096);
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        }
        return stream;
    }

}
