/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.history.internal;

import com.google.common.base.Optional;

/**
 *
 */
public class Change {

    public enum Type {
        create, modify, delete
    }

    private Node node;

    private Way way;

    private Relation relation;

    private final Type type;

    public Change(Type type, Primitive primitive) {
        this.type = type;
        if (primitive instanceof Node) {
            node = (Node) primitive;
        } else if (primitive instanceof Way) {
            way = (Way) primitive;
        } else if (primitive instanceof Relation) {
            relation = (Relation) primitive;
        } else {
            throw new IllegalArgumentException("Unknown primitive: " + primitive);
        }
    }

    public Type getType() {
        return type;
    }

    public Optional<Node> getNode() {
        return Optional.fromNullable(node);
    }

    public Optional<Way> getWay() {
        return Optional.fromNullable(way);
    }

    public Optional<Relation> getRelation() {
        return Optional.fromNullable(relation);
    }

    void setNode(Node node) {
        this.node = node;
    }

    void setWay(Way way) {
        this.way = way;
    }

    void setRelation(Relation relation) {
        this.relation = relation;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[type:").append(getType())
                .append(", ")
                .append(getNode().isPresent() ? node : (getWay().isPresent() ? way : relation))
                .append("]").toString();
    }
}
