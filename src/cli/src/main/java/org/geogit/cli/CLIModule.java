/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.cli;

import java.util.ServiceLoader;

import com.google.inject.Module;

/**
 * Marker interface for modules that provide extra CLI commands.
 * 
 * <p>
 * The {@link GeogitCLI CLI} app uses the standard {@link ServiceLoader}
 * "Service Provider Interface" mechanism to look for implementations of this interface on the
 * classpath.
 * <p>
 * Any CLI plugin that provides extra command line commands shall include a file named
 * {@code org.geogit.cli.CLIModule} text file inside the jar's {@code META-INF/services} folder,
 * whose content is the full qualified class name of the module implementation. There can be more
 * than one module declared on each file, separated by a newline.
 */
public interface CLIModule extends Module {

}
