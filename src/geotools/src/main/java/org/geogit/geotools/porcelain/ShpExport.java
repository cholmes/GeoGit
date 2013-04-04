/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.storage.ObjectDatabase;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeatureType;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Exports features from a feature type into a shapefile.
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export", commandDescription = "Export to Shapefile")
public class ShpExport extends AbstractShpCommand implements CLICommand {

    @Parameter(description = "<featureType> <shapefile>", arity = 2)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output file")
    public boolean overwrite;

    /**
     * Executes the export command using the provided options.
     * 
     * @param cli
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        if (args.isEmpty()) {
            printUsage();
            return;
        }

        String featureTypeName = args.get(0);
        String shapefile = args.get(1);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        File file = new File(shapefile);
        if (file.exists() && !overwrite) {
            cli.getConsole().println("The selected shapefile already exists. Use -o to overwrite");
            return;
        }

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", new File(shapefile).toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params);

        SimpleFeatureType featureType;
        try {
            featureType = getFeatureType(featureTypeName, cli);
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("No features to export.");
            return;
        }
        dataStore.createSchema(featureType);

        final String typeName = dataStore.getTypeNames()[0];
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            final SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            cli.getGeogit().command(ExportOp.class).setFeatureStore(featureStore)
                    .setFeatureTypeName(featureTypeName)
                    .setProgressListener(cli.getProgressListener()).call();
            cli.getConsole().println(featureTypeName + " exported successfully to " + shapefile);
        } else {
            // do we need to check this?
            cli.getConsole().println("Could not create feature store.");
            return;
        }

    }

    private SimpleFeatureType getFeatureType(String featureTypeName, GeogitCLI cli) {

        checkArgument(featureTypeName != null, "No feature type name specified.");

        String refspec;
        if (featureTypeName.contains(":")) {
            refspec = featureTypeName;
        } else {
            refspec = "WORK_HEAD:" + featureTypeName;
        }

        checkArgument(refspec.endsWith(":") != true, "No feature type name specified.");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> rootTreeId = geogit.command(ResolveTreeish.class)
                .setTreeish(refspec.split(":")[0]).call();

        checkState(rootTreeId.isPresent(), "Couldn't resolve '" + refspec + "' to a treeish object");

        RevTree rootTree = geogit.getRepository().getTree(rootTreeId.get());
        Optional<NodeRef> featureTypeTree = geogit.command(FindTreeChild.class)
                .setChildPath(refspec.split(":")[1]).setParent(rootTree).setIndex(true).call();

        checkArgument(featureTypeTree.isPresent(), "pathspec '" + refspec.split(":")[1]
                + "' did not match any valid path");

        Optional<RevObject> revObject = geogit.command(RevObjectParse.class).setRefSpec(refspec)
                .call(RevObject.class);

        checkArgument(revObject.isPresent(), "Invalid reference: %s", refspec);
        checkArgument(revObject.get().getType() == TYPE.TREE, "%s did not resolve to a tree",
                refspec);

        ObjectDatabase database = cli.getGeogit().getRepository().getObjectDatabase();
        DepthTreeIterator iter = new DepthTreeIterator("", featureTypeTree.get().getMetadataId(),
                (RevTree) revObject.get(), database, Strategy.FEATURES_ONLY);

        while (iter.hasNext()) {
            NodeRef nodeRef = iter.next();
            revObject = cli.getGeogit().command(RevObjectParse.class)
                    .setObjectId(nodeRef.getMetadataId()).call();
            if (revObject.isPresent() && revObject.get() instanceof RevFeatureType) {
                RevFeatureType revFeatureType = (RevFeatureType) revObject.get();
                if (revFeatureType.type() instanceof SimpleFeatureType) {
                    return (SimpleFeatureType) revFeatureType.type();
                }
            }
        }

        throw new GeoToolsOpException(StatusCode.NO_FEATURES_FOUND);

    }
}
