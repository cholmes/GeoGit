/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.storage;

import org.geogit.api.Node;

import com.google.common.collect.Ordering;

/**
 * Implements storage order of {@link Node} based on its {@link #pathHash(Node) hashed path}
 */
public final class NodeStorageOrder extends Ordering<Node> {

    private final NodePathStorageOrder pathOrder = new NodePathStorageOrder();

    @Override
    public int compare(Node nr1, Node nr2) {
        return pathOrder.compare(nr1.getName(), nr2.getName());
    }

    /**
     * @see NodePathStorageOrder#bucket(String, int)
     */
    public Integer bucket(final Node ref, final int depth) {
        return pathOrder.bucket(ref.getName(), depth);
    }
}