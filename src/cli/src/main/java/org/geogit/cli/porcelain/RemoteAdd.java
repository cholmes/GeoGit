/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteException;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Adds a remote for the repository with the given name and URL.
 * <p>
 * With {@code -t <branch>} option, instead of the default global refspec for the remote to track
 * all branches under the refs/remotes/<name>/ namespace, a refspec to track only <branch> is
 * created.
 * <p>
 * CLI proxy for {@link RemoteAddOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit remote add [-t <branch>] <name> <url>}
 * </ul>
 * 
 * @see RemoteAddOp
 */
@Parameters(commandNames = "remote add", commandDescription = "Add a remote for the repository")
public class RemoteAdd extends AbstractCommand implements CLICommand {

    @Parameter(names = { "-t", "--track" }, description = "branch to track")
    private String branch = "*";

    @Parameter(description = "<name> <url>")
    private List<String> params = new ArrayList<String>();

    /**
     * Executes the remote add command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        if (params == null || params.size() != 2) {
            printUsage();
            return;
        }

        try {
            cli.getGeogit().command(RemoteAddOp.class).setName(params.get(0)).setURL(params.get(1))
                    .setBranch(branch).call();
        } catch (RemoteException e) {
            switch (e.statusCode) {
            case REMOTE_ALREADY_EXISTS:
                cli.getConsole().println(
                        "Could not add, a remote called '" + params.get(0) + "' already exists.");
                break;
            default:
                break;
            }
        }

    }

}
