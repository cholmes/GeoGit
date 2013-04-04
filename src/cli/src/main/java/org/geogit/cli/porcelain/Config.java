/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.ConfigOp.ConfigScope;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 * You can query/set/unset options with this command. The name is actually the section and the key
 * separated by a dot, and the value will be escaped. By default, the config file of the current
 * repository will be assumed. If the --global option is set, the global .geogitconfig file will be
 * used.
 * <p>
 * CLI proxy for {@link ConfigOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit config [--global] name [value]}: retrieves or sets the config variable
 * specified by name
 * <li> {@code geogit config [--global] --get name}: retrieves the config variable specified by name
 * <li> {@code geogit config [--global] --unset name}: removes the config variable specified by name
 * <li> {@code geogit config [--global] --remove-section name}: removes the config section specified
 * by name
 * <li> {@code geogit config [--global] -l}: lists all config variables
 * </ul>
 * 
 * @see ConfigOp
 */
@Parameters(commandNames = "config", commandDescription = "Get and set repository or global options")
public class Config extends AbstractCommand implements CLICommand {

    @Parameter(names = "--global", description = "Use global config file.")
    private boolean global = false;

    @Parameter(names = "--local", description = "Use repository config file.")
    private boolean local = false;

    @Parameter(names = "--get", description = "Get the value for a given key.")
    private boolean get = false;

    @Parameter(names = "--unset", description = "Remove the line matching the given key.")
    private boolean unset = false;

    @Parameter(names = "--remove-section", description = "Remove the given section.")
    private boolean remove_section = false;

    @Parameter(names = { "--list", "-l" }, description = "List all variables.")
    private boolean list = false;

    @Parameter(description = "name value (name is section.key format, value is only required when setting)")
    private List<String> nameValuePair;

    /**
     * Executes the config command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {

        GeoGIT geogit = cli.getGeogit();
        if (null == geogit) {
            // we're not in a repository, need a geogit anyways to run the global commands
            geogit = cli.newGeoGIT();
        }

        try {
            String name = null;
            String value = null;
            if (nameValuePair != null && !nameValuePair.isEmpty()) {
                name = nameValuePair.get(0);
                value = buildValueString();
            }

            ConfigAction action = resolveConfigAction();

            if (action == ConfigAction.CONFIG_NO_ACTION) {
                printUsage();
                return;
            }
            if (global && local) {
                printUsage();
                return;
            }
            ConfigScope scope = ConfigScope.DEFAULT;

            if (global) {
                scope = ConfigScope.GLOBAL;
            } else if (local) {
                scope = ConfigScope.LOCAL;
            }

            final Optional<Map<String, String>> commandResult = geogit.command(ConfigOp.class)
                    .setScope(scope).setAction(action).setName(name).setValue(value).call();

            if (commandResult.isPresent()) {
                switch (action) {
                case CONFIG_GET: {
                    cli.getConsole().println(commandResult.get().get(name));
                    break;
                }
                case CONFIG_LIST: {
                    Iterator<Map.Entry<String, String>> it = commandResult.get().entrySet()
                            .iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
                        cli.getConsole().println(pairs.getKey() + "=" + pairs.getValue());
                    }
                    break;
                }
                default:
                    break;
                }
            }
        } catch (ConfigException e) {
            // These mirror 'git config' status codes. Some of these are unused,
            // since we don't have regex support yet.
            switch (e.statusCode) {
            case INVALID_LOCATION:
                // TODO: This could probably be more descriptive.
                cli.getConsole().println("The config location is invalid");
                break;
            case CANNOT_WRITE:
                cli.getConsole().println("Cannot write to the config");
                break;
            case SECTION_OR_NAME_NOT_PROVIDED:
                cli.getConsole().println("No section or name was provided");
                break;
            case SECTION_OR_KEY_INVALID:
                cli.getConsole().println("The section or key is invalid");
                break;
            case OPTION_DOES_N0T_EXIST:
                cli.getConsole().println("Tried to unset an option that does not exist");
                break;
            case MULTIPLE_OPTIONS_MATCH:
                cli.getConsole().println(
                        "Tried to unset/set an option for which multiple lines match");
                break;
            case INVALID_REGEXP:
                cli.getConsole().println("Tried to use an invalid regexp");
                break;
            case USERHOME_NOT_SET:
                cli.getConsole().println("Used --global option without $HOME being properly set");
                break;
            case TOO_MANY_ACTIONS:
                cli.getConsole().println("Tried to use more than one action at a time");
                break;
            case MISSING_SECTION:
                cli.getConsole().println("Could not find a section with the name provided");
                break;
            case TOO_MANY_ARGS:
                cli.getConsole().println("Too many arguments provided.");
                break;
            }
        }
    }

    /**
     * Determines which action should be set based on the state of several option flags.
     * 
     * @return the determined ConfigAction
     * @see ConfigAction
     */
    private ConfigAction resolveConfigAction() {
        ConfigAction action = ConfigAction.CONFIG_NO_ACTION;
        if (get) {
            action = ConfigAction.CONFIG_GET;
        }
        if (unset) {
            if (action != ConfigAction.CONFIG_NO_ACTION)
                throw new ConfigException(StatusCode.TOO_MANY_ACTIONS);
            action = ConfigAction.CONFIG_UNSET;
        }
        if (remove_section) {
            if (action != ConfigAction.CONFIG_NO_ACTION)
                throw new ConfigException(StatusCode.TOO_MANY_ACTIONS);
            action = ConfigAction.CONFIG_REMOVE_SECTION;
        }
        if (list) {
            if (action != ConfigAction.CONFIG_NO_ACTION)
                throw new ConfigException(StatusCode.TOO_MANY_ACTIONS);
            action = ConfigAction.CONFIG_LIST;
        }
        if (action == ConfigAction.CONFIG_NO_ACTION && nameValuePair != null) {
            if (nameValuePair.size() == 1) {
                action = ConfigAction.CONFIG_GET;
            } else if (nameValuePair.size() > 1) {
                action = ConfigAction.CONFIG_SET;
            }
        }
        return action;
    }

    /**
     * Builds a single string out of all of the string parameters after the first one.
     * 
     * @return the concatenated value string
     */
    private String buildValueString() {
        if (nameValuePair.isEmpty())
            return null;

        ArrayList<String> arrayCopy = new ArrayList<String>(nameValuePair);
        arrayCopy.remove(0); // Remove name

        if (arrayCopy.isEmpty())
            return null;

        Joiner stringJoiner = Joiner.on(" ");
        return stringJoiner.join(arrayCopy);
    }

}
