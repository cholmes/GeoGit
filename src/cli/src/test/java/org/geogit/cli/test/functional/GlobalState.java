/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.geogit.api.GeoGIT;
import org.geogit.cli.GeogitCLI;

/**
 */
public class GlobalState {

    public static File homeDirectory;

    public static File currentDirectory;

    public static ByteArrayInputStream stdIn;

    public static ByteArrayOutputStream stdOut;

    public static GeoGIT geogit;

    public static GeogitCLI geogitCLI;

}
