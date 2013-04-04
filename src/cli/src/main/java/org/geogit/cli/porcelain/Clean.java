/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.porcelain.CleanOp;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.Repository;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

@Parameters(commandNames = "clean", commandDescription = "Deletes untracked features from working tree")
public class Clean implements CLICommand {

    @Parameter(description = "<path>")
    private List<String> path = new ArrayList<String>();

    @Parameter(names = { "--dry-run", "-n" }, description = "Don't actually remove anything, just show what would be done.")
    private boolean dryRun;

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        String pathFilter = null;
        if (!path.isEmpty()) {
            pathFilter = path.get(0);
        }

        if (dryRun) {
            if (pathFilter != null) {
                // check that is a valid path
                Repository repository = cli.getGeogit().getRepository();
                NodeRef.checkValidPath(pathFilter);

                Optional<NodeRef> ref = repository.command(FindTreeChild.class).setIndex(true)
                        .setParent(repository.getWorkingTree().getTree()).setChildPath(pathFilter)
                        .call();

                Preconditions.checkArgument(ref.isPresent(),
                        "pathspec '%s' did not match any tree", pathFilter);
                Preconditions.checkArgument(ref.get().getType() == TYPE.TREE,
                        "pathspec '%s' did not resolve to a tree", pathFilter);
            }
            Iterator<DiffEntry> unstaged = geogit.command(DiffWorkTree.class).setFilter(pathFilter)
                    .call();
            while (unstaged.hasNext()) {
                DiffEntry entry = unstaged.next();
                if (entry.changeType() == ChangeType.ADDED) {
                    console.println("Would remove " + entry.newPath());
                }
            }
        } else {
            geogit.command(CleanOp.class).setPath(pathFilter).call();
            console.println("Clean operation completed succesfully.");
        }
    }

}
