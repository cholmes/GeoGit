/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

/**
 * Encapsulates and describes the arguments for the {@link Log} command
 */
public class LogArgs {

    @Parameter(names = { "--max-count", "-n" }, description = "Maximum number of commits to log.")
    @Nullable
    public Integer limit;

    @Parameter(names = "--skip", description = "Skip number commits before starting to show the commit output.")
    @Nullable
    public Integer skip;

    @Parameter(names = "--since", description = "Maximum number of commits to log")
    @Nullable
    public String since;

    @Parameter(names = "--until", description = "Maximum number of commits to log")
    @Nullable
    public String until;

    @Parameter(names = "--oneline", description = "Print only commit id and message on a sinlge line per commit")
    public boolean oneline;

    @Parameter(description = "[[<until>]|[<since>..<until>]], arity = 1")
    public List<String> sinceUntilPaths = Lists.newArrayList();

    @Parameter(names = { "--path", "-p" }, description = "Print only commits that have modified the given path(s)", variableArity = true)
    public List<String> pathNames = Lists.newArrayList();

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    @Nullable
    public ColorArg color = ColorArg.auto;

    @Parameter(names = "--raw", description = "Show raw contents for commits")
    @Nullable
    public boolean raw;
}
