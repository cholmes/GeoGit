/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.inject.Inject;

public class TestNeo4JGraphDatabase extends Neo4JGraphDatabase {

    @Inject
    public TestNeo4JGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected GraphDatabaseService getGraphDatabase(String dbPath) {
        return new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    }

    @Override
    public void close() {
        if (isOpen()) {
            graphDB.shutdown();
            graphDB = null;
        }

    }
}
