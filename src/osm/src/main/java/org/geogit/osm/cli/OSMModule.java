/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.cli;

import org.geogit.cli.CLIModule;

import com.google.inject.Binder;

/**
 *
 */
public class OSMModule implements CLIModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(OSMCommandProxy.class);
    }

}
