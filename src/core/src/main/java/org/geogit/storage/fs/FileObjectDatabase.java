/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerializingFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGit object database that utilizes the file system for the
 * storage of objects.
 * 
 * @see AbstractObjectDatabase
 */
public class FileObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private final Platform platform;

    private final String databaseName;

    private File dataRoot;

    private String dataRootPath;

    /**
     * Constructs a new {@code FileObjectDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public FileObjectDatabase(final Platform platform, final ObjectSerializingFactory serialFactory) {
        this(platform, "objects", serialFactory);
    }

    protected FileObjectDatabase(final Platform platform, final String databaseName,
            final ObjectSerializingFactory serialFactory) {
        super(serialFactory);
        checkNotNull(platform);
        checkNotNull(databaseName);
        this.platform = platform;
        this.databaseName = databaseName;
    }

    protected File getDataRoot() {
        return dataRoot;
    }

    protected String getDataRootPath() {
        return dataRootPath;
    }

    /**
     * @return true if the database is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return dataRoot != null;
    }

    /**
     * Opens the database for use by GeoGit.
     */
    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        final URL repoUrl = new ResolveGeogitDir(platform).call();
        if (repoUrl == null) {
            throw new IllegalStateException("Can't find geogit repository home");
        }

        try {
            dataRoot = new File(new File(repoUrl.toURI()), databaseName);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }

        if (!dataRoot.exists() && !dataRoot.mkdirs()) {
            throw new IllegalStateException("Can't create environment: "
                    + dataRoot.getAbsolutePath());
        }
        if (!dataRoot.isDirectory()) {
            throw new IllegalStateException("Environment but is not a directory: "
                    + dataRoot.getAbsolutePath());
        }
        if (!dataRoot.canWrite()) {
            throw new IllegalStateException("Environment is not writable: "
                    + dataRoot.getAbsolutePath());
        }
        dataRootPath = dataRoot.getAbsolutePath();
    }

    /**
     * Closes the database.
     */
    @Override
    public void close() {
        dataRoot = null;
        dataRootPath = null;
    }

    /**
     * Determines if the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to search for
     * @return true if the object exists, false otherwise
     */
    @Override
    public boolean exists(final ObjectId id) {
        File f = filePath(id);
        return f.exists();
    }

    @Override
    protected InputStream getRawInternal(ObjectId id, boolean failIfNotFound) {
        File f = filePath(id);
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            if (failIfNotFound) {
                throw Throwables.propagate(e);
            }
            return null;
        }
    }

    /**
     * @see org.geogit.storage.AbstractObjectDatabase#putInternal(org.geogit.api.ObjectId, byte[])
     */
    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData) {
        final File f = filePath(id);
        if (f.exists()) {
            return false;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(f);
        } catch (FileNotFoundException dirDoesNotExist) {
            final File parent = f.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new RuntimeException("Can't create " + parent.getAbsolutePath());
            }
            try {
                fileOutputStream = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                throw Throwables.propagate(e);
            }
        }
        try {
            fileOutputStream.write(rawData);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return true;
    }

    /**
     * Deletes the object with the provided {@link ObjectId id} from the database.
     * 
     * @param objectId the id of the object to delete
     * @return true if the object was deleted, false if it was not found
     */
    @Override
    public boolean delete(ObjectId objectId) {
        File filePath = filePath(objectId);
        boolean delete = filePath.delete();
        return delete;
    }

    private File filePath(final ObjectId id) {
        final String idName = id.toString();
        return filePath(idName);
    }

    private File filePath(final String objectId) {
        checkNotNull(objectId);
        checkArgument(objectId.length() > 4, "partial object id is too short");

        final char[] path1 = new char[2];
        final char[] path2 = new char[2];
        objectId.getChars(0, 2, path1, 0);
        objectId.getChars(2, 4, path2, 0);

        StringBuilder sb = new StringBuilder(dataRootPath);
        sb.append(File.separatorChar).append(path1).append(File.separatorChar).append(path2)
                .append(File.separatorChar).append(objectId);
        String filePath = sb.toString();
        return new File(filePath);
    }

    /**
     * Searches the database for {@link ObjectId}s that match the given partial id.
     * 
     * @param partialId the partial id to search for
     * @return a list of matching results
     */
    @Override
    public List<ObjectId> lookUp(final String partialId) {
        File parent = filePath(partialId).getParentFile();
        String[] list = parent.list();
        if (null == list) {
            return ImmutableList.of();
        }
        Builder<ObjectId> builder = ImmutableList.builder();
        for (String oid : list) {
            if (oid.startsWith(partialId)) {
                builder.add(ObjectId.valueOf(oid));
            }
        }
        return builder.build();
    }

    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException(
                "This method should not be called, we override lookUp(String) directly");
    }
}
