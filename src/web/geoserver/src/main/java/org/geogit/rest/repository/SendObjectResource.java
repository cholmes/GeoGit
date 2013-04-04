/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import static org.geogit.rest.repository.GeogitResourceUtils.getGeogit;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.StagingDatabase;
import org.geogit.web.api.commands.PushManager;
import org.geoserver.rest.RestletException;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

/**
 *
 */
public class SendObjectResource extends Resource {

    @Override
    public boolean allowPost() {
        return true;
    }

    public void post(Representation entity) {

        final Request request = getRequest();
        final Response response = getResponse();

        ClientInfo info = request.getClientInfo();
        Form options = getRequest().getResourceRef().getQueryAsForm();
        // make a combined ip address to handle requests from multiple machines in the same
        // external network.
        // e.g.: ext.ern.al.IP.int.ern.al.IP
        String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");
        byte objectIdBytes[] = new byte[20];
        InputStream input;
        try {
            input = entity.getStream();
        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }

        try {
            input.read(objectIdBytes, 0, 20);
            ObjectId objectId = new ObjectId(objectIdBytes);

            final Optional<GeoGIT> ggit = getGeogit(request);
            Preconditions.checkState(ggit.isPresent());

            PushManager pushManager = PushManager.get();

            Representation result;
            GeoGIT geogti = ggit.get();
            Repository repository = geogti.getRepository();

            if (repository.blobExists(objectId)) {
                result = new StringRepresentation("Object already existed: " + objectId.toString());

            } else {
                // put it into the staging database until we have all of the data
                StagingArea index = repository.getIndex();
                StagingDatabase stagingDatabase = index.getDatabase();
                stagingDatabase.put(objectId, input);
                pushManager.addObject(ipAddress, objectId);
                result = new StringRepresentation("Object added: " + objectId.toString());
            }

            response.setEntity(result);

        } catch (Exception e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            Closeables.closeQuietly(input);
        }
    }
}
