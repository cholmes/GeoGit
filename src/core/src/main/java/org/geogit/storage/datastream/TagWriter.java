/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;

import com.google.common.base.Throwables;

import org.geogit.api.RevTag;
import org.geogit.storage.ObjectWriter;

public class TagWriter implements ObjectWriter<RevTag> {
    public void write(RevTag tag, OutputStream out) {
        final DataOutput data = new DataOutputStream(out);
        try {
            FormatCommon.writeHeader(data, "tag");
            FormatCommon.writeTag(tag, data);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
