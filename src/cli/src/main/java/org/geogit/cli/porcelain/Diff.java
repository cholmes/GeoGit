/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Shows changes between commits, commits and working tree, etc.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit diff [-- <path>...]}: compare working tree and index
 * <li> {@code geogit diff <commit> [-- <path>...]}: compare the working tree with the given commit
 * <li> {@code geogit diff --cached [-- <path>...]}: compare the index with the HEAD commit
 * <li> {@code geogit diff --cached <commit> [-- <path>...]}: compare the index with the given commit
 * <li> {@code geogit diff <commit1> <commit2> [-- <path>...]}: compare {@code commit1} with
 * {@code commit2}, where {@code commit1} is the eldest or left side of the diff.
 * </ul>
 * 
 * @see DiffOp
 */
@Parameters(commandNames = "diff", commandDescription = "Show changes between commits, commit and working tree, etc")
public class Diff extends AbstractCommand implements CLICommand {

    @Parameter(description = "[<commit> [<commit>]] [-- <path>...]", arity = 2)
    private List<String> refSpec = Lists.newArrayList();

    @Parameter(names = "--", hidden = true, variableArity = true)
    private List<String> paths = Lists.newArrayList();

    @Parameter(names = "--cached", description = "compares the specified tree (commit, branch, etc) and the staging area")
    private boolean cached;

    @Parameter(names = "--summary", description = "List only summary of changes")
    private boolean summary;

    @Parameter(names = "--nogeom", description = "Do not show detailed coordinate changes in geometries")
    private boolean nogeom;

    /**
     * Executes the diff command with the specified options.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (refSpec.size() > 2) {
            cli.getConsole().println("Commit list is too long :" + refSpec);
            return;
        }

        if (nogeom && summary) {
            cli.getConsole().println("Only one printing mode allowed");
            return;
        }

        GeoGIT geogit = cli.getGeogit();

        DiffOp diff = geogit.command(DiffOp.class);

        String oldVersion = resolveOldVersion();
        String newVersion = resolveNewVersion();

        diff.setOldVersion(oldVersion).setNewVersion(newVersion).setCompareIndex(cached);

        Iterator<DiffEntry> entries;
        if (paths.isEmpty()) {
            entries = diff.setProgressListener(cli.getProgressListener()).call();
        } else {
            entries = Iterators.emptyIterator();
            for (String path : paths) {
                Iterator<DiffEntry> moreEntries = diff.setFilter(path)
                        .setProgressListener(cli.getProgressListener()).call();
                entries = Iterators.concat(entries, moreEntries);
            }
        }

        if (!entries.hasNext()) {
            cli.getConsole().println("No differences found");
            return;
        }

        DiffPrinter printer;
        if (summary) {
            printer = new SummaryDiffPrinter();
        } else {
            printer = new FullDiffPrinter(nogeom, false);
        }

        DiffEntry entry;
        while (entries.hasNext()) {
            entry = entries.next();
            printer.print(geogit, cli.getConsole(), entry);
        }
    }

    private String resolveOldVersion() {
        return refSpec.size() > 0 ? refSpec.get(0) : null;
    }

    private String resolveNewVersion() {
        return refSpec.size() > 1 ? refSpec.get(1) : null;
    }

}
