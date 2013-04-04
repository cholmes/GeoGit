/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli;

/**
 * Marker interface to aid in looking up CLI commands
 */
public interface CLICommand {

    /**
     * @param cli
     * 
     */
    void run(GeogitCLI cli) throws Exception;

}
