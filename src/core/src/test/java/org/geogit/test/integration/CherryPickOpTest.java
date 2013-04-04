/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.Iterator;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CherryPickOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.LogOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Suppliers;

public class CherryPickOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("groldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();
    }

    @Test
    public void testCherryPick() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - master - Points 1 added
        // .\
        // . o - Points 2 added
        // . |
        // . o - Points 3 added
        // . |
        // . o - Lines 1 added
        // . |
        // . o - branch1 - Lines 2 added
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines1);
        final RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idL1).call();
        insertAndAdd(lines2);
        final RevCommit c5 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();

        // Cherry pick several commits to create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |
        // o - Lines 2 added
        // |
        // o - Points 3 added
        // |
        // o - master - Points 2 added

        // switch back to master
        geogit.command(CheckoutOp.class).setSource("master").call();
        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.setCommit(Suppliers.ofInstance(c5.getId()));
        RevCommit commit2 = cherryPick.call();

        assertEquals(c5.getAuthor(), commit2.getAuthor());
        assertEquals(c5.getCommitter().getName(), commit2.getCommitter().getName());
        assertEquals(c5.getMessage(), commit2.getMessage());
        assertFalse(c5.getCommitter().getTimestamp() == commit2.getCommitter().getTimestamp());
        assertFalse(c5.getTreeId().equals(commit2.getTreeId()));

        cherryPick.setCommit(Suppliers.ofInstance(c3.getId()));
        RevCommit commit3 = cherryPick.call();

        assertEquals(c3.getAuthor(), commit3.getAuthor());
        assertEquals(c3.getCommitter().getName(), commit3.getCommitter().getName());
        assertEquals(c3.getMessage(), commit3.getMessage());
        assertFalse(c3.getCommitter().getTimestamp() == commit3.getCommitter().getTimestamp());
        assertFalse(c3.getTreeId().equals(commit3.getTreeId()));

        cherryPick.setCommit(Suppliers.ofInstance(c2.getId()));
        RevCommit commit4 = cherryPick.call();

        assertEquals(c2.getAuthor(), commit4.getAuthor());
        assertEquals(c2.getCommitter().getName(), commit4.getCommitter().getName());
        assertEquals(c2.getCommitter().getEmail(), commit4.getCommitter().getEmail());
        assertEquals(c2.getMessage(), commit4.getMessage());
        assertFalse(c2.getCommitter().getTimestamp() == commit4.getCommitter().getTimestamp());
        assertFalse(c2.getTreeId().equals(commit4.getTreeId()));

        cherryPick.setCommit(Suppliers.ofInstance(c4.getId()));
        RevCommit commit5 = cherryPick.call();

        assertEquals(c4.getMessage(), commit5.getMessage());
        assertEquals(c4.getAuthor().getName(), commit5.getAuthor().getName());
        assertEquals(c4.getAuthor().getEmail(), commit5.getAuthor().getEmail());
        assertEquals(c4.getCommitter().getName(), commit5.getCommitter().getName());
        assertFalse(c4.getCommitter().getTimestamp() == commit5.getCommitter().getTimestamp());
        assertFalse(c4.getTreeId().equals(commit5.getTreeId()));

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 5
        RevCommit logC5 = log.next();
        assertEquals(commit5, logC5);

        // Commit 4
        RevCommit logC4 = log.next();
        assertEquals(commit4, logC4);

        // Commit 3
        RevCommit logC3 = log.next();
        assertEquals(commit3, logC3);

        // Commit 2
        RevCommit logC2 = log.next();
        assertEquals(commit2, logC2);

        // Commit 1
        RevCommit logC1 = log.next();
        assertEquals(c1, logC1);

        assertFalse(log.hasNext());

    }

    @Test
    public void testCherryPickInvalidCommit() throws Exception {
        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.setCommit(Suppliers.ofInstance(ObjectId.NULL));
        exception.expect(IllegalArgumentException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickDirtyWorkTree() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master and insert some features
        geogit.command(CheckoutOp.class).setSource("master").call();
        insert(points3);

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.setCommit(Suppliers.ofInstance(c1.getId()));
        exception.expect(IllegalStateException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickDirtyIndex() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master and insert some features
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(points3);

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.setCommit(Suppliers.ofInstance(c1.getId()));
        exception.expect(IllegalStateException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickRootCommit() throws Exception {
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.setCommit(Suppliers.ofInstance(c1.getId()));
        cherryPick.call();

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 2
        RevCommit logC2 = log.next();
        assertEquals(c1.getMessage(), logC2.getMessage());
        assertEquals(c1.getAuthor(), logC2.getAuthor());
        assertEquals(c1.getCommitter().getName(), logC2.getCommitter().getName());
        assertEquals(c1.getCommitter().getEmail(), logC2.getCommitter().getEmail());
        assertFalse(c1.getCommitter().getTimestamp() == logC2.getCommitter().getTimestamp());
        assertEquals(c1.getTreeId(), logC2.getTreeId());

        // Commit 1
        RevCommit logC1 = log.next();
        assertEquals(c1, logC1);

        assertFalse(log.hasNext());

    }
}
