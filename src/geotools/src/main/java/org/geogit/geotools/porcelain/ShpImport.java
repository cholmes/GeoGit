/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.io.FileNotFoundException;
import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geotools.data.DataStore;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Imports features from one or more shapefiles.
 * 
 * PostGIS CLI proxy for {@link ImportOp}
 * 
 * @see ImportOp
 */
@Parameters(commandNames = "import", commandDescription = "Import Shapefile")
public class ShpImport extends AbstractShpCommand implements CLICommand {

    /**
     * Shapefiles to import.
     */
    @Parameter(description = "<shapefile> [<shapefile>]...")
    List<String> shapeFile;

    /**
     * Executes the import command using the provided options.
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
        if (shapeFile == null || shapeFile.isEmpty()) {
            cli.getConsole().println("No shapefile specified");
            return;
        }

        for (String shp : shapeFile) {

            DataStore dataStore = null;
            try {
                dataStore = getDataStore(shp);
            } catch (FileNotFoundException e) {
                cli.getConsole().println(
                        "The shapefile '" + shp + "' could not be found, skipping...");
                continue;
            }

            try {

                cli.getConsole().println("Importing from shapefile " + shp);

                ProgressListener progressListener = cli.getProgressListener();
                cli.getGeogit().command(ImportOp.class).setAll(true).setTable(null)
                        .setDataStore(dataStore).setProgressListener(progressListener).call();

                cli.getConsole().println(shp + " imported successfully.");

            } catch (GeoToolsOpException e) {
                switch (e.statusCode) {
                case NO_FEATURES_FOUND:
                    cli.getConsole().println("No features were found in the shapefile.");
                    break;
                case UNABLE_TO_GET_NAMES:
                    cli.getConsole().println("Unable to get feature types from the shapefile.");
                    break;
                case UNABLE_TO_GET_FEATURES:
                    cli.getConsole().println("Unable to get features from the shapefile.");
                    break;
                case UNABLE_TO_INSERT:
                    cli.getConsole().println("Unable to insert features into the working tree.");
                    break;
                default:
                    cli.getConsole()
                            .println("Import failed with exception: " + e.statusCode.name());
                }
            } finally {
                dataStore.dispose();
                cli.getConsole().flush();
            }
        }
    }
}
