/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevTreeSerializationTest;

public class DataStreamRevTreeSerializationTest extends RevTreeSerializationTest {
    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new DataStreamSerializationFactory();
    }
}
