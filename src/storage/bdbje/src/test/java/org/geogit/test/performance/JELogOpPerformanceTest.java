/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.test.performance;

import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class JELogOpPerformanceTest extends LogOpPerformanceTest {
    @Override
    protected Injector createInjector() {
        return Guice.createInjector(Modules.override(new GeogitModule())
                .with(new JEStorageModule()));
    }
}
