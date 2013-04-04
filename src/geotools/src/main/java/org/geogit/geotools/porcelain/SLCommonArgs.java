/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import com.beust.jcommander.Parameter;

/**
 * Common arguments for SpatiaLite porcelain commands.
 */
public class SLCommonArgs {

    /**
     * The databse to connect to. Default: database
     */
    @Parameter(names = "--database", description = "The databse to connect to.  Default: database.sqlite")
    public String database = "database.sqlite";

    /**
     * User name. Default: user
     */
    @Parameter(names = "--user", description = "User name.  Default: user")
    public String username = "user";

}
