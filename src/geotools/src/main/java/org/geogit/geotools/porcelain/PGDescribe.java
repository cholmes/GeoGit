/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.net.ConnectException;
import java.util.Map;
import java.util.Map.Entry;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.DescribeOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geotools.data.DataStore;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Describes a table from a PostGIS database.
 * 
 * PostGIS CLI proxy for {@link DescribeOp}
 * 
 * @see DescribeOp
 */
@Parameters(commandNames = "describe", commandDescription = "Describe a PostGIS table")
public class PGDescribe extends AbstractPGCommand implements CLICommand {

    /**
     * Table to describe.
     */
    @Parameter(names = { "--table", "-t" }, description = "Table to describe.", required = true)
    public String table = "";

    /**
     * Executes the describe command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractPGCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        DataStore dataStore = null;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
            cli.getConsole().flush();
            return;
        }

        try {
            cli.getConsole().println("Fetching table...");

            Optional<Map<String, String>> propertyMap = cli.getGeogit().command(DescribeOp.class)
                    .setTable(table).setDataStore(dataStore).call();

            if (propertyMap.isPresent()) {
                cli.getConsole().println("Table : " + table);
                cli.getConsole().println("----------------------------------------");
                for (Entry<String, String> entry : propertyMap.get().entrySet()) {
                    cli.getConsole().println("\tProperty  : " + entry.getKey());
                    cli.getConsole().println("\tType      : " + entry.getValue());
                    cli.getConsole().println("----------------------------------------");
                }
            } else {
                cli.getConsole().println("Could not find the specified table.");
            }
        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                cli.getConsole().println("No table supplied.");
                break;
            case UNABLE_TO_GET_FEATURES:
                cli.getConsole().println("Unable to read the feature source.");
                break;
            case UNABLE_TO_GET_NAMES:
                cli.getConsole().println("Unable to read feature types.");
                break;
            default:
                cli.getConsole().println("Exception: " + e.statusCode.name());
            }

        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
