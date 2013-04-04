/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.geotools.porcelain;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Map;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;

/**
 * A template for shapefile commands; provides out of the box support for the --help argument so
 * far.
 * 
 * @see CLICommand
 */
public abstract class AbstractShpCommand implements CLICommand {

    /**
     * Flag for displaying help for the command.
     */
    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

    /**
     * Factory for constructing the data store.
     * 
     * @see ShapefileDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

    /**
     * Executes the command.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        if (help) {
            printUsage();
            return;
        }

        runInternal(cli);
    }

    protected void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit shp " + commandName);
        jc.usage();
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;

    /**
     * Constructs a new shapefile data store using the specified shapefile.
     * 
     * @param shapefile the url of the shapefile to use in creating the data store
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore(String shapefile) throws Exception {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(ShapefileDataStoreFactory.URLP.key, shapefile);
        params.put(ShapefileDataStoreFactory.NAMESPACEP.key, "http://www.opengis.net/gml");

        if (!dataStoreFactory.canProcess(params)) {
            throw new FileNotFoundException();
        }

        DataStore dataStore = dataStoreFactory.createDataStore(params);

        if (dataStore == null) {
            throw new FileNotFoundException();
        }

        return dataStore;
    }
}