/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.LsRemote;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.ConfigOp.ConfigScope;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Clones a remote repository to a given directory.
 * 
 */
public class CloneOp extends AbstractGeoGitOp<Void> {

    private Optional<String> branch = Optional.absent();

    private String repositoryURL;

    /**
     * Constructs a new {@code CloneOp}.
     */
    @Inject
    public CloneOp() {
    }

    /**
     * @param repositoryURL the URL of the repository to clone
     * @return {@code this}
     */
    public CloneOp setRepositoryURL(final String repositoryURL) {
        this.repositoryURL = repositoryURL;
        return this;
    }

    /**
     * @param branch the branch to checkout when the clone is complete
     * @return {@code this}
     */
    public CloneOp setBranch(@Nullable String branch) {
        this.branch = Optional.fromNullable(branch);
        return this;
    }

    /**
     * Executes the clone operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {
        Preconditions.checkArgument(repositoryURL != null && !repositoryURL.isEmpty(),
                "No repository specified to clone from.");

        getProgressListener().started();
        getProgressListener().progress(0.f);

        // Set up origin
        Remote remote = command(RemoteAddOp.class).setName("origin").setURL(repositoryURL).call();

        // Fetch remote data
        command(FetchOp.class).setProgressListener(subProgress(90.f)).call();

        // Set up remote tracking branches
        final ImmutableSet<Ref> remoteRefs = command(LsRemote.class).setRemote(
                Suppliers.ofInstance(Optional.of(remote))).call();

        boolean emptyRepo = true;

        for (Ref remoteRef : remoteRefs) {
            if (emptyRepo && !remoteRef.getObjectId().isNull()) {
                emptyRepo = false;
            }
            String branchName = remoteRef.localName();
            if (!command(RefParse.class).setName(remoteRef.getName()).call().isPresent()) {
                command(BranchCreateOp.class).setName(branchName)
                        .setSource(remoteRef.getObjectId().toString()).call();
            } else {
                command(UpdateRef.class).setName(remoteRef.getName())
                        .setNewValue(remoteRef.getObjectId()).call();
            }

            command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setScope(ConfigScope.LOCAL)
                    .setName("branches." + branchName + ".remote").setValue(remote.getName())
                    .call();

            command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setScope(ConfigScope.LOCAL)
                    .setName("branches." + branchName + ".merge").setValue(remoteRef.getName())
                    .call();
        }
        getProgressListener().progress(95.f);

        if (!emptyRepo) {
            // checkout branch
            if (branch.isPresent()) {
                command(CheckoutOp.class).setForce(true).setSource(branch.get()).call();
            } else {
                // checkout the head
                final Optional<Ref> currRemoteHead = command(RefParse.class).setName(
                        Ref.REMOTES_PREFIX + remote.getName() + "/" + Ref.HEAD).call();
                Preconditions.checkState(currRemoteHead.isPresent(), "No remote HEAD.");
                Preconditions.checkState(currRemoteHead.get() instanceof SymRef,
                        "Remote HEAD is detached." + currRemoteHead.get().toString());
                final SymRef remoteHeadRef = (SymRef) currRemoteHead.get();
                final String currentBranch = Ref.localName(remoteHeadRef.getTarget());

                command(CheckoutOp.class).setForce(true).setSource(currentBranch).call();

            }
        }

        getProgressListener().complete();

        return null;
    }
}
