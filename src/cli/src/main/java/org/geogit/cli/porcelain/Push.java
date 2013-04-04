/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.porcelain.PushException;
import org.geogit.api.porcelain.PushOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Updates remote refs using local refs, while sending objects necessary to complete the given refs.
 * <p>
 * CLI proxy for {@link PushOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit push [<options>] [<repository> [<refspec>...]]}
 * </ul>
 * 
 * @see PushOp
 */
@Parameters(commandNames = "push", commandDescription = "Update remote refs along with associated objects")
public class Push extends AbstractCommand implements CLICommand {

    @Parameter(names = "--all", description = "Instead of naming each ref to push, specifies that all refs under refs/heads/ be pushed.")
    private boolean all = false;

    @Parameter(description = "[<repository> [<refspec>...]]")
    private List<String> args;

    /**
     * Executes the push command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        PushOp push = cli.getGeogit().command(PushOp.class);
        push.setProgressListener(cli.getProgressListener());
        push.setAll(all);

        if (args != null) {
            if (args.size() > 0) {
                push.setRemote(args.get(0));
            }
            for (int i = 1; i < args.size(); i++) {
                push.addRefSpec(args.get(i));
            }
        }
        try {
            push.call();
        } catch (PushException e) {
            switch (e.statusCode) {
            case NOTHING_TO_PUSH:
                cli.getConsole().println("Nothing to push.");
                break;
            case REMOTE_HAS_CHANGES:
                cli.getConsole()
                        .println(
                                "Push failed: The remote repository has changes that would be lost in the event of a push.");
                break;
            }
        }
    }
}
