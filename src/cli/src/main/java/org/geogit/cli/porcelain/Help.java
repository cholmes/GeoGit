/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * This command displays the usage for GeoGit or a specific command if provided.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit [--]help [<command>]}
 * </ul>
 */
@Parameters(commandNames = { "help", "--help" }, commandDescription = "Print this help message, or provide a command name to get help for")
public class Help implements CLICommand {

    @Parameter
    private List<String> parameters = new ArrayList<String>();

    /**
     * Executes the help command.
     * 
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    // @Override
    public void run(GeogitCLI cli) {

        JCommander jc = cli.newCommandParser();

        if (parameters.isEmpty()) {
            cli.printShortCommandList(jc);
        } else {
            String command = parameters.get(0);
            jc.usage(command);
        }
    }

}
