/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class EnvironmentBuilder implements Provider<Environment> {

    @Inject
    private Platform platform;

    private String[] path;

    private File absolutePath;

    public EnvironmentBuilder setRelativePath(String... path) {
        this.path = path;
        this.absolutePath = null;
        return this;
    }

    public EnvironmentBuilder setAbsolutePath(File absolutePath) {
        this.absolutePath = absolutePath;
        this.path = null;
        return this;
    }

    /**
     * @return
     * @see com.google.inject.Provider#get()
     */
    @Override
    public synchronized Environment get() {

        final URL repoUrl = new ResolveGeogitDir(platform).call();
        if (repoUrl == null && absolutePath == null) {
            throw new IllegalStateException("Can't find geogit repository home");
        }
        final File storeDirectory;

        if (absolutePath != null) {
            storeDirectory = absolutePath;
        } else {
            File currDir;
            try {
                currDir = new File(repoUrl.toURI());
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
            File dir = currDir;
            for (String subdir : path) {
                dir = new File(dir, subdir);
            }
            storeDirectory = dir;
        }

        if (!storeDirectory.exists() && !storeDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create Environment directory: '"
                    + storeDirectory.getAbsolutePath() + "'");
        }
        EnvironmentConfig envCfg;
        File envConfigFile = new File(storeDirectory, "environment.properties");
        if (envConfigFile.exists()) {
            // use the environment setttings
            Properties jeProps = new Properties();
            InputStream in = null;
            try {
                in = new FileInputStream(envConfigFile);
                jeProps.load(in);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                Closeables.closeQuietly(in);
            }
            envCfg = new EnvironmentConfig(jeProps);
        } else {
            // use the default settings
            envCfg = new EnvironmentConfig();
            envCfg.setAllowCreate(true);
            envCfg.setCacheMode(CacheMode.MAKE_COLD);
            envCfg.setLockTimeout(1000, TimeUnit.MILLISECONDS);
            envCfg.setDurability(Durability.COMMIT_WRITE_NO_SYNC);
            envCfg.setSharedCache(true);
            envCfg.setTransactional(true);
            envCfg.setCachePercent(50);// Use up to 50% of the heap size for the shared db cache
            envCfg.setConfigParam("je.log.fileMax", String.valueOf(256 * 1024 * 1024));
            // check <http://www.oracle.com/technetwork/database/berkeleydb/je-faq-096044.html#35>
            envCfg.setConfigParam("je.evictor.lruOnly", "false");
            envCfg.setConfigParam("je.evictor.nodesPerScan", "100");
        }

        Environment env = new Environment(storeDirectory, envCfg);
        return env;
    }

}
