/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.osm.history.cli.OSMHistoryImport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for osm specific commands.
 * 
 * @see OSMHistoryImport
 */
@Parameters(commandNames = "osm", commandDescription = "GeoGit/OpenStreetMap integration utilities")
public class OSMCommandProxy implements CLICommandExtension {

    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit osm");
        commander.addCommand("import-history", new OSMHistoryImport());
        return commander;
    }
}
