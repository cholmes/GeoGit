/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;

public class ConflictsWriteOp extends AbstractGeoGitOp<Void> {

    private List<Conflict> conflicts;

    private StagingDatabase indexDatabase;

    @Inject
    public ConflictsWriteOp(StagingDatabase indexDatabase) {
        checkNotNull(indexDatabase);
        this.indexDatabase = indexDatabase;
    }

    @Override
    public Void call() {
        for (Conflict conflict : conflicts) {
            indexDatabase.addConflict(conflict);
        }
        return null;

    }

    public ConflictsWriteOp setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
        return this;
    }

}
