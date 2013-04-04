/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.File;
import java.net.URI;

import org.geogit.api.Remote;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Injector;

/**
 * Provides utilities for creating interfaces to remote repositories.
 */
public class RemoteUtils {

    /**
     * Constructs an interface to allow access to a remote repository.
     * 
     * @param injector a Guice injector for the new repository
     * @param remoteConfig the remote to connect to
     * @return an {@link Optional} of the interface to the remote repository, or
     *         {@link Optional#absent()} if a connection to the remote could not be established.
     */
    public static Optional<IRemoteRepo> newRemote(Injector injector, Remote remoteConfig) {

        try {
            URI fetchURI = URI.create(remoteConfig.getFetchURL());
            String protocol = fetchURI.getScheme();

            IRemoteRepo remoteRepo = null;
            if (protocol == null || protocol.equals("file")) {
                remoteRepo = new LocalRemoteRepo(injector, new File(remoteConfig.getFetchURL()));
            } else if (protocol.equals("http")) {
                remoteRepo = new HttpRemoteRepo(fetchURI.toURL());
            } else {
                throw new UnsupportedOperationException(
                        "Only file and http remotes are currently supported.");
            }
            return Optional.fromNullable(remoteRepo);
        } catch (Exception e) {
            // Invalid fetch URL
            Throwables.propagate(e);
        }

        return Optional.absent();
    }

}
