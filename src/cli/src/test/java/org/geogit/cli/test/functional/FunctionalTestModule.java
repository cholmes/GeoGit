/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.test.functional;

import org.geogit.api.Platform;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.TestNeo4JGraphDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module with tweaks to run functional tests on the target {@link Platform}'s working
 * directory.
 * 
 * @see CLITestInjectorBuilder
 */
public class FunctionalTestModule extends AbstractModule {

    private Platform testPlatform;

    /**
     * @param testPlatform
     */
    public FunctionalTestModule(Platform testPlatform) {
        this.testPlatform = testPlatform;
    }

    @Override
    protected void configure() {
        if (testPlatform != null) {
            bind(Platform.class).toInstance(testPlatform);
        }

        // Use the testing neo4j graph db, otherwise functional tests are extremely slow
        bind(GraphDatabase.class).to(TestNeo4JGraphDatabase.class).in(Scopes.SINGLETON);
    }

}
