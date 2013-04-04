/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.test.performance;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;

import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;

public class LogOpPerformanceTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Ignore
    @Test
    public void testCommits() throws Exception {
        System.err.println("############### Warming up....");
        createAndLogMultipleCommits(1000);
        System.err.println("############### Warm up done.");

        createAndLogMultipleCommits(1000);
        createAndLogMultipleCommits(1000 * 10);
        // createAndLogMultipleCommits(1000 * 100);
        // createAndLogMultipleCommits(1000 * 1000);
    }

    private void createAndLogMultipleCommits(int numCommits) throws Exception {
        super.doSetUp();

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        System.err.println("***********\nCreating " + numberFormat.format(numCommits)
                + " commits...");

        Stopwatch sw = new Stopwatch().start();
        createCommits(numCommits);
        sw.stop();
        System.err.println(numberFormat.format(numCommits) + " created in " + sw.toString());
        System.err.flush();

        sw.reset().start();
        Iterator<RevCommit> commits = geogit.command(LogOp.class).call();
        sw.stop();
        System.err.println("LogOp took " + sw.toString());

        sw.reset().start();
        int c = 0;
        while (commits.hasNext()) {
            c++;
            commits.next();
        }
        sw.stop();
        System.err.println("Iterated " + numberFormat.format(c) + " commits in " + sw.toString());
        super.tearDown();
    }

    private void createCommits(int numCommits) {
        int largeStep = numCommits / 10;
        int smallStep = numCommits / 100;

        for (int i = 1; i <= numCommits; i++) {
            if (i % largeStep == 0) {
                System.err.print(i);
                System.err.flush();
            } else if (i % smallStep == 0) {
                System.err.print('.');
                System.err.flush();
            }
            geogit.command(CommitOp.class).setAllowEmpty(true).setMessage("Commit " + i).call();
        }
        System.err.print('\n');
    }
}
