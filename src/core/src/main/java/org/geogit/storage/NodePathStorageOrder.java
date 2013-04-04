/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.WeakHashMap;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;

import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;

/**
 * Implements storage order of {@link Node} based on its {@link #pathHash(Node) hashed path}
 */
public final class NodePathStorageOrder extends Ordering<String> {

    private final MessageDigest hasher;

    public NodePathStorageOrder() {
        try {
            hasher = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public int compare(String p1, String p2) {
        ObjectId left = pathHash(p1);
        ObjectId right = pathHash(p2);
        return left.compareTo(right);
    }

    // private Cache<String, ObjectId> cache = CacheBuilder.newBuilder().maximumSize(1000).build();
    private Map<String, ObjectId> cache = new WeakHashMap<String, ObjectId>();

    private ObjectId pathHash(final String path) {
        ObjectId pathHash = cache.get(path);// .getIfPresent(path);
        if (pathHash == null) {
            hasher.reset();
            hasher.update(path.getBytes(Charset.forName("UTF-8")));
            pathHash = new ObjectId(hasher.digest());
            cache.put(path, pathHash);
        }
        return pathHash;
    }

    /**
     * Computes the bucket index that corresponds to the given node name at the given depth.
     * 
     * @return and Integer between zero and {@link RevTree#MAX_BUCKETS} minus one
     */
    public Integer bucket(final String nodeName, final int depth) {

        final int byteN = pathHash(nodeName).byteN(depth);// 0-255
        final int maxBuckets = RevTree.MAX_BUCKETS;

        final int bucket = (byteN * maxBuckets) / 256;
        return Integer.valueOf(bucket);
    }
}