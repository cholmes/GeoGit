/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.BranchDeleteOp;
import org.geogit.api.porcelain.BranchListOp;
import org.geogit.api.porcelain.BranchRenameOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * With no arguments the command will display all existing branches with the current branch
 * highlighted with the asterisk. The {@code -r} option will list only remote branches and the
 * {@code -a} option will list both local and remote branches. Adding the {@code --color} option
 * with the value of auto, always, or never will add or remove color from the listing. With the
 * {@code -v} option it will list the branches along with the commit id and commit message that the
 * branch is currently on.
 * <p>
 * With a branch name specified it will create a branch of off the current branch. If a start point
 * is specified as well then it will be created off of the given start point. If the -c option is
 * given it will automatically checkout the branch once it is created.
 * <p>
 * With the -d option with a branch name specified will delete that branch. You cannot delete the
 * branch that you are currently on, checkout a different branch to delete it. Also with the -d
 * option you can list multiple branches for deletion.
 * <p>
 * With the -m option you can specify an oldBranchName to rename with the given newBranchName or you
 * can rename the current branch by not specifying oldBranchName. With the --force option you can
 * rename a branch to a name that already exists as a branch, however this will delete the other
 * branch.
 * <p>
 * CLI proxy {@link BranchListOp}, {@link BranchCreateOp}, {@link BranchDeleteOp},
 * {@link BranchRenameOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit branch [-c] <branchname>[<startpoint>]}: Creates a new branch with the given
 * branchname at the specified startpoint and checks it out immediately
 * <li> {@code geogit branch [--color=always] [-v] [-a]}: Lists all branches (Local and Remote) in
 * color with commit id and commit message
 * <li> {@code geogit branch [-r]}: List only remote branches
 * <li> {@code geogit branch [--delete] <branchname>...}: Deletes all the branches listed unless HEAD
 * is pointing to it
 * <li> {@code geogit branch [--force] [--rename] [<oldBranchName>] <newBranchName>}: Renames a
 * branch specified by oldBranchName or current branch if no oldBranchName is given to newBranchName
 * </ul>
 * 
 * @see BranchListOp
 * @see BranchCreateOp
 * @see BranchDeleteOp
 * @see BranchRenameOp
 */
@Parameters(commandNames = "branch", commandDescription = "List, create, or delete branches")
public class Branch extends AbstractCommand implements CLICommand {

    @Parameter(description = "<branch name> [<start point>]")
    private List<String> branchName = Lists.newArrayList();

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    private ColorArg color = ColorArg.auto;

    @Parameter(names = { "--checkout", "-c" }, description = "automatically checkout the new branch when the command is used to create a branch")
    private boolean checkout;

    @Parameter(names = { "--delete", "-d" })
    private boolean delete = false;

    @Parameter(names = { "--force", "-f" }, description = "Force renaming of a branch")
    private boolean force = false;

    @Parameter(names = { "--verbose", "-v",
            "Verbose output for list mode. Shows branch commit id and commit message." })
    private boolean verbose = false;

    @Parameter(names = { "--remote", "-r" }, description = "List or delete (if used with -d) the remote-tracking branches.")
    private boolean remotes = false;

    @Parameter(names = { "--all", "-a" }, description = "List all branches, both local and remote")
    private boolean all = false;

    @Parameter(names = { "--rename", "-m" }, description = "Rename branch ")
    private boolean rename = false;

    @Override
    public void runInternal(final GeogitCLI cli) throws Exception {
        final GeoGIT geogit = cli.getGeogit();
        checkState(geogit != null, "not in a geogit repository.");

        final ConsoleReader console = cli.getConsole();

        if (delete) {
            if (branchName.isEmpty()) {
                console.println("no name specified for deletion");
            }
            for (String br : branchName) {
                Optional<? extends Ref> deletedBranch = geogit.command(BranchDeleteOp.class)
                        .setName(br).call();
                if (deletedBranch.isPresent()) {
                    console.println("Deleted branch '" + br + "'.");
                } else {
                    console.println("No branch called '" + br + "'.");
                }
            }
            return;
        }

        checkArgument(branchName.size() < 3, "too many arguments: " + branchName.toString());

        if (rename) {
            if (branchName.isEmpty()) {
                console.println("You must specify a branch to rename.");
            } else if (branchName.size() == 1) {
                Optional<Ref> headRef = geogit.command(RefParse.class).setName(Ref.HEAD).call();
                geogit.command(BranchRenameOp.class).setNewName(branchName.get(0)).setForce(force)
                        .call();
                try {
                    if (headRef.isPresent()) {
                        console.println("renamed branch '"
                                + ((SymRef) (headRef.get())).getTarget().substring(
                                        Ref.HEADS_PREFIX.length()) + "' to '" + branchName.get(0)
                                + "'");
                    }

                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            } else {
                geogit.command(BranchRenameOp.class).setOldName(branchName.get(0))
                        .setNewName(branchName.get(1)).setForce(force).call();
                try {
                    console.println("renamed branch '" + branchName.get(0) + "' to '"
                            + branchName.get(1) + "'");
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return;
        }

        if (branchName.isEmpty()) {
            listBranches(cli);
            return;
        }

        final String branch = branchName.get(0);
        final String origin = branchName.size() > 1 ? branchName.get(1) : Ref.HEAD;

        Ref newBranch = geogit.command(BranchCreateOp.class).setName(branch)
                .setAutoCheckout(checkout).setSource(origin).call();

        try {
            console.println("Created branch " + newBranch.getName());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void listBranches(GeogitCLI cli) {
        final ConsoleReader console = cli.getConsole();
        final GeoGIT geogit = cli.getGeogit();

        boolean local = all || remotes == false;
        boolean remote = all || remotes;

        ImmutableList<Ref> branches = geogit.command(BranchListOp.class).setLocal(local)
                .setRemotes(remote).call();

        final Terminal terminal = console.getTerminal();
        boolean useColor;
        switch (color) {
        case never:
            useColor = false;
            break;
        case always:
            useColor = true;
            break;
        default:
            useColor = terminal.isAnsiSupported();
        }

        final Ref currentHead = geogit.command(RefParse.class).setName(Ref.HEAD).call().get();

        final int largest = verbose ? largestLenght(branches) : 0;

        for (Ref branchRef : branches) {
            final String branchRefName = branchRef.getName();

            Ansi ansi = AnsiDecorator.newAnsi(useColor);

            if ((currentHead instanceof SymRef)
                    && ((SymRef) currentHead).getTarget().equals(branchRefName)) {
                ansi.a("* ").fg(Color.GREEN);
            } else {
                ansi.a("  ");
            }
            // print unqualified names for local branches
            String branchName = branchRefName.startsWith(Ref.HEADS_PREFIX) ? branchRefName
                    .substring(Ref.HEADS_PREFIX.length()) : branchRefName;
            if (branchRef instanceof SymRef) {
                branchName += " -> " + Ref.localName(((SymRef) branchRef).getTarget());
            }
            ansi.a(branchName);
            ansi.reset();

            if (verbose) {
                ansi.a(Strings.repeat(" ", 1 + (largest - branchName.length())));
                ansi.a(branchRef.getObjectId().toString().substring(0, 7)).a(" ");

                Optional<RevCommit> commit = findCommit(geogit, branchRef);
                if (commit.isPresent()) {
                    ansi.a(messageTitle(commit.get()));
                }
            }

            try {
                console.println(ansi.toString());
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private String messageTitle(RevCommit commit) {
        String message = Optional.fromNullable(commit.getMessage()).or("");
        int newline = message.indexOf('\n');
        return newline == -1 ? message : message.substring(0, newline);
    }

    /**
     * @param branchRef
     * @return
     */
    private Optional<RevCommit> findCommit(GeoGIT geogit, Ref branchRef) {
        ObjectId commitId = branchRef.getObjectId();
        if (commitId.isNull()) {
            return Optional.absent();
        }
        RevCommit commit = geogit.getRepository().getCommit(commitId);
        return Optional.of(commit);
    }

    /**
     * @param branches
     * @return
     */
    private int largestLenght(ImmutableList<Ref> branches) {
        int len = 0;
        for (Ref ref : branches) {
            len = Math.max(len, ref.getName().length());
        }
        return len;
    }
}
