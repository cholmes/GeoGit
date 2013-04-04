/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TagRemoveOp extends AbstractGeoGitOp<RevTag> {

    private final ObjectDatabase objectDb;

    private String name;

    /**
     * Constructs a new {@code TagCreateOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public TagRemoveOp(final ObjectDatabase objectDb) {
        this.objectDb = objectDb;
    }

    /**
     * Executes the tag creation operation.
     * 
     * @return the created tag
     * 
     */
    public RevTag call() throws RuntimeException {
        String fullPath = Ref.TAGS_PREFIX + name;
        Optional<RevObject> revTag = command(RevObjectParse.class).setRefSpec(fullPath).call();
        Preconditions.checkArgument(revTag.isPresent(), "Wrong tag name: " + name);
        Preconditions.checkArgument(revTag.get().getType().equals(RevObject.TYPE.TAG), name
                + " does not resolve to a tag");
        UpdateRef updateRef = command(UpdateRef.class).setName(fullPath).setDelete(true)
                .setReason("Delete tag " + name);
        Optional<Ref> tagRef = updateRef.call();
        checkState(tagRef.isPresent());
        return (RevTag) revTag.get();
    }

    public TagRemoveOp setName(String name) {
        this.name = name;
        return this;
    }
}
