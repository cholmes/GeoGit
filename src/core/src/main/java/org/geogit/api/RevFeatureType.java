/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * A binary representation of the state of a Feature Type.
 */
public class RevFeatureType extends AbstractRevObject {

    private final FeatureType featureType;

    private ImmutableList<PropertyDescriptor> sortedDescriptors;

    /**
     * Provides a method of sorting {@link PropertyDescriptor}s by their names.
     */
    public static final Ordering<PropertyDescriptor> PROPERTY_ORDER = new Ordering<PropertyDescriptor>() {
        @Override
        public int compare(PropertyDescriptor left, PropertyDescriptor right) {
            int c = Ordering.natural().nullsFirst()
                    .compare(left.getName().getNamespaceURI(), right.getName().getNamespaceURI());
            if (c == 0) {
                c = Ordering.natural().nullsFirst()
                        .compare(left.getName().getLocalPart(), right.getName().getLocalPart());
            }
            return c;
        }
    };

    /**
     * Constructs a new {@code RevFeatureType} from the given {@link FeatureType}.
     * 
     * @param featureType the feature type to use
     */
    public RevFeatureType(FeatureType featureType) {
        this(ObjectId.NULL, featureType);
    }

    /**
     * Constructs a new {@code RevFeatureType} from the given {@link ObjectId} and
     * {@link FeatureType}.
     * 
     * @param id the object id to use for this feature type
     * @param featureType the feature type to use
     */
    public RevFeatureType(ObjectId id, FeatureType featureType) {
        super(id);
        this.featureType = featureType;
        ArrayList<PropertyDescriptor> descriptors = Lists.newArrayList(this.featureType
                .getDescriptors());
        Collections.sort(descriptors, PROPERTY_ORDER);
        sortedDescriptors = ImmutableList.copyOf(descriptors);

    }

    @Override
    public TYPE getType() {
        return TYPE.FEATURETYPE;
    }

    public FeatureType type() {
        return featureType;
    }

    /**
     * @return the sorted {@link PropertyDescriptor}s of the feature type
     */
    public ImmutableList<PropertyDescriptor> sortedDescriptors() {
        return sortedDescriptors;
    }

    /**
     * @return the name of the feature type
     */
    public QName getName() {
        Name name = type().getName();
        return new QName(name.getNamespaceURI(), name.getLocalPart());
    }
}