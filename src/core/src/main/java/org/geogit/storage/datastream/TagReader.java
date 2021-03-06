/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;

import com.google.common.base.Throwables;

import org.geogit.api.ObjectId;
import org.geogit.api.RevTag;
import org.geogit.storage.ObjectReader;

public class TagReader implements ObjectReader<RevTag> {
    public RevTag read(ObjectId id, InputStream in) {
        DataInput data = new DataInputStream(in);
        try {
            FormatCommon.requireHeader(data, "tag");
            return FormatCommon.readTag(id, data);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
