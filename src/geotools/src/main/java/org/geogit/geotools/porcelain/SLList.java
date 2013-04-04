/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.net.ConnectException;
import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ListOp;
import org.geotools.data.DataStore;

import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Lists tables from a SpatiaLite database.
 * 
 * SpatiaLite CLI proxy for {@link ListOp}
 * 
 * @see ListOp
 */
@Parameters(commandNames = "list", commandDescription = "List available feature types in a database")
public class SLList extends AbstractSLCommand implements CLICommand {

    /**
     * Executes the list command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractSLCommand#runInternal(org.geogit.cli.GeogitCLI)
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
            cli.getConsole().println("Fetching feature types...");

            Optional<List<String>> features = cli.getGeogit().command(ListOp.class)
                    .setDataStore(dataStore).call();
            ;

            if (features.isPresent()) {
                for (String featureType : features.get()) {
                    cli.getConsole().println(" - " + featureType);
                }
            } else {
                cli.getConsole().println("No features types were found in the specified database.");
            }
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("Unable to get feature types from the database.");
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
