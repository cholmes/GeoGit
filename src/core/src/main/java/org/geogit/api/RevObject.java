/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Base object type accessed during revision walking.
 * 
 * @see RevCommit
 * @see RevTree
 * @see RevFeature
 * @see RevTag
 */
public interface RevObject {

    /**
     * {@code RevObject} types enumeration.
     */
    public static enum TYPE {
        COMMIT {
            @Override
            public int value() {
                return 0;
            }

            @Override
            public Class<RevCommit> binding() {
                return RevCommit.class;
            }
        },
        TREE {
            @Override
            public int value() {
                return 1;
            }

            @Override
            public Class<RevTree> binding() {
                return RevTree.class;
            }
        },
        FEATURE {
            @Override
            public int value() {
                return 2;
            }

            @Override
            public Class<RevFeature> binding() {
                return RevFeature.class;
            }
        },
        TAG {
            @Override
            public int value() {
                return 3;
            }

            @Override
            public Class<RevTag> binding() {
                return RevTag.class;
            }
        },
        FEATURETYPE {
            @Override
            public int value() {
                return 4;
            }

            @Override
            public Class<RevFeatureType> binding() {
                return RevFeatureType.class;
            }
        };

        public abstract int value();

        public abstract Class<? extends RevObject> binding();

        public static TYPE valueOf(final int value) {
            return TYPE.values()[value];
        }

        private static final ImmutableMap<Class<? extends RevObject>, Integer> byBinding = ImmutableMap
                .of(COMMIT.binding(), COMMIT.value(), TREE.binding(), TREE.value(),
                        FEATURE.binding(), FEATURE.value(), TAG.binding(), TAG.value(),
                        FEATURETYPE.binding(), FEATURETYPE.value());

        public static TYPE valueOf(final Class<? extends RevObject> binding) {
            Preconditions.checkNotNull(binding);
            Integer value = byBinding.get(binding);
            Preconditions.checkNotNull(value);
            return valueOf(value);
        }
    }

    /**
     * @return the object type of this object
     */
    public TYPE getType();

    /**
     * Get the name of this object.
     * 
     * @return unique hash of this object.
     */
    public ObjectId getId();

    /**
     * Equality is based on id
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o);
}
