/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import org.geogit.api.RevFeatureType;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.ImmutableSortedSet;

public class DescribeFeatureTypeTest extends RepositoryTestCase {

    RevFeatureType featureType;

    @Override
    protected void setUpInternal() throws Exception {
        featureType = RevFeatureType.build(pointsType);
    }

    @Test
    public void testDescribeNullFeatureType() throws Exception {
        DescribeFeatureType describe = new DescribeFeatureType();

        try {
            describe.call();
            fail("expected IllegalStateException on null feature type");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("FeatureType has not been set"));
        }
    }

    @Test
    public void testDescribeFeatureType() throws Exception {
        DescribeFeatureType describe = new DescribeFeatureType();

        ImmutableSortedSet<PropertyDescriptor> properties = describe.setFeatureType(featureType)
                .call();

        for (PropertyDescriptor prop : properties) {
            assertTrue(pointsType.getDescriptors().contains(prop));
        }

    }
}
