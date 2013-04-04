/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.test.integration.je;

import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class JEDiffOpTest extends org.geogit.test.integration.DiffOpTest {
    @Override
    protected Injector createInjector() {
        return Guice.createInjector(Modules.override(new GeogitModule())
                .with(new JEStorageModule()));
    }
}
