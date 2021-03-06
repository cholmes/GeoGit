/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevCommitSerializationTest;

public class DataStreamCommitSerializationTest extends RevCommitSerializationTest {

    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new DataStreamSerializationFactory();
    }

}
