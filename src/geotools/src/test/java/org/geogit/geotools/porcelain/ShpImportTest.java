/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.exceptions.base.MockitoException;

/**
 *
 */
public class ShpImportTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @After
    public void tearDown() throws Exception {
        cli.close();
    }

    @Test
    public void testImport() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        importCommand.dataStoreFactory = TestHelper.createTestFactory();
        importCommand.run(cli);
    }

    @Test
    public void testImportFileNotExist() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://nonexistent.shp");
        importCommand.run(cli);
    }

    @Test
    public void testImportNullShapefileList() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.run(cli);
    }

    @Test
    public void testImportEmptyShapefileList() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.run(cli);
    }

    @Test
    public void testNoRepository() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        importCommand.run(cli);
    }

    @Test
    public void testImportHelp() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.help = true;
        importCommand.run(cli);
    }

    @Test
    public void testImportException() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        GeogitCLI mockCli = spy(new GeogitCLI(consoleReader));

        setUpGeogit(mockCli);

        when(mockCli.getConsole()).thenThrow(new MockitoException("Exception"));
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        exception.expect(MockitoException.class);
        importCommand.run(mockCli);
    }

    @Test
    public void testImportGetNamesException() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        importCommand.dataStoreFactory = TestHelper.createFactoryWithGetNamesException();
        importCommand.run(cli);
    }

    @Test
    public void testImportFeatureSourceException() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        importCommand.dataStoreFactory = TestHelper.createFactoryWithGetFeatureSourceException();
        importCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        ShpImport importCommand = new ShpImport();
        importCommand.shapeFile = new ArrayList<String>();
        importCommand.shapeFile.add("file://test.shp");
        importCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        importCommand.run(cli);
    }

    private void setUpGeogit(GeogitCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }
}
