/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.DiffFeature;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.ConflictsReport;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.plumbing.diff.FeatureDiff;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Reports conflicts between changes introduced by two different histories. Given a commit and
 * another reference commit, it returns the set of changes from the common ancestor to the first
 * commit, classified according to whether they can or not be safely applied onto the reference
 * commit. Changes that will have no effect on the target commit are not included as unconflicted.
 */
public class ReportMergeConflictsOp extends AbstractGeoGitOp<ConflictsReport> {

    private RevCommit toMerge;

    private RevCommit mergeInto;

    @Inject
    public ReportMergeConflictsOp() {
    }

    /**
     * @param toMerge the commit with the changes to apply {@link RevCommit}
     */
    public ReportMergeConflictsOp setToMergeCommit(RevCommit toMerge) {
        this.toMerge = toMerge;
        return this;
    }

    /**
     * @param mergeInto the commit into which changes are to be merged {@link RevCommit}
     */
    public ReportMergeConflictsOp setMergeIntoCommit(RevCommit mergeInto) {
        this.mergeInto = mergeInto;
        return this;
    }

    @Override
    public ConflictsReport call() {

        Optional<RevCommit> ancestor = command(FindCommonAncestor.class).setLeft(toMerge)
                .setRight(mergeInto).call();
        Preconditions.checkState(ancestor.isPresent(), "No ancestor commit could be found.");

        Map<String, DiffEntry> mergeIntoDiffs = Maps.newHashMap();
        ConflictsReport report = new ConflictsReport();

        Iterator<DiffEntry> diffs = command(DiffTree.class).setOldTree(ancestor.get().getId())
                .setReportTrees(true).setNewTree(mergeInto.getId()).call();
        while (diffs.hasNext()) {
            DiffEntry diff = diffs.next();
            String path = diff.oldPath() == null ? diff.newPath() : diff.oldPath();
            mergeIntoDiffs.put(path, diff);
        }

        Iterator<DiffEntry> toMergeDiffs = command(DiffTree.class)
                .setOldTree(ancestor.get().getId()).setReportTrees(true)
                .setNewTree(toMerge.getId()).call();
        while (toMergeDiffs.hasNext()) {
            DiffEntry toMergeDiff = toMergeDiffs.next();
            String path = toMergeDiff.oldPath() == null ? toMergeDiff.newPath() : toMergeDiff
                    .oldPath();
            if (mergeIntoDiffs.containsKey(path)) {
                RevTree ancestorTree = command(RevObjectParse.class)
                        .setObjectId(ancestor.get().getTreeId()).call(RevTree.class).get();
                Optional<NodeRef> ancestorVersion = command(FindTreeChild.class).setChildPath(path)
                        .setParent(ancestorTree).call();
                ObjectId ancestorVersionId = ancestorVersion.isPresent() ? ancestorVersion.get()
                        .getNode().getObjectId() : ObjectId.NULL;
                ObjectId theirs = toMergeDiff.getNewObject() == null ? ObjectId.NULL : toMergeDiff
                        .getNewObject().objectId();
                DiffEntry mergeIntoDiff = mergeIntoDiffs.get(path);
                ObjectId ours = mergeIntoDiff.getNewObject() == null ? ObjectId.NULL
                        : mergeIntoDiff.getNewObject().objectId();
                if (!mergeIntoDiff.changeType().equals(toMergeDiff.changeType())) {
                    report.addConflict(new Conflict(path, ancestorVersionId, ours, theirs));
                    continue;
                }
                switch (toMergeDiff.changeType()) {
                case ADDED:
                    if (toMergeDiff.getNewObject().equals(mergeIntoDiff.getNewObject())) {
                        // report.addUnconflicted(toMergeDiff);
                    } else {
                        TYPE type = command(ResolveObjectType.class).setObjectId(
                                toMergeDiff.getNewObject().objectId()).call();
                        if (TYPE.TREE.equals(type)) {
                            boolean conflict = !toMergeDiff.getNewObject().getMetadataId()
                                    .equals(mergeIntoDiff.getNewObject().getMetadataId());
                            if (conflict) {
                                // In this case, we store the metadata id, not the element id
                                ancestorVersionId = ancestorVersion.isPresent() ? ancestorVersion
                                        .get().getMetadataId() : ObjectId.NULL;
                                ours = mergeIntoDiff.getNewObject().getMetadataId();
                                theirs = toMergeDiff.getNewObject().getMetadataId();
                                report.addConflict(new Conflict(path, ancestorVersionId, ours,
                                        theirs));
                            }
                            // if the metadata ids match, it means both branches have added the same
                            // tree, maybe with different content, but there is no need to do
                            // anything. The correct tree is already there and the merge can be run
                            // safely, so we do not added neither as a conflicted change nor as an
                            // unconflicted on
                        } else {
                            report.addConflict(new Conflict(path, ancestorVersionId, ours, theirs));
                        }
                    }
                    break;
                case REMOVED:
                    // removed by both histories => no conflict
                    // report.addUnconflicted(toMergeDiff);
                    break;
                case MODIFIED:
                    TYPE type = command(ResolveObjectType.class).setObjectId(
                            toMergeDiff.getNewObject().objectId()).call();
                    if (TYPE.TREE.equals(type)) {
                        boolean conflict = !toMergeDiff.getNewObject().getMetadataId()
                                .equals(mergeIntoDiff.getNewObject().getMetadataId());
                        if (conflict) {
                            // In this case, we store the metadata id, not the element id
                            ancestorVersionId = ancestorVersion.isPresent() ? ancestorVersion.get()
                                    .getMetadataId() : ObjectId.NULL;
                            ours = mergeIntoDiff.getNewObject().getMetadataId();
                            theirs = toMergeDiff.getNewObject().getMetadataId();
                            report.addConflict(new Conflict(path, ancestorVersionId, ours, theirs));
                        }
                    } else {
                        FeatureDiff toMergeFeatureDiff = command(DiffFeature.class)
                                .setOldVersion(Suppliers.ofInstance(toMergeDiff.getOldObject()))
                                .setNewVersion(Suppliers.ofInstance(toMergeDiff.getNewObject()))
                                .call();
                        FeatureDiff mergeIntoFeatureDiff = command(DiffFeature.class)
                                .setOldVersion(Suppliers.ofInstance(mergeIntoDiff.getOldObject()))
                                .setNewVersion(Suppliers.ofInstance(mergeIntoDiff.getNewObject()))
                                .call();
                        if (toMergeFeatureDiff.conflicts(mergeIntoFeatureDiff)) {
                            report.addConflict(new Conflict(path, ancestorVersionId, ours, theirs));
                        } else {
                            if (!toMergeFeatureDiff.equals(mergeIntoFeatureDiff)) {
                                report.addUnconflicted(toMergeDiff);
                            }
                        }
                    }
                    break;
                }
            } else {
                // If the element is a tree, not a feature, it might be a conflict even if the other
                // branch has not modified it.
                // If we are removing the tree, we have to make sure that there are no features
                // modified in the other branch under it.
                if (ChangeType.REMOVED.equals(toMergeDiff.changeType())) {
                    TYPE type = command(ResolveObjectType.class).setObjectId(
                            toMergeDiff.oldObjectId()).call();
                    if (TYPE.TREE.equals(type)) {
                        String parentPath = toMergeDiff.oldPath();
                        Set<Entry<String, DiffEntry>> entries = mergeIntoDiffs.entrySet();
                        boolean conflict = false;
                        for (Entry<String, DiffEntry> entry : entries) {
                            if (entry.getKey().startsWith(parentPath)) {
                                if (!ChangeType.REMOVED.equals(entry.getValue().changeType())) {
                                    RevTree ancestorTree = command(RevObjectParse.class)
                                            .setObjectId(ancestor.get().getTreeId())
                                            .call(RevTree.class).get();
                                    Optional<NodeRef> ancestorVersion = command(FindTreeChild.class)
                                            .setChildPath(path).setParent(ancestorTree).call();
                                    ObjectId ancestorVersionId = ancestorVersion.isPresent() ? ancestorVersion
                                            .get().getNode().getObjectId()
                                            : ObjectId.NULL;
                                    ObjectId theirs = toMergeDiff.getNewObject() == null ? ObjectId.NULL
                                            : toMergeDiff.getNewObject().objectId();
                                    String oursRefSpec = mergeInto.getId().toString() + ":"
                                            + parentPath;
                                    Optional<ObjectId> ours = command(RevParse.class).setRefSpec(
                                            oursRefSpec).call();
                                    report.addConflict(new Conflict(path, ancestorVersionId, ours
                                            .get(), theirs));
                                    conflict = true;
                                    break;
                                }
                            }
                        }
                        if (!conflict) {
                            report.addUnconflicted(toMergeDiff);
                        }
                    }
                } else {
                    report.addUnconflicted(toMergeDiff);
                }
            }

        }

        return report;

    }
}
