/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.CherryPickOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * Given an existing commit, apply the change it introduces, recording a new commit . This requires
 * your working tree to be clean (no modifications from the HEAD commit).
 * <p>
 * CLI proxy for {@link CherryPickOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit cherry-pick <commitish>}
 * </ul>
 * 
 * @see CherryPickOp
 */
@Parameters(commandNames = "cherry-pick", commandDescription = "Apply the changes introduced by existing commits")
public class CherryPick extends AbstractCommand implements CLICommand {

    @Parameter(description = "<commitish>...")
    private List<String> commits = Lists.newArrayList();

    @Override
    public void runInternal(GeogitCLI cli) {
        final GeoGIT geogit = cli.getGeogit();
        checkState(geogit != null, "not in a geogit repository.");
        checkArgument(commits.size() > 0, "No commits specified.");
        checkArgument(commits.size() < 2, "Too many commits specified.");

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);

        Optional<ObjectId> commitId;
        commitId = geogit.command(RevParse.class).setRefSpec(commits.get(0)).call();
        Preconditions.checkArgument(commitId.isPresent(), "Commit not found '%s'", commits.get(0));
        cherryPick.setCommit(Suppliers.ofInstance(commitId.get()));

        cherryPick.call();

    }
}
