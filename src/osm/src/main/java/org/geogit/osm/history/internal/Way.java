/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.history.internal;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 */
public class Way extends Primitive {

    private List<Long> nodes;

    public Way() {
        super();
        this.nodes = Lists.newLinkedList();
    }

    /**
     * @param nodeRef
     */
    void addNode(long nodeRef) {
        nodes.add(Long.valueOf(nodeRef));
    }

    public ImmutableList<Long> getNodes() {
        return ImmutableList.copyOf(nodes);
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(",nodes:").append(nodes).append("]")
                .toString();
    }

}
