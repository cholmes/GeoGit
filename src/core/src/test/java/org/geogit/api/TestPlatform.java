/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.File;

public class TestPlatform extends DefaultPlatform implements Platform {

    private File workingDirectory;

    private File userHomeDirectory;

    public TestPlatform(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.userHomeDirectory = new File(workingDirectory, "userhome");
        this.userHomeDirectory.mkdir();
    }

    @Override
    public File pwd() {
        return workingDirectory;
    }

    @Override
    public File getUserHome() {
        return userHomeDirectory;
    }

    public void setUserHome(File userHomeDirectory) {
        this.userHomeDirectory = userHomeDirectory;
    }
}
