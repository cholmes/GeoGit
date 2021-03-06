package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class SendObjectResource extends ServerResource {

    @Post
    public Representation acceptObject(Representation entity) throws IOException {
        Representation result = null;

        ClientInfo info = getRequest().getClientInfo();

        Form options = getRequest().getResourceRef().getQueryAsForm();
        // make a combined ip address to handle requests from multiple machines in the same
        // external network.
        // e.g.: ext.ern.al.IP.int.ern.al.IP
        String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");

        InputStream input = entity.getStream();
        byte objectIdBytes[] = new byte[20];
        input.read(objectIdBytes, 0, 20);
        ObjectId objectId = new ObjectId(objectIdBytes);

        final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
        PushManager pushManager = PushManager.get();
        if (ggit.getRepository().getObjectDatabase().exists(objectId)) {
            result = new StringRepresentation("Object already existed: " + objectId.toString());

        } else {
            // put it into the staging database until we have all of the data
            ggit.getRepository().getIndex().getDatabase().put(objectId, input);
            pushManager.addObject(ipAddress, objectId);
            result = new StringRepresentation("Object added: " + objectId.toString());
        }

        return result;
    }
}
