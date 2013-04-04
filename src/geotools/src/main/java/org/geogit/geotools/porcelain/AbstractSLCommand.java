/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.geotools.porcelain;

import java.io.Serializable;
import java.net.ConnectException;
import java.sql.Connection;
import java.util.Map;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.spatialite.SpatiaLiteDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Maps;

/**
 * A template for SpatiaLite commands; provides out of the box support for the --help argument so
 * far.
 * 
 * @see CLICommand
 */
public abstract class AbstractSLCommand implements CLICommand {

    /**
     * Flag for displaying help for the command.
     */
    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

    /**
     * Common arguments for SpatiaLite commands.
     * 
     * @see SLCommonArgs
     */
    @ParametersDelegate
    public SLCommonArgs commonArgs = new SLCommonArgs();

    /**
     * Factory for constructing the data store.
     * 
     * @see SpatiaLiteDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new SpatiaLiteDataStoreFactory();

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
            JCommander jc = new JCommander(this);
            String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
            jc.setProgramName("geogit sl " + commandName);
            jc.usage();
            return;
        }

        runInternal(cli);
    }

    /**
     * Prints the correct usage of the geogit sl command.
     */
    protected void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit sl " + commandName);
        jc.usage();
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;

    /**
     * Constructs a new SpatiaLite data store using connection parameters from {@link SLCommonArgs}.
     * 
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore() throws Exception {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(SpatiaLiteDataStoreFactory.DBTYPE.key, "spatialite");
        params.put(SpatiaLiteDataStoreFactory.DATABASE.key, commonArgs.database);
        params.put(SpatiaLiteDataStoreFactory.USER.key, commonArgs.username);

        DataStore dataStore = dataStoreFactory.createDataStore(params);

        if (dataStore == null) {
            throw new ConnectException();
        }

        if (dataStore instanceof JDBCDataStore) {
            Connection con = null;
            try {
                con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
            } catch (Exception e) {
                throw new ConnectException();
            }

            ((JDBCDataStore) dataStore).closeSafe(con);
        }

        return dataStore;
    }
}
