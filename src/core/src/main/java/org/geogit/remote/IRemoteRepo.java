/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.IOException;

import org.geogit.api.Ref;
import org.geogit.api.porcelain.PushException;
import org.geogit.repository.Repository;

import com.google.common.collect.ImmutableSet;

/**
 * Provides an interface for interacting with remote repositories.
 */
public interface IRemoteRepo {

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    public void open() throws IOException;

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    public void close() throws IOException;

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    public ImmutableSet<Ref> listRefs(boolean getHeads, boolean getTags);

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    public Ref headRef();

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param localRepository the repository to add new objects to
     * @param ref the remote ref that points to new commit data
     */
    public void fetchNewData(Repository localRepository, Ref ref);

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     */
    public void pushNewData(Repository localRepository, Ref ref) throws PushException;

    /**
     * Push all new objects from the specified {@link Ref} to the given refspec.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     * @param refspec the refspec to push to
     */
    public void pushNewData(Repository localRepository, Ref ref, String refspec)
            throws PushException;

    /**
     * Delete the given refspec from the remote repository.
     * 
     * @param refspec the refspec to delete
     */
    public void deleteRef(String refspec);
}
