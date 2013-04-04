/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.test.functional;

import static org.geogit.cli.test.functional.GlobalState.currentDirectory;
import static org.geogit.cli.test.functional.GlobalState.homeDirectory;
import static org.geogit.cli.test.functional.GlobalState.stdOut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.geogit.cli.test.functional.GlobalState;

import cucumber.annotation.After;
import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;

/**
 *
 */
public class GeoToolsInitSteps extends AbstractGeoToolsFunctionalTest {

    @After
    public void after() {
        if (GlobalState.geogit != null) {
            GlobalState.geogit.close();
        }
    }

    @Given("^I am in an empty directory$")
    public void I_am_in_an_empty_directory() throws Throwable {
        setUpDirectories();
        assertEquals(0, currentDirectory.list().length);
        setupGeogit();
    }

    @When("^I run the command \"([^\"]*)\"$")
    public void I_run_the_command_X(String commandSpec) throws Throwable {
        String[] args = commandSpec.split(" ");
        runCommand(args);
    }

    @When("^I run the command \"([^\"]*)\" on the PostGIS database$")
    public void I_run_the_command_X_on_the_postgis_database(String commandSpec) throws Throwable {
        commandSpec += getPGDatabaseParameters();
        String[] args = commandSpec.split(" ");
        runCommand(args);
    }

    @When("^I run the command \"([^\"]*)\" on the SpatiaLite database$")
    public void I_run_the_command_X_on_the_spatialite_database(String commandSpec) throws Throwable {
        commandSpec += " --database ";
        commandSpec += getClass().getResource("testdb.sqlite").getPath();
        String[] args = commandSpec.split(" ");
        runCommand(args);
    }

    @Then("^it should answer \"([^\"]*)\"$")
    public void it_should_answer_exactly(String expected) throws Throwable {
        expected = expected.replace("${currentdir}", currentDirectory.getAbsolutePath());
        String actual = stdOut.toString().replaceAll("\n", "");
        assertEquals(expected, actual);
    }

    @Then("^the response should start with \"([^\"]*)\"$")
    public void the_response_should_start_with(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll("\n", "");
        assertTrue(actual, actual.startsWith(expected));
    }

    @Then("^the response should contain \"([^\"]*)\"$")
    public void the_response_should_contain(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll("\n", "");
        assertTrue(actual, actual.contains(expected));
    }

    @Then("^the repository directory shall exist$")
    public void the_repository_directory_shall_exist() throws Throwable {
        List<String> output = runAndParseCommand("rev-parse", "--resolve-geogit-dir");
        assertEquals(output.toString(), 1, output.size());
        String location = output.get(0);
        assertNotNull(location);
        if (location.startsWith("Error:")) {
            fail(location);
        }
        File repoDir = new File(new URI(location));
        assertTrue("Repository directory not found: " + repoDir.getAbsolutePath(), repoDir.exists());
    }

    @Given("^I have an unconfigured repository$")
    public void I_have_an_unconfigured_repository() throws Throwable {
        setUpDirectories();
        setupGeogit();

        List<String> output = runAndParseCommand("init");
        assertEquals(output.toString(), 1, output.size());
        assertNotNull(output.get(0));
        assertTrue(output.get(0), output.get(0).startsWith("Initialized"));
    }

    @Given("^I have a repository$")
    public void I_have_a_repository() throws Throwable {
        I_have_an_unconfigured_repository();
        runCommand("config", "--global", "user.name", "John Doe");
        runCommand("config", "--global", "user.email", "JohnDoe@example.com");
    }

    @Then("^if I change to the respository subdirectory \"([^\"]*)\"$")
    public void if_I_change_to_the_respository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = currentDirectory;
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.exists());
        currentDirectory = dir;
    }

    @Given("^I am inside a repository subdirectory \"([^\"]*)\"$")
    public void I_am_inside_a_repository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = currentDirectory;
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.mkdirs());
        currentDirectory = dir;
    }

    private void setUpDirectories() throws IOException {
        homeDirectory = new File("target", "fakeHomeDir" + new Random().nextInt());
        FileUtils.deleteDirectory(homeDirectory);
        assertFalse(homeDirectory.exists());
        assertTrue(homeDirectory.mkdirs());

        currentDirectory = new File("target", "testrepo" + new Random().nextInt());
        FileUtils.deleteDirectory(currentDirectory);
        assertFalse(currentDirectory.exists());
        assertTrue(currentDirectory.mkdirs());
    }

    @Given("^I have 6 unstaged features$")
    public void I_have_6_unstaged_features() throws Throwable {
        insertFeatures();
    }

    @Given("^I stage 6 features$")
    public void I_stage_6_features() throws Throwable {
        insertAndAddFeatures();
    }

    @Given("^I have several commits")
    public void I_have_several_commits() throws Throwable {
        insertAndAdd(points1);
        insertAndAdd(points2);
        runCommand(("commit -m Commit1").split(" "));
        insertAndAdd(points3);
        insertAndAdd(lines1);
        runCommand(("commit -m Commit2").split(" "));
        insertAndAdd(lines2);
        insertAndAdd(lines3);
        runCommand(("commit -m Commit3").split(" "));
        insertAndAdd(points1_modified);
        runCommand(("commit -m Commit4").split(" "));

    }
}
