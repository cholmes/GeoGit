/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geotools.data.DataStore;
import org.opengis.feature.type.Name;

import com.google.common.base.Optional;

/**
 * Internal operation for listing tables from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */
public class ListOp extends AbstractGeoGitOp<Optional<List<String>>> {

    private DataStore dataStore;

    /**
     * Executes the list operation on the provided data store.
     * 
     * @return a list of all tables, or Optional.absent() if none were found
     */
    @Override
    public Optional<List<String>> call() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }

        List<String> features = new ArrayList<String>();

        boolean foundTable = false;

        List<Name> typeNames;
        try {
            typeNames = dataStore.getNames();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
        }

        for (Name typeName : typeNames) {
            foundTable = true;

            features.add(typeName.toString());
        }

        if (!foundTable) {
            return Optional.absent();
        }
        return Optional.of(features);
    }

    /**
     * @param dataStore the data store to use for the import process
     * @return {@code this}
     */
    public ListOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

}
