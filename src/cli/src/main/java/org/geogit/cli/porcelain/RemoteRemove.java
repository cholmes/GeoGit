/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.porcelain.RemoteException;
import org.geogit.api.porcelain.RemoteRemoveOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Remove the remote named <name>. All remote-tracking branches and configuration settings for the
 * remote are removed.
 * 
 * <p>
 * CLI proxy for {@link RemoteRemoveOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit rm <name>}
 * </ul>
 * 
 * @see RemoteRemoveOp
 */
@Parameters(commandNames = "rm", commandDescription = "Remove a remote from the repository")
public class RemoteRemove extends AbstractCommand implements CLICommand {

    @Parameter(description = "<name>")
    private List<String> params;

    /**
     * Executes the remote remove command.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        if (params == null || params.size() != 1) {
            printUsage();
            return;
        }

        try {
            cli.getGeogit().command(RemoteRemoveOp.class).setName(params.get(0)).call();
        } catch (RemoteException e) {
            switch (e.statusCode) {
            case REMOTE_NOT_FOUND:
                cli.getConsole().println("Could not find a remote called '" + params.get(0) + "'.");
                break;
            default:
                break;
            }
        }
    }

}
