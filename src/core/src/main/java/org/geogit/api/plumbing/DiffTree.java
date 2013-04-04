/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Compares the content and metadata links of blobs found via two tree objects on the repository's
 * {@link ObjectDatabase}
 */
public class DiffTree extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private StagingDatabase objectDb;

    private String path;

    private String oldRefSpec;

    private String newRefSpec;

    private boolean reportTrees;

    /**
     * Constructs a new instance of the {@code DiffTree} operation with the given parameters.
     * 
     * @param objectDb the repository object database
     */
    @Inject
    public DiffTree(StagingDatabase objectDb) {
        this.objectDb = objectDb;
    }

    /**
     * @param oldRefSpec the ref that points to the "old" version
     * @return {@code this}
     */
    public DiffTree setOldVersion(String oldRefSpec) {
        this.oldRefSpec = oldRefSpec;
        return this;
    }

    /**
     * @param newRefSpec the ref that points to the "new" version
     * @return {@code this}
     */
    public DiffTree setNewVersion(String newRefSpec) {
        this.newRefSpec = newRefSpec;
        return this;
    }

    /**
     * @param oldTreeId the {@link ObjectId} of the "old" tree
     * @return {@code this}
     */
    public DiffTree setOldTree(ObjectId oldTreeId) {
        this.oldRefSpec = oldTreeId.toString();
        return this;
    }

    /**
     * @param newTreeId the {@link ObjectId} of the "new" tree
     * @return {@code this}
     */
    public DiffTree setNewTree(ObjectId newTreeId) {
        this.newRefSpec = newTreeId.toString();
        return this;
    }

    /**
     * @param path the path filter to use during the diff operation
     * @return {@code this}
     */
    public DiffTree setFilterPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Finds differences between the two specified trees.
     * 
     * @return an iterator to a set of differences between the two trees
     * @see DiffEntry
     */
    @Override
    public Iterator<DiffEntry> call() throws IllegalArgumentException {
        checkNotNull(oldRefSpec, "old version not specified");
        checkNotNull(newRefSpec, "new version not specified");

        final RevTree oldTree;
        final RevTree newTree;

        if (!oldRefSpec.equals(ObjectId.NULL.toString())) {
            final Optional<ObjectId> oldTreeId = command(ResolveTreeish.class).setTreeish(
                    oldRefSpec).call();
            checkArgument(oldTreeId.isPresent(), oldRefSpec + " did not resolve to a tree");
            oldTree = command(RevObjectParse.class).setObjectId(oldTreeId.get())
                    .call(RevTree.class).or(RevTree.EMPTY);
        } else {
            oldTree = RevTree.EMPTY;
        }

        if (!newRefSpec.equals(ObjectId.NULL.toString())) {
            final Optional<ObjectId> newTreeId = command(ResolveTreeish.class).setTreeish(
                    newRefSpec).call();
            checkArgument(newTreeId.isPresent(), newRefSpec + " did not resolve to a tree");
            newTree = command(RevObjectParse.class).setObjectId(newTreeId.get())
                    .call(RevTree.class).or(RevTree.EMPTY);
        } else {
            newTree = RevTree.EMPTY;
        }

        DiffTreeWalk treeWalk = new DiffTreeWalk(objectDb, oldTree, newTree);
        treeWalk.addFilter(this.path);
        treeWalk.setReportTrees(reportTrees);
        return treeWalk.get();
    }

    /**
     * @param reportTrees
     * @return
     */
    public DiffTree setReportTrees(boolean reportTrees) {
        this.reportTrees = reportTrees;
        return this;
    }
}
