/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.MergeConflictsException;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.api.porcelain.ResetOp;
import org.geogit.api.porcelain.ResetOp.ResetMode;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * Incorporates changes from the named commits (since the time their histories diverged from the
 * current branch) into the current branch.
 * <p>
 * CLI proxy for {@link MergeOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit merge [-m <msg>] [--ours] [--theirs] <commitish>...}
 * </ul>
 * 
 * @see MergeOp
 */
@Parameters(commandNames = "merge", commandDescription = "Merge two or more histories into one")
public class Merge extends AbstractCommand implements CLICommand {

    @Parameter(names = "-m", description = "Commit message")
    private String message;

    @Parameter(names = "--ours", description = "Use 'ours' strategy")
    private boolean ours;

    @Parameter(names = "--theirs", description = "Use 'theirs' strategy")
    private boolean theirs;

    @Parameter(names = "--no-commit", description = "Do not perform a commit after merging")
    private boolean noCommit;

    @Parameter(names = "--abort", description = "Aborts the current merge")
    private boolean abort;

    @Parameter(description = "<commitish>...")
    private List<String> commits = Lists.newArrayList();

    /**
     * Executes the merge command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkState(commits.size() > 0, "No commits provided to merge.");

        ConsoleReader console = cli.getConsole();

        final GeoGIT geogit = cli.getGeogit();

        Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

        if (abort) {
            Optional<Ref> ref = geogit.command(RefParse.class).setName(Ref.MERGE_HEAD).call();
            if (!ref.isPresent()) {
                throw new IllegalArgumentException(
                        "There is no merge to abort <MERGE_HEAD missing>.");
            }
            geogit.command(ResetOp.class).setMode(ResetMode.HARD)
                    .setCommit(Suppliers.ofInstance(ref.get().getObjectId()));
            console.println("Merge aborted successfully.");
            return;
        }

        RevCommit commit;
        try {
            MergeOp merge = geogit.command(MergeOp.class);
            merge.setOurs(ours).setTheirs(theirs).setNoCommit(noCommit);
            merge.setMessage(message).setProgressListener(cli.getProgressListener());
            for (String commitish : commits) {
                Optional<ObjectId> commitId;
                commitId = geogit.command(RevParse.class).setRefSpec(commitish).call();
                checkArgument(commitId.isPresent(), "Commit not found '%s'", commitish);
                merge.addCommit(Suppliers.ofInstance(commitId.get()));
            }
            commit = merge.call();
        } catch (NothingToCommitException noChanges) {
            console.println(ansi.fg(Color.RED).a(noChanges.getMessage()).reset().toString());
            return;
        } catch (MergeConflictsException e) {
            console.println(e.getMessage());
            return;
        }
        final ObjectId parentId = commit.parentN(0).or(ObjectId.NULL);

        console.println("[" + commit.getId() + "] " + commit.getMessage());

        console.print("Committed, counting objects...");
        Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(parentId)
                .setNewVersion(commit.getId()).call();

        int adds = 0, deletes = 0, changes = 0;
        DiffEntry diffEntry;
        while (diff.hasNext()) {
            diffEntry = diff.next();
            switch (diffEntry.changeType()) {
            case ADDED:
                ++adds;
                break;
            case REMOVED:
                ++deletes;
                break;
            case MODIFIED:
                ++changes;
                break;
            }
        }

        ansi.fg(Color.GREEN).a(adds).reset().a(" features added, ").fg(Color.YELLOW).a(changes)
                .reset().a(" changed, ").fg(Color.RED).a(deletes).reset().a(" deleted.").reset()
                .newline();

        console.print(ansi.toString());
    }
}
