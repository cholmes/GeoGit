/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.porcelain;

import java.util.List;
import java.util.Map;

import org.geogit.api.Ref;

import com.google.common.collect.Maps;

/**
 *
 */
public class FetchResult {

    private Map<String, List<ChangedRef>> changedRefs = Maps.newHashMap();

    public Map<String, List<ChangedRef>> getChangedRefs() {
        return changedRefs;
    }

    static public class ChangedRef {
        public enum ChangeTypes {
            ADDED_REF, REMOVED_REF, CHANGED_REF
        }

        private Ref oldRef;

        private Ref newRef;

        private ChangeTypes type;

        public ChangedRef(Ref oldRef, Ref newRef, ChangeTypes type) {
            this.oldRef = oldRef;
            this.newRef = newRef;
            this.type = type;
        }

        public Ref getOldRef() {
            return oldRef;
        }

        public void setOldRef(Ref oldRef) {
            this.oldRef = oldRef;
        }

        public Ref getNewRef() {
            return newRef;
        }

        public void setNewRef(Ref newRef) {
            this.newRef = newRef;
        }

        public ChangeTypes getType() {
            return type;
        }

        public void setType(ChangeTypes type) {
            this.type = type;
        }

    }
}
