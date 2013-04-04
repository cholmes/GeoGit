/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

/**
 * Incorporates changes from a remote repository into the current branch.
 * 
 */
public class PullOp extends AbstractGeoGitOp<PullResult> {

    private boolean all;

    private boolean rebase;

    private Supplier<Optional<Remote>> remote;

    private List<String> refSpecs = new ArrayList<String>();

    /**
     * Constructs a new {@code PullOp}.
     */
    @Inject
    public PullOp() {
    }

    /**
     * @param all if {@code true}, pull from all remotes.
     * @return {@code this}
     */
    public PullOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param rebase if {@code true}, perform a rebase on the remote branch instead of a merge
     * @return {@code this}
     */
    public PullOp setRebase(final boolean rebase) {
        this.rebase = rebase;
        return this;
    }

    /**
     * @param refSpec the refspec of a remote branch
     * @return {@code this}
     */
    public PullOp addRefSpec(final String refSpec) {
        refSpecs.add(refSpec);
        return this;
    }

    /**
     * @param remoteName the name or URL of a remote repository to fetch from
     * @return {@code this}
     */
    public PullOp setRemote(final String remoteName) {
        Preconditions.checkNotNull(remoteName);
        return setRemote(command(RemoteResolve.class).setName(remoteName));
    }

    /**
     * @param remoteSupplier the remote repository to fetch from
     * @return {@code this}
     */
    public PullOp setRemote(Supplier<Optional<Remote>> remoteSupplier) {
        Preconditions.checkNotNull(remoteSupplier);
        remote = remoteSupplier;

        return this;
    }

    /**
     * Executes the pull operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public PullResult call() {

        if (remote == null) {
            setRemote("origin");
        }

        PullResult result = new PullResult();

        Optional<Remote> remoteRepo = remote.get();

        Preconditions.checkArgument(remoteRepo.isPresent(), "Remote could not be resolved.");
        getProgressListener().started();

        FetchResult fetchResult = command(FetchOp.class).addRemote(remote).setAll(all)
                .setProgressListener(subProgress(80.f)).call();

        result.setFetchResult(fetchResult);

        if (refSpecs.size() == 0) {
            // pull current branch
            final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
            Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't pull.");
            Preconditions.checkState(currHead.get() instanceof SymRef,
                    "Can't pull from detached HEAD");
            final SymRef headRef = (SymRef) currHead.get();
            final String currentBranch = Ref.localName(headRef.getTarget());

            refSpecs.add(currentBranch + ":" + currentBranch);
        }

        for (String refspec : refSpecs) {
            String[] refs = refspec.split(":");
            Preconditions.checkArgument(refs.length < 3,
                    "Invalid refspec, please use [+]<remoteref>[:<localref>].");

            boolean force = refspec.startsWith("+");
            String remoteref = refs[0].substring(force ? 1 : 0);
            Optional<Ref> sourceRef = findRemoteRef(remoteref);
            Preconditions.checkState(sourceRef.isPresent(),
                    "The remote reference could not be found.");

            String destinationref = "";
            if (refs.length == 2) {
                destinationref = refs[1];
            } else {
                // pull into current branch
                final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
                Preconditions.checkState(currHead.isPresent(),
                        "Repository has no HEAD, can't pull.");
                Preconditions.checkState(currHead.get() instanceof SymRef,
                        "Can't pull from detached HEAD");
                final SymRef headRef = (SymRef) currHead.get();
                destinationref = headRef.getTarget();
            }

            Optional<Ref> destRef = command(RefParse.class).setName(destinationref).call();
            if (destRef.isPresent()) {
                if (destRef.get().getObjectId().equals(sourceRef.get().getObjectId())) {
                    // Already up to date.
                    result.setOldRef(destRef.get());
                    result.setNewRef(destRef.get());
                    continue;
                }
                result.setOldRef(destRef.get());
                if (destRef.get().getObjectId().equals(ObjectId.NULL)) {
                    command(UpdateRef.class).setName(destRef.get().getName())
                            .setNewValue(sourceRef.get().getObjectId()).call();
                } else {
                    command(CheckoutOp.class).setSource(destinationref).call();
                    if (rebase) {
                        command(RebaseOp.class).setUpstream(
                                Suppliers.ofInstance(sourceRef.get().getObjectId())).call();
                    } else {
                        command(MergeOp.class).addCommit(
                                Suppliers.ofInstance(sourceRef.get().getObjectId())).call();
                    }
                }
                destRef = command(RefParse.class).setName(destinationref).call();
                result.setNewRef(destRef.get());
            } else {
                // make a new branch
                Ref newRef = command(BranchCreateOp.class).setAutoCheckout(true)
                        .setName(destinationref)
                        .setSource(sourceRef.get().getObjectId().toString()).call();
                result.setNewRef(newRef);
            }

        }

        getProgressListener().complete();

        result.setRemoteName(remote.get().get().getFetchURL());

        return result;
    }

    /**
     * @param ref the ref to find
     * @return an {@link Optional} of the ref, or {@link Optional#absent()} if it wasn't found
     */
    public Optional<Ref> findRemoteRef(String ref) {

        String remoteRef = Ref.REMOTES_PREFIX + remote.get().get().getName() + "/" + ref;
        return command(RefParse.class).setName(remoteRef).call();
    }
}
