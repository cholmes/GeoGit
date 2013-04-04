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

public class ConflictsReadOp extends AbstractGeoGitOp<List<Conflict>> {

    private StagingDatabase indexDatabase;

    @Inject
    public ConflictsReadOp(StagingDatabase indexDatabase) {
        checkNotNull(indexDatabase);
        this.indexDatabase = indexDatabase;
    }

    @Override
    public List<Conflict> call() {
        return indexDatabase.getConflicts(null);
    }

}
