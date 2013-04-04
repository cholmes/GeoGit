/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import static org.geogit.storage.datastream.FormatCommon.writeBucket;
import static org.geogit.storage.datastream.FormatCommon.writeHeader;
import static org.geogit.storage.datastream.FormatCommon.writeNode;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

public class TreeWriter implements ObjectWriter<RevTree> {
    @Override
    public void write(RevTree tree, OutputStream out) throws IOException {
        DataOutput data = new DataOutputStream(out);
        writeHeader(data, "tree");
        data.writeLong(tree.size());
        data.writeInt(tree.numTrees());
        if (tree.features().isPresent()) {
            data.writeInt(tree.features().get().size());
            ImmutableList<Node> features = tree.features().get();
            for (Node feature : features) {
                writeNode(feature, data);
            }
        } else {
            data.writeInt(0);
        }
        if (tree.trees().isPresent()) {
            data.writeInt(tree.trees().get().size());
            ImmutableList<Node> subTrees = tree.trees().get();
            for (Node subTree : subTrees) {
                writeNode(subTree, data);
            }
        } else {
            data.writeInt(0);
        }
        if (tree.buckets().isPresent()) {
            data.writeInt(tree.buckets().get().size());
            ImmutableSortedMap<Integer, Bucket> buckets = tree.buckets().get();
            for (Map.Entry<Integer, Bucket> bucket : buckets.entrySet()) {
                writeBucket(bucket.getKey(), bucket.getValue(), data);
            }
        } else {
            data.writeInt(0);
        }
    }
}
