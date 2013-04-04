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
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.CatObject;
import org.geogit.api.plumbing.FeatureNodeRefFromRefspec;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * Starts the merge tool to resolve conflicts
 * 
 * @see MergeOp
 */

// Currently it just print conflict descriptions, so they can be used by another tool instead.

@Parameters(commandNames = "conflicts", commandDescription = "Shows existing conflicts")
public class Conflicts extends AbstractCommand implements CLICommand {

    @Parameter(description = "<path> [<path>...]")
    private List<String> paths = Lists.newArrayList();

    @Parameter(names = { "--diff" }, description = "Show diffs instead of full element descriptions")
    private boolean previewDiff;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        GeoGIT geogit = cli.getGeogit();
        List<Conflict> conflicts = geogit.command(ConflictsReadOp.class).call();

        if (conflicts.isEmpty()) {
            cli.getConsole().println("No elements need merging.");
            return;
        }
        for (Conflict conflict : conflicts) {
            if (paths.isEmpty() || paths.contains(conflict.getPath())) {

                if (previewDiff) {
                    printConflictDiff(conflict, cli.getConsole(), geogit);
                } else {
                    printConflict(conflict, cli.getConsole(), geogit);
                }
            }
        }
    }

    private void printConflictDiff(Conflict conflict, ConsoleReader console, GeoGIT geogit)
            throws IOException {
        FullDiffPrinter diffPrinter = new FullDiffPrinter(false, true);
        console.println("---" + conflict.getPath() + "---");
        ObjectId mergeHeadId = geogit.command(RefParse.class).setName(Ref.MERGE_HEAD).call().get()
                .getObjectId();
        Optional<RevCommit> mergeHead = geogit.command(RevObjectParse.class)
                .setObjectId(mergeHeadId).call(RevCommit.class);
        ObjectId origHeadId = geogit.command(RefParse.class).setName(Ref.ORIG_HEAD).call().get()
                .getObjectId();
        Optional<RevCommit> origHead = geogit.command(RevObjectParse.class).setObjectId(origHeadId)
                .call(RevCommit.class);
        Optional<RevCommit> commonAncestor = geogit.command(FindCommonAncestor.class)
                .setLeft(mergeHead.get()).setRight(origHead.get()).call();
        String ancestorPath = commonAncestor.get().getId().toString() + ":" + conflict.getPath();
        NodeRef ancestorNodeRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(ancestorPath).call();
        String path = Ref.ORIG_HEAD + ":" + conflict.getPath();
        NodeRef oursNodeRef = geogit.command(FeatureNodeRefFromRefspec.class).setRefspec(path)
                .call();
        DiffEntry diffEntry = new DiffEntry(ancestorNodeRef, oursNodeRef);
        console.println("Ours");
        diffPrinter.print(geogit, console, diffEntry);
        path = Ref.MERGE_HEAD + ":" + conflict.getPath();
        NodeRef theirsNodeRef = geogit.command(FeatureNodeRefFromRefspec.class).setRefspec(path)
                .call();
        diffEntry = new DiffEntry(ancestorNodeRef, theirsNodeRef);
        console.println("Theirs");
        diffPrinter.print(geogit, console, diffEntry);

    }

    private void printConflict(Conflict conflict, ConsoleReader console, GeoGIT geogit)
            throws IOException {

        console.println(conflict.getPath());
        console.println();
        printObject("Ancestor", conflict.getAncestor(), console, geogit);
        console.println();
        printObject("Ours", conflict.getOurs(), console, geogit);
        console.println();
        printObject("Theirs", conflict.getTheirs(), console, geogit);
        console.println();

    }

    private void printObject(String name, ObjectId id, ConsoleReader console, GeoGIT geogit)
            throws IOException {

        console.println(name + "\t" + id.toString());
        if (!id.isNull()) {
            Optional<RevObject> obj = geogit.command(RevObjectParse.class).setObjectId(id).call();
            CharSequence s = geogit.command(CatObject.class)
                    .setObject(Suppliers.ofInstance(obj.get())).call();
            console.println(s);
        }

    }
}
