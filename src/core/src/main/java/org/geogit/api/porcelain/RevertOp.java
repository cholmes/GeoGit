/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.CommitBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * Given one or more existing commits, revert the changes that the related patches introduce, and
 * record some new commits that record them. This requires your working tree to be clean (no
 * modifications from the HEAD commit).
 * 
 * <b>NOTE:</b> so far we don't have the ability to merge non conflicting changes. Instead, the diff
 * list we get acts on whole objects, so this operation will not revert feature changes if that
 * feature has been modified on both branches.
 */
public class RevertOp extends AbstractGeoGitOp<Boolean> {

    private List<ObjectId> commits;

    private Repository repository;

    private StagingArea index;

    private WorkingTree workTree;

    private Platform platform;

    /**
     * Constructs a new {@code RevertOp} using the specified parameters.
     * 
     * @param repository the repository to use
     * @param index the staging area
     * @param workTree the working tree
     * @param platform the platform to use
     */
    @Inject
    public RevertOp(Repository repository, StagingArea index, WorkingTree workTree,
            Platform platform) {
        this.repository = repository;
        this.index = index;
        this.workTree = workTree;
        this.platform = platform;
    }

    /**
     * Adds a commit to revert.
     * 
     * @param onto a supplier for the commit id
     * @return {@code this}
     */
    public RevertOp addCommit(final Supplier<ObjectId> commit) {
        Preconditions.checkNotNull(commit);

        if (this.commits == null) {
            this.commits = new ArrayList<ObjectId>();
        }
        this.commits.add(commit.get());
        return this;
    }

    /**
     * Executes the revert operation.
     * 
     * @return always {@code true}
     */
    @Override
    public Boolean call() {

        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't revert.");
        Preconditions.checkState(currHead.get() instanceof SymRef,
                "Can't revert from detached HEAD");
        final SymRef headRef = (SymRef) currHead.get();
        Preconditions.checkState(!headRef.getObjectId().equals(ObjectId.NULL),
                "HEAD has no history.");
        final String currentBranch = headRef.getTarget();

        // count staged and unstaged changes
        long staged = index.countStaged(null);
        long unstaged = workTree.countUnstaged(null);
        Preconditions.checkState((staged == 0 && unstaged == 0),
                "You must have a clean working tree and index to perform a revert.");

        getProgressListener().started();

        ObjectId revertHead = headRef.getObjectId();

        for (ObjectId commitId : commits) {
            Preconditions.checkState(repository.commitExists(commitId),
                    "Commit was not found in the repsoitory: " + commitId.toString());

            final RevCommit headCommit = repository.getCommit(revertHead);
            final RevCommit commit = repository.getCommit(commitId);

            ObjectId parentCommitId = ObjectId.NULL;
            if (commit.getParentIds().size() > 0) {
                parentCommitId = commit.getParentIds().get(0);
            }
            ObjectId parentTreeId = ObjectId.NULL;
            if (repository.commitExists(parentCommitId)) {
                parentTreeId = repository.getCommit(parentCommitId).getTreeId();
            }

            // get changes (in reverse)
            Iterator<DiffEntry> diff = command(DiffTree.class).setNewTree(parentTreeId)
                    .setOldTree(commit.getTreeId()).setReportTrees(true).call();

            final RevTree headTree = repository.getTree(headCommit.getTreeId());

            // filter out features that were changed after the commit
            final Iterator<DiffEntry> filtered = Iterators.filter(diff, new Predicate<DiffEntry>() {

                @Override
                public boolean apply(@Nullable DiffEntry input) {
                    // Find the latest in the tree
                    if (input.oldObjectId().equals(ObjectId.NULL)) {
                        // Feature was deleted
                        Optional<NodeRef> node = command(FindTreeChild.class)
                                .setChildPath(input.newPath()).setIndex(true).setParent(headTree)
                                .call();
                        // make sure it is still deleted
                        return !node.isPresent();
                    } else {
                        // Feature was added or modified
                        Optional<NodeRef> node = command(FindTreeChild.class)
                                .setChildPath(input.oldPath()).setIndex(true).setParent(headTree)
                                .call();
                        // Make sure it wasn't changed
                        return node.isPresent()
                                && node.get().getNode().getObjectId().equals(input.oldObjectId());

                    }
                }
            });

            // stage the reverted changes
            index.stage(subProgress(1.f / commits.size()), filtered, 0);

            // write new tree
            ObjectId newTreeId = command(WriteTree.class).call();
            long timestamp = platform.currentTimeMillis();
            String committerName = resolveCommitter();
            String committerEmail = resolveCommitterEmail();
            // Create new commit
            CommitBuilder builder = new CommitBuilder();
            builder.setParentIds(Arrays.asList(headCommit.getId()));
            builder.setTreeId(newTreeId);
            builder.setCommitterTimestamp(timestamp);
            builder.setMessage("Revert of commit '" + commitId.toString() + "'");
            builder.setCommitter(committerName);
            builder.setCommitterEmail(committerEmail);
            builder.setAuthor(committerName);
            builder.setAuthorEmail(committerEmail);
            // builder.setCommitterTimestamp(timestamp);
            // builder.setCommitterTimeZoneOffset(TimeZone.getDefault().getOffset(timestamp));
            // builder.setAuthorTimestamp(timestamp);
            // builder.setAuthorTimeZoneOffset(TimeZone.getDefault().getOffset(timestamp));

            RevCommit newCommit = builder.build();
            repository.getObjectDatabase().put(newCommit);

            revertHead = newCommit.getId();

            command(UpdateRef.class).setName(currentBranch).setNewValue(revertHead).call();
            command(UpdateSymRef.class).setName(Ref.HEAD).setNewValue(currentBranch).call();

            repository.getWorkingTree().updateWorkHead(newTreeId);
            repository.getIndex().updateStageHead(newTreeId);

        }

        getProgressListener().complete();

        return true;
    }

    private String resolveCommitter() {
        final String key = "user.name";
        Optional<String> name = command(ConfigGet.class).setName(key).call();

        checkState(
                name.isPresent(),
                "%s not found in config. Use geogit config [--global] %s <your name> to configure it.",
                key, key);

        return name.get();
    }

    private String resolveCommitterEmail() {
        final String key = "user.email";
        Optional<String> email = command(ConfigGet.class).setName(key).call();

        checkState(
                email.isPresent(),
                "%s not found in config. Use geogit config [--global] %s <your email> to configure it.",
                key, key);

        return email.get();
    }
}
