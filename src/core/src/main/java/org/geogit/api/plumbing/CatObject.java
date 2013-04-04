/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevObject;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.text.TextSerializationFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

/**
 * Provides content information for repository objects
 */
public class CatObject extends AbstractGeoGitOp<CharSequence> {

    private Supplier<? extends RevObject> object;

    public CatObject setObject(Supplier<? extends RevObject> object) {
        this.object = object;
        return this;
    }

    @Override
    public CharSequence call() {
        Preconditions.checkState(object != null);
        RevObject revObject = object.get();

        TextSerializationFactory factory = new TextSerializationFactory();
        ObjectWriter<RevObject> writer = factory.createObjectWriter(revObject.getType());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String s = "id " + revObject.getId().toString() + "\n";
        OutputStreamWriter streamWriter = new OutputStreamWriter(output, Charsets.UTF_8);
        try {
            streamWriter.write(s);
            streamWriter.flush();
            writer.write(revObject, output);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot print object: "
                    + revObject.getId().toString());
        }
        return output.toString();
    }
}
