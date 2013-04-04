/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.storage.FieldType;
import org.opengis.feature.type.PropertyDescriptor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Shows formatted information about a commit, feature or feature type
 * 
 */
@Parameters(commandNames = "show", commandDescription = "Displays information about a commit, feature or feature type")
public class Show implements CLICommand {

    /**
     * The path to the element to display.
     */
    @Parameter(description = "<path>")
    private List<String> paths = new ArrayList<String>();

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkArgument(paths.size() < 2, "Only one refspec allowed");
        checkArgument(!paths.isEmpty(), "A refspec must be specified");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        String path = paths.get(0);

        Optional<RevObject> obj = geogit.command(RevObjectParse.class).setRefSpec(path).call();
        checkState(obj.isPresent(), "refspec did not resolve to any object.");
        RevObject revObject = obj.get();
        if (revObject instanceof RevFeature) {
            RevFeatureType ft = geogit.command(ResolveFeatureType.class).setRefSpec(path).call()
                    .get();
            ImmutableList<PropertyDescriptor> attribs = ft.sortedDescriptors();
            RevFeature feature = (RevFeature) revObject;
            Ansi ansi = AnsiDecorator.newAnsi(true);
            ansi.newline().fg(Color.YELLOW).a("ID:  ").reset().a(feature.getId().toString())
                    .newline().newline();
            ansi.a("ATTRIBUTES  ").newline();
            ansi.a("----------  ").newline();
            ImmutableList<Optional<Object>> values = feature.getValues();
            int i = 0;
            for (Optional<Object> value : values) {
                ansi.fg(Color.YELLOW).a(attribs.get(i).getName() + ": ").reset();
                if (value.isPresent()) {
                    ansi.a(value.get().toString()).newline();
                } else {
                    ansi.a(value.toString()).newline();
                }
                i++;
            }
            console.println(ansi.toString());

        } else if (revObject instanceof RevTree) {
            RevTree tree = (RevTree) revObject;
            Optional<RevFeatureType> opt = geogit.command(ResolveFeatureType.class)
                    .setRefSpec(path).call();
            checkArgument(opt.isPresent(),
                    "Refspec must resolve to a commit, feature or feature type");
            RevFeatureType ft = opt.get();
            ImmutableList<PropertyDescriptor> attribs = ft.sortedDescriptors();
            Ansi ansi = AnsiDecorator.newAnsi(true);

            ansi.fg(Color.YELLOW).a("TREE ID:  ").reset().a(tree.getId().toString()).newline();
            ansi.fg(Color.YELLOW).a("SIZE:  ").reset().a(Long.toString(tree.size())).newline();
            ansi.fg(Color.YELLOW).a("NUMBER Of SUBTREES:  ").reset()
                    .a(Integer.toString(tree.numTrees()).toString()).newline();

            ansi.fg(Color.YELLOW).a("DEFAULT FEATURE TYPE ID:  ").reset().a(ft.getId().toString())
                    .newline().newline();
            ansi.a("DEFAULT FEATURE TYPE ATTRIBUTES").newline();
            ansi.a("--------------------------------").newline();
            for (PropertyDescriptor attrib : attribs) {
                ansi.fg(Color.YELLOW).a(attrib.getName() + ": ").reset()
                        .a("<" + FieldType.forBinding(attrib.getType().getBinding()) + ">")
                        .newline();
            }
            console.println(ansi.toString());
        } else if (revObject instanceof RevCommit) {
            RevCommit commit = (RevCommit) revObject;
            Ansi ansi = AnsiDecorator.newAnsi(true);
            ansi.a(Strings.padEnd("Commit:", 15, ' ')).fg(Color.YELLOW)
                    .a(commit.getId().toString()).reset().newline();
            ansi.a(Strings.padEnd("Author:", 15, ' ')).fg(Color.GREEN)
                    .a(formatPerson(commit.getAuthor())).reset().newline();
            ansi.a(Strings.padEnd("Committer:", 15, ' ')).fg(Color.GREEN)
                    .a(formatPerson(commit.getAuthor())).reset().newline();
            ansi.a(Strings.padEnd("Author date:", 15, ' ')).a("(").fg(Color.RED)
                    .a(estimateSince(geogit.getPlatform(), commit.getAuthor().getTimestamp()))
                    .reset().a(") ").a(new Date(commit.getAuthor().getTimestamp())).newline();
            ansi.a(Strings.padEnd("Committer date:", 15, ' ')).a("(").fg(Color.RED)
                    .a(estimateSince(geogit.getPlatform(), commit.getCommitter().getTimestamp()))
                    .reset().a(") ").a(new Date(commit.getCommitter().getTimestamp())).newline();
            ansi.a(Strings.padEnd("Subject:", 15, ' ')).a(commit.getMessage()).newline();
            console.println(ansi.toString());
        } else {
            throw new IllegalArgumentException(
                    "Refspec must resolve to a commit, feature or feature type");
        }

    }

    /**
     * Converts a RevPerson for into a readable string.
     * 
     * @param person the person to format.
     * @return the formatted string
     * @see RevPerson
     */
    private String formatPerson(RevPerson person) {
        StringBuilder sb = new StringBuilder();
        sb.append(person.getName().or("<name not set>"));
        sb.append(" <").append(person.getEmail().or("")).append(">");
        return sb.toString();
    }

    /**
     * Converts a timestamp into a readable string that represents the rough time since that
     * timestamp.
     * 
     * @param platform
     * @param timestamp
     * @return
     */
    private String estimateSince(Platform platform, long timestamp) {
        long now = platform.currentTimeMillis();
        long diff = now - timestamp;
        final long seconds = 1000;
        final long minutes = seconds * 60;
        final long hours = minutes * 60;
        final long days = hours * 24;
        final long weeks = days * 7;
        final long months = days * 30;
        final long years = days * 365;

        if (diff > years) {
            return diff / years + " years ago";
        }
        if (diff > months) {
            return diff / months + " months ago";
        }
        if (diff > weeks) {
            return diff / weeks + " weeks ago";
        }
        if (diff > days) {
            return diff / days + " days ago";
        }
        if (diff > hours) {
            return diff / hours + " hours ago";
        }
        if (diff > minutes) {
            return diff / minutes + " minutes ago";
        }
        if (diff > seconds) {
            return diff / seconds + " seconds ago";
        }
        return "just now";
    }

}
