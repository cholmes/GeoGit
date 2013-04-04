/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.geogit.api.Bucket;
import org.geogit.api.GeoGIT;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.porcelain.PushException;
import org.geogit.api.porcelain.PushException.StatusCode;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectInserter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

/**
 * An implementation of a remote repository that exists on the local machine.
 * 
 * @see IRemoteRepo
 */
public class LocalRemoteRepo implements IRemoteRepo {

    private GeoGIT remoteGeoGit;

    private Injector injector;

    private File workingDirectory;

    private Queue<ObjectId> commitQueue;

    private List<ObjectId> touchedIds;

    /**
     * Constructs a new {@code LocalRemoteRepo} with the given parameters.
     * 
     * @param injector the Guice injector for the new repository
     * @param workingDirectory the directory of the remote repository
     */
    public LocalRemoteRepo(Injector injector, File workingDirectory) {
        this.injector = injector;
        this.workingDirectory = workingDirectory;
        this.commitQueue = new LinkedList<ObjectId>();
    }

    /**
     * @param geogit manually set a geogit for this remote repository
     */
    public void setGeoGit(GeoGIT geogit) {
        this.remoteGeoGit = geogit;
    }

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void open() throws IOException {
        if (remoteGeoGit == null) {
            remoteGeoGit = new GeoGIT(injector, workingDirectory);
            remoteGeoGit.getRepository();
        }

    }

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        remoteGeoGit.close();

    }

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    @Override
    public Ref headRef() {
        final Optional<Ref> currHead = remoteGeoGit.command(RefParse.class).setName(Ref.HEAD)
                .call();
        Preconditions.checkState(currHead.isPresent(), "Remote repository has no HEAD.");
        return currHead.get();
    }

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    @Override
    public ImmutableSet<Ref> listRefs(final boolean getHeads, final boolean getTags) {
        Predicate<Ref> filter = new Predicate<Ref>() {
            @Override
            public boolean apply(Ref input) {
                boolean keep = false;
                if (getHeads) {
                    keep = input.getName().startsWith(Ref.HEADS_PREFIX);
                }
                if (getTags) {
                    keep = keep || input.getName().startsWith(Ref.TAGS_PREFIX);
                }
                return keep;
            }
        };
        return remoteGeoGit.command(ForEachRef.class).setFilter(filter).call();
    }

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param localRepository the repository to add new objects to
     * @param ref the remote ref that points to new commit data
     */
    @Override
    public void fetchNewData(Repository localRepository, Ref ref) {
        touchedIds = new LinkedList<ObjectId>();
        ObjectInserter objectInserter = localRepository.newObjectInserter();
        commitQueue.clear();
        commitQueue.add(ref.getObjectId());
        try {
            while (!commitQueue.isEmpty()) {
                walkCommit(commitQueue.remove(), remoteGeoGit.getRepository(), localRepository,
                        objectInserter);
            }
        } catch (Exception e) {
            for (ObjectId oid : touchedIds) {
                localRepository.getObjectDatabase().delete(oid);
            }
            Throwables.propagate(e);
        } finally {
            touchedIds.clear();
            touchedIds = null;
        }
    }

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     */
    @Override
    public void pushNewData(Repository localRepository, Ref ref) throws PushException {
        pushNewData(localRepository, ref, ref.getName());
    }

    /**
     * Push all new objects from the specified {@link Ref} to the given refspec.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     * @param refspec the refspec to push to
     */
    @Override
    public void pushNewData(Repository localRepository, Ref ref, String refspec)
            throws PushException {
        checkPush(localRepository, ref, refspec);
        touchedIds = new LinkedList<ObjectId>();
        ObjectInserter objectInserter = remoteGeoGit.getRepository().newObjectInserter();
        commitQueue.clear();
        commitQueue.add(ref.getObjectId());
        try {
            while (!commitQueue.isEmpty()) {
                walkCommit(commitQueue.remove(), localRepository, remoteGeoGit.getRepository(),
                        objectInserter);
            }
            Ref updatedRef = remoteGeoGit.command(UpdateRef.class).setName(refspec)
                    .setNewValue(ref.getObjectId()).call().get();

            Ref remoteHead = headRef();
            if (remoteHead instanceof SymRef) {
                if (((SymRef) remoteHead).getTarget().equals(updatedRef.getName())) {
                    remoteGeoGit.command(UpdateSymRef.class).setName(Ref.HEAD)
                            .setNewValue(ref.getName()).call();
                    RevCommit commit = remoteGeoGit.getRepository().getCommit(ref.getObjectId());
                    remoteGeoGit.getRepository().getWorkingTree()
                            .updateWorkHead(commit.getTreeId());
                    remoteGeoGit.getRepository().getIndex().updateStageHead(commit.getTreeId());
                }
            }
        } catch (Exception e) {
            for (ObjectId oid : touchedIds) {
                remoteGeoGit.getRepository().getObjectDatabase().delete(oid);
            }
            Throwables.propagate(e);
        } finally {
            touchedIds.clear();
            touchedIds = null;
        }
    }

    private void checkPush(Repository localRepository, Ref ref, String refspec)
            throws PushException {
        Optional<Ref> remoteRef = remoteGeoGit.command(RefParse.class).setName(refspec).call();
        if (remoteRef.isPresent()) {
            if (remoteRef.get().getObjectId().equals(ref.getObjectId())) {
                // The branches are equal, no need to push.
                throw new PushException(StatusCode.NOTHING_TO_PUSH);
            } else if (localRepository.blobExists(remoteRef.get().getObjectId())) {
                RevCommit leftCommit = localRepository.getCommit(remoteRef.get().getObjectId());
                RevCommit rightCommit = localRepository.getCommit(ref.getObjectId());
                Optional<RevCommit> ancestor = localRepository.command(FindCommonAncestor.class)
                        .setLeft(leftCommit).setRight(rightCommit).call();
                if (!ancestor.isPresent()) {
                    // There is no common ancestor, a push will overwrite history
                    throw new PushException(StatusCode.REMOTE_HAS_CHANGES);
                } else if (ancestor.get().getId().equals(ref.getObjectId())) {
                    // My last commit is the common ancestor, the remote already has my data.
                    throw new PushException(StatusCode.NOTHING_TO_PUSH);
                } else if (!ancestor.get().getId().equals(remoteRef.get().getObjectId())) {
                    // The remote branch's latest commit is not my ancestor, a push will cause a
                    // loss of history.
                    throw new PushException(StatusCode.REMOTE_HAS_CHANGES);
                }
            } else {
                // The remote has data that I do not, a push will cause this data to be lost.
                throw new PushException(StatusCode.REMOTE_HAS_CHANGES);
            }
        }
    }

    /**
     * Delete the given refspec from the remote repository.
     * 
     * @param refspec the refspec to delete
     */
    @Override
    public void deleteRef(String refspec) {
        remoteGeoGit.command(UpdateRef.class).setName(refspec).setDelete(true).call();
    }

    private void walkCommit(ObjectId commitId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(commitId)) {
            return;
        }

        Optional<RevObject> object = from.command(RevObjectParse.class).setObjectId(commitId)
                .call();
        if (object.isPresent() && object.get().getType().equals(TYPE.COMMIT)) {
            RevCommit commit = (RevCommit) object.get();
            walkTree(commit.getTreeId(), from, to, objectInserter);

            objectInserter.insert(commit);
            touchedIds.add(commitId);
            for (ObjectId parentCommit : commit.getParentIds()) {
                commitQueue.add(parentCommit);
            }
        }
    }

    private void walkTree(ObjectId treeId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(treeId)) {
            return;
        }

        Optional<RevObject> object = from.command(RevObjectParse.class).setObjectId(treeId).call();
        if (object.isPresent() && object.get().getType().equals(TYPE.TREE)) {
            RevTree tree = (RevTree) object.get();

            objectInserter.insert(tree);
            touchedIds.add(treeId);
            // walk subtrees
            if (tree.buckets().isPresent()) {
                for (Bucket bucket : tree.buckets().get().values()) {
                    walkTree(bucket.id(), from, to, objectInserter);
                }
            } else {
                // get new objects
                for (Iterator<Node> children = tree.children(); children.hasNext();) {
                    Node ref = children.next();
                    moveObject(ref.getObjectId(), from, to, objectInserter);
                    ObjectId metadataId = ref.getMetadataId().or(ObjectId.NULL);
                    if (!metadataId.isNull()) {
                        moveObject(metadataId, from, to, objectInserter);
                    }
                }
            }
        }
    }

    private void moveObject(ObjectId objectId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(objectId)) {
            return;
        }

        Optional<RevObject> childObject = from.command(RevObjectParse.class).setObjectId(objectId)
                .call();
        if (childObject.isPresent()) {
            RevObject revObject = childObject.get();
            if (TYPE.TREE.equals(revObject.getType())) {
                walkTree(objectId, from, to, objectInserter);
            }
            objectInserter.insert(revObject);
            touchedIds.add(objectId);
        }
    }
}
