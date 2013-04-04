/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geotools.util.Range;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.Feature;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class LogOpTest extends RepositoryTestCase {

    private LogOp logOp;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        logOp = geogit.command(LogOp.class);
    }

    @Test
    public void testEmptyRepo() throws Exception {
        Iterator<RevCommit> logs = logOp.call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());
    }

    @Test
    public void testHeadWithSingleCommit() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = geogit.command(CommitOp.class).call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHeadWithTwoCommits() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = geogit.command(CommitOp.class).call();

        insertAndAdd(lines1);
        final RevCommit secondCommit = geogit.command(CommitOp.class).call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        // by default returns most recent first
        assertEquals(secondCommit, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHeadWithMultipleCommits() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = geogit.command(CommitOp.class).call();
            expected.addFirst(commit);
        }

        Iterator<RevCommit> logs = logOp.call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);
    }

    @Test
    public void testPathFilterSingleFeature() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        List<RevCommit> allCommits = Lists.newArrayList();
        RevCommit expectedCommit = null;

        for (Feature f : features) {
            insertAndAdd(f);
            String id = f.getIdentifier().getID();
            final RevCommit commit = geogit.command(CommitOp.class).call();
            if (id.equals(lines1.getIdentifier().getID())) {
                expectedCommit = commit;
            }
            allCommits.add(commit);
        }

        String path = NodeRef.appendChild(linesName, lines1.getIdentifier().getID());

        List<RevCommit> feature2_1Commits = toList(logOp.addPath(path).call());
        assertEquals(1, feature2_1Commits.size());
        assertEquals(Collections.singletonList(expectedCommit), feature2_1Commits);
    }

    @Test
    public void testMultiplePaths() throws Exception {
        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        List<RevCommit> allCommits = Lists.newArrayList();
        RevCommit expectedLineCommit = null;
        RevCommit expectedPointCommit = null;
        for (Feature f : features) {
            insertAndAdd(f);
            String id = f.getIdentifier().getID();
            final RevCommit commit = geogit.command(CommitOp.class).call();
            if (id.equals(lines1.getIdentifier().getID())) {
                expectedLineCommit = commit;
            } else if (id.equals(points1.getIdentifier().getID())) {
                expectedPointCommit = commit;
            }
            allCommits.add(commit);
        }

        String linesPath = NodeRef.appendChild(linesName, lines1.getIdentifier().getID());
        String pointsPath = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());

        List<RevCommit> feature2_1Commits = toList(logOp.addPath(linesPath).call());
        List<RevCommit> featureCommits = toList(logOp.addPath(pointsPath).call());

        assertEquals(1, feature2_1Commits.size());
        assertEquals(2, featureCommits.size());

        assertEquals(Collections.singletonList(expectedLineCommit), feature2_1Commits);
        assertEquals(
                true,
                featureCommits.contains(expectedPointCommit)
                        && featureCommits.contains(expectedLineCommit));
    }

    @Test
    public void testPathFilterByTypeName() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> commits = Lists.newLinkedList();

        LinkedList<RevCommit> typeName1Commits = Lists.newLinkedList();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = geogit.command(CommitOp.class)
                    .setMessage(f.getIdentifier().toString()).call();
            commits.addFirst(commit);
            if (pointsName.equals(f.getType().getName().getLocalPart())) {
                typeName1Commits.addFirst(commit);
            }
        }

        // path to filter commits on type1
        String path = pointsName;

        List<RevCommit> logCommits = toList(logOp.addPath(path).call());
        assertEquals(typeName1Commits.size(), logCommits.size());
        assertEquals(typeName1Commits, logCommits);
    }

    @Test
    public void testLimit() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        for (Feature f : features) {
            insertAndAdd(f);
            geogit.command(CommitOp.class).call();
        }

        assertEquals(3, Iterators.size(logOp.setLimit(3).call()));
        assertEquals(1, Iterators.size(logOp.setLimit(1).call()));
        assertEquals(4, Iterators.size(logOp.setLimit(4).call()));

        exception.expect(IllegalArgumentException.class);
        logOp.setLimit(-1).call();
    }

    @Test
    public void testSkip() throws Exception {
        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        for (Feature f : features) {
            insertAndAdd(f);
            geogit.command(CommitOp.class).call();
        }

        logOp.setSkip(2).call();

        exception.expect(IllegalArgumentException.class);
        logOp.setSkip(-1).call();
    }

    @Test
    public void testTemporalConstraint() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        List<Long> timestamps = Arrays.asList(Long.valueOf(1000), Long.valueOf(2000),
                Long.valueOf(3000), Long.valueOf(4000), Long.valueOf(5000), Long.valueOf(6000));

        LinkedList<RevCommit> allCommits = new LinkedList<RevCommit>();

        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);
            Long timestamp = timestamps.get(i);
            insertAndAdd(f);
            final RevCommit commit = geogit.command(CommitOp.class)
                    .setCommitterTimestamp(timestamp).call();
            allCommits.addFirst(commit);
        }

        // test time range exclusive
        boolean minInclusive = false;
        boolean maxInclusive = false;
        Range<Date> commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive,
                new Date(5000), maxInclusive);
        logOp.setTimeRange(commitRange);

        List<RevCommit> logged = toList(logOp.call());
        List<RevCommit> expected = allCommits.subList(2, 4);
        assertEquals(expected, logged);

        // test time range inclusive
        minInclusive = true;
        maxInclusive = true;
        commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive, new Date(5000),
                maxInclusive);
        logOp = geogit.command(LogOp.class).setTimeRange(commitRange);

        logged = toList(logOp.call());
        expected = allCommits.subList(1, 5);
        assertEquals(expected, logged);

        // test reset time range
        logOp = geogit.command(LogOp.class).setTimeRange(commitRange).setTimeRange(null);
        logged = toList(logOp.call());
        expected = allCommits;
        assertEquals(expected, logged);
    }

    @Test
    public void testSinceUntil() throws Exception {
        final ObjectId oid1_1 = insertAndAdd(points1);
        final RevCommit commit1_1 = geogit.command(CommitOp.class).call();

        insertAndAdd(points2);
        final RevCommit commit1_2 = geogit.command(CommitOp.class).call();

        insertAndAdd(lines1);
        final RevCommit commit2_1 = geogit.command(CommitOp.class).call();

        final ObjectId oid2_2 = insertAndAdd(lines2);
        final RevCommit commit2_2 = geogit.command(CommitOp.class).call();

        try {
            logOp.setSince(oid1_1).call();
            fail("Expected ISE as since is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("since"));
        }

        try {
            logOp.setSince(null).setUntil(oid2_2).call();
            fail("Expected ISE as until is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("until"));
        }

        List<RevCommit> logs;
        List<RevCommit> expected;

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(null).call());
        expected = Arrays.asList(commit2_2, commit2_1);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit2_2.getId()).setUntil(null).call());
        expected = Collections.emptyList();
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(null).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1, commit1_2, commit1_1);
        assertEquals(expected, logs);
    }
}
