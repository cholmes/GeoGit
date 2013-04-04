/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Removes a feature or a tree from the working tree and index
 * 
 */
@CanRunDuringConflict
public class RemoveOp extends AbstractGeoGitOp<WorkingTree> {

    private List<String> pathsToRemove;

    @Inject
    public RemoveOp() {
        this.pathsToRemove = new ArrayList<String>();
    }

    /**
     * @param path a path to remove
     * @return {@code this}
     */
    public RemoveOp addPathToRemove(final String path) {
        pathsToRemove.add(path);
        return this;
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public WorkingTree call() {

        // Check that all paths are valid and exist
        for (String pathToRemove : pathsToRemove) {
            NodeRef.checkValidPath(pathToRemove);
            Optional<NodeRef> node;
            node = command(FindTreeChild.class).setParent(getWorkTree().getTree()).setIndex(true)
                    .setChildPath(pathToRemove).call();
            Preconditions.checkArgument(node.isPresent(),
                    "pathspec '%s' did not match any feature or tree", pathToRemove);
        }

        // separate trees from features an delete accordingly
        for (String pathToRemove : pathsToRemove) {
            Optional<NodeRef> node = command(FindTreeChild.class)
                    .setParent(getWorkTree().getTree()).setIndex(true).setChildPath(pathToRemove)
                    .call();
            switch (node.get().getType()) {
            case TREE:
                getWorkTree().delete(pathToRemove);
                break;
            case FEATURE:
                String parentPath = NodeRef.parentPath(pathToRemove);
                String name = node.get().name();
                getWorkTree().delete(parentPath, name);
                break;
            default:
                break;
            }

            final long numChanges = getWorkTree().countUnstaged(pathToRemove);
            Iterator<DiffEntry> unstaged = getWorkTree().getUnstaged(pathToRemove);
            getIndex().stage(getProgressListener(), unstaged, numChanges);
        }

        return getWorkTree();
    }

}
