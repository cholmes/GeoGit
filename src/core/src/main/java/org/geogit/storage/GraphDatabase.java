/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.ObjectId;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@Beta
public interface GraphDatabase {

    /**
     * Initializes/opens the databse. It's safe to call this method multiple times, and only the
     * first call shall take effect.
     */
    public void open();

    /**
     * @return true if the database is open, false otherwise
     */
    public boolean isOpen();

    /**
     * Closes the database.
     */
    public void close();

    /**
     * Determines if the given commit exists in the graph database.
     * 
     * @param commitId the commit id to search for
     * @return true if the commit exists, false otherwise
     */
    public boolean exists(final ObjectId commitId);

    /**
     * Retrieves all of the parents for the given commit
     * 
     * @param commitid
     * @return
     * @throws IllegalArgumentException
     */
    public ImmutableList<ObjectId> getParents(ObjectId commitId) throws IllegalArgumentException;

    /**
     * Adds a commit to the database with the given parents. If a commit with the same id already
     * exists, it will not be inserted.
     * 
     * @param commitId the commit id to insert
     * @param parentIds the commit ids of the commit's parents
     * @return true if the commit id was inserted, false otherwise
     */
    public boolean put(final ObjectId commitId, ImmutableList<ObjectId> parentIds);

    /**
     * Finds the lowest common ancestor of two commits.
     * 
     * @param leftId the commit id of the left commit
     * @param rightId the commit id of the right commit
     * @return An {@link Optional} of the lowest common ancestor of the two commits, or
     *         {@link Optional#absent()} if a common ancestor could not be found.
     */
    public Optional<ObjectId> findLowestCommonAncestor(ObjectId leftId, ObjectId rightId);
}
