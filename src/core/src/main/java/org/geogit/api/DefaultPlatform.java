/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.TimeZone;

/**
 * Standard platform for GeoGit.
 */
public class DefaultPlatform implements Platform {

    private File workingDir;

    /**
     * @return the working directory
     */
    @Override
    public File pwd() {
        if (workingDir != null) {
            return workingDir;
        }
        return new File(".").getAbsoluteFile().getParentFile();
    }

    /**
     * @param workingDir the working directory to use
     */
    @Override
    public void setWorkingDir(File workingDir) {
        checkArgument(workingDir == null || workingDir.isDirectory(),
                "file does not exist or is not a directory: " + workingDir);
        this.workingDir = workingDir;
    }

    /**
     * @see Platform#whoami()
     */
    @Override
    public String whoami() {
        return System.getProperty("user.name", "nobody");
    }

    /**
     * @return the current time in milliseconds
     * @see org.geogit.api.Platform#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @return the user home directory
     */
    @Override
    public File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    @Override
    public int timeZoneOffset(long timeStamp) {
        return TimeZone.getDefault().getOffset(timeStamp);
    }

}
