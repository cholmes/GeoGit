/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Throwables;

/**
 * Provides the ability to execute several commands in succession without re-initializing GeoGit or
 * the command line interface.
 */
public class GeogitConsole {

    /**
     * Constructs the GeogitConsole
     */
    public GeogitConsole() {
    }

    /**
     * Entry point for the Geogit console.
     * 
     * @param args unused
     */
    public static void main(String... args) {
        try {
            new GeogitConsole().run();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * @throws IOException
     * 
     */
    private void run() throws IOException {

        InputStream in = System.in;
        OutputStream out = System.out;
        ConsoleReader consoleReader = new ConsoleReader(in, out);
        consoleReader.setAutoprintThreshold(20);
        consoleReader.setPaginationEnabled(true);
        consoleReader.setHistoryEnabled(true);
        // needed for CTRL+C not to let the console broken
        consoleReader.getTerminal().setEchoEnabled(true);

        final GeogitCLI cli = new GeogitCLI(consoleReader);
        final JCommander globalCommandParser = cli.newCommandParser();

        final Map<String, JCommander> commands = globalCommandParser.getCommands();

        List<Completer> completers = new ArrayList<Completer>(commands.size());
        for (Map.Entry<String, JCommander> entry : commands.entrySet()) {

            String commandName = entry.getKey();
            JCommander commandParser = entry.getValue();

            List<ParameterDescription> parameters = commandParser.getParameters();
            List<String> options = new ArrayList<String>(parameters.size());
            for (ParameterDescription pd : parameters) {
                String longestName = pd.getLongestName();
                options.add(longestName);
            }
            Collections.sort(options);

            ArgumentCompleter commandCompleter = new ArgumentCompleter(new StringsCompleter(
                    commandName), new StringsCompleter(options));
            completers.add(commandCompleter);
        }

        completers.add(new StringsCompleter("exit", "clear"));

        Completer completer = new AggregateCompleter(completers);
        consoleReader.addCompleter(completer);

        String currentDir = new File(".").getCanonicalPath();
        String prompt = "(geogit):" + currentDir + " $ ";
        consoleReader.setPrompt(prompt);

        try {
            runInternal(cli);
        } finally {
            Terminal terminal = consoleReader.getTerminal();
            try {
                cli.close();
            } finally {
                try {
                    terminal.restore();
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    private void runInternal(final GeogitCLI cli) throws IOException {

        final ConsoleReader consoleReader = cli.getConsole();
        while (true) {
            try {
                String line = consoleReader.readLine();
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] args = line.split(" ");

                if (args != null && args.length == 1 && "exit".equals(args[0])) {
                    return;
                }
                if (args != null && args.length == 1 && "clear".equals(args[0])) {
                    consoleReader.clearScreen();
                    consoleReader.redrawLine();
                    continue;
                }

                cli.execute(args);
            } catch (ParameterException pe) {
                consoleReader.print("Error: " + pe.getMessage());
                consoleReader.println();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
