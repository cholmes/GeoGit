/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.rest.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.di.GeogitModule;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geogit.geotools.data.GeoGitDataStoreFactory;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geogit.storage.datastream.DataStreamSerializationFactory;
import org.geogit.test.integration.RepositoryTestCase;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataAccess;
import org.junit.AfterClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.restlet.data.MediaType;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Integration test for GeoServer cached layers using the GWC REST API
 * 
 */
public class GeoServerRESTIntegrationTest extends GeoServerSystemTestSupport {

    private static final String WORKSPACE = "geogittest";

    private static final String STORE = "geogitstore";

    private static final String BASE_URL = "/geogit/" + WORKSPACE + ":" + STORE;

    private static RepositoryTestCase helper;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        helper = new RepositoryTestCase() {

            @Override
            protected Injector createInjector() {
                return Guice.createInjector(Modules.override(new GeogitModule()).with(
                        new JEStorageModule()));
            }

            @Override
            protected void setUpInternal() throws Exception {
                configureGeogitDataStore();
            }
        };
        helper.setUp();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
        }
    }

    private void configureGeogitDataStore() throws Exception {
        helper.insertAndAdd(helper.lines1);
        helper.getGeogit().command(CommitOp.class).call();

        Catalog catalog = getCatalog();
        CatalogFactory factory = catalog.getFactory();
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(WORKSPACE);
        ns.setURI("http://geogit.org");
        catalog.add(ns);
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(ns.getName());
        catalog.add(ws);

        DataStoreInfo ds = factory.createDataStore();
        ds.setEnabled(true);
        ds.setDescription("Test Geogit DataStore");
        ds.setName(STORE);
        ds.setType(GeoGitDataStoreFactory.DISPLAY_NAME);
        ds.setWorkspace(ws);
        Map<String, Serializable> connParams = ds.getConnectionParameters();

        URL geogitDir = helper.getGeogit().command(ResolveGeogitDir.class).call();
        File repositoryUrl = new File(geogitDir.toURI()).getParentFile();
        assertTrue(repositoryUrl.exists() && repositoryUrl.isDirectory());

        connParams.put(GeoGitDataStoreFactory.REPOSITORY.key, repositoryUrl);
        connParams.put(GeoGitDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
        catalog.add(ds);

        DataStoreInfo dsInfo = catalog.getDataStoreByName(WORKSPACE, STORE);
        assertNotNull(dsInfo);
        assertEquals(GeoGitDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGitDataStore);
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/manifest}
     */
    @Test
    public void testGetManifest() throws Exception {
        final String url = BASE_URL + "/repo/manifest";
        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatusCode());

        String contentType = sr.getContentType();
        assertTrue(contentType, sr.getContentType().startsWith("text/plain"));

        String responseBody = sr.getOutputStreamContent();
        assertNotNull(responseBody);
        assertTrue(responseBody, responseBody.startsWith("HEAD refs/heads/master"));
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/exists?oid=...}
     */
    @Test
    public void testRevObjectExists() throws Exception {
        final String resource = BASE_URL + "/repo/exists?oid=";

        GeoGIT geogit = helper.getGeogit();
        Ref head = geogit.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();

        String url;
        url = resource + commitId.toString();
        assertResponse(url, "1");

        ObjectId treeId = geogit.command(ResolveTreeish.class).setTreeish(commitId).call().get();
        url = resource + treeId.toString();
        assertResponse(url, "1");

        url = resource + ObjectId.forString("fake");
        assertResponse(url, "0");
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/objects/<oid>}
     */
    @Test
    public void testGetObject() throws Exception {
        GeoGIT geogit = helper.getGeogit();
        Ref head = geogit.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();
        ObjectId treeId = geogit.command(ResolveTreeish.class).setTreeish(commitId).call().get();

        testGetRemoteObject(commitId);
        testGetRemoteObject(treeId);
    }

    private void testGetRemoteObject(ObjectId oid) throws Exception {
        GeoGIT geogit = helper.getGeogit();

        final String resource = BASE_URL + "/repo/objects/";
        final String url = resource + oid.toString();

        MockHttpServletResponse servletResponse;
        InputStream responseStream;

        servletResponse = getAsServletResponse(url);
        assertEquals(200, servletResponse.getStatusCode());

        String contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
        assertEquals(contentType, servletResponse.getContentType());

        responseStream = getBinaryInputStream(servletResponse);

        ObjectSerializingFactory factory = new DataStreamSerializationFactory();

        RevObject actual = factory.createObjectReader().read(oid, responseStream);
        RevObject expected = geogit.command(RevObjectParse.class).setObjectId(oid).call().get();
        assertEquals(expected, actual);
    }

    private MockHttpServletResponse assertResponse(String url, String expectedContent)
            throws Exception {

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatusCode());

        String responseBody = sr.getOutputStreamContent();

        assertNotNull(responseBody);
        assertEquals(expectedContent, responseBody);
        return sr;
    }
}
