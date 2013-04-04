/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import org.geogit.cli.CLICommandExtension;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for remote specific commands.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit remote <command> <args>...}
 * </ul>
 * 
 * @see RemoteAdd
 * @see RemoteRemove
 * @see RemoteList
 */
@Parameters(commandNames = "remote", commandDescription = "remote utilities")
public class RemoteExtension implements CLICommandExtension {

    /**
     * @return the JCommander parser for this extension
     * @see JCommander
     */
    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander(this);
        commander.setProgramName("geogit remote");
        commander.addCommand("add", new RemoteAdd());
        commander.addCommand("rm", new RemoteRemove());
        commander.addCommand("list", new RemoteList());
        return commander;
    }
}
