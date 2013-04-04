/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTag;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.TagCreateOp;
import org.geogit.api.porcelain.TagListOp;
import org.geogit.api.porcelain.TagRemoveOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Creates or deletes tags Usage:
 * <ul>
 * <li> {@code geogit commit <tagname> [tag_commit] [-d] [-m <msg>]}
 * </ul>
 * 
 * @see TagOp
 */
@Parameters(commandNames = "tag", commandDescription = "creates/deletes tags")
public class Tag extends AbstractCommand implements CLICommand {

    @Parameter(names = "-m", description = "Tag message")
    private String message;

    @Parameter(names = "-d", description = "Delete tag")
    private boolean delete;

    @Parameter(description = "<tag_name> [tag_commit]")
    private List<String> nameAndCommit = Lists.newArrayList();

    /**
     * Executes the commit command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkState((message != null && !message.trim().isEmpty()) || nameAndCommit.isEmpty()
                || delete, "No tag message provided");
        checkState(nameAndCommit.size() < 2 || (nameAndCommit.size() == 2 && !delete),
                "Too many parameters provided");

        if (nameAndCommit.isEmpty()) {
            listTags(cli);
            return;
        }

        String name = nameAndCommit.get(0);
        String commit = nameAndCommit.size() > 1 ? nameAndCommit.get(1) : Ref.HEAD;

        ConsoleReader console = cli.getConsole();

        final GeoGIT geogit = cli.getGeogit();

        if (delete) {
            geogit.command(TagRemoveOp.class).setName(name).call();
            console.println("Deleted tag " + name);
        } else {
            Optional<ObjectId> commitId = geogit.command(RevParse.class).setRefSpec(commit).call();
            checkState(commitId.isPresent(), "Wrong reference: " + commit);
            RevTag tag = geogit.command(TagCreateOp.class).setName(name).setMessage(message)
                    .setCommitId(commitId.get()).call();
            console.println("Created tag " + name + " -> " + tag.getCommitId());
        }

    }

    private void listTags(GeogitCLI cli) {

        GeoGIT geogit = cli.getGeogit();
        ImmutableList<RevTag> tags = geogit.command(TagListOp.class).call();
        for (RevTag tag : tags) {
            try {
                cli.getConsole().println(tag.getName());
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

    }
}
