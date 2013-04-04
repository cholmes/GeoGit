/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.osm.history.internal;

import com.google.common.base.Optional;
import com.vividsolutions.jts.geom.Point;

/**
 *
 */
public class Node extends Primitive {

    /** WGS84 location, lon/lat axis order */
    private Point location;

    /** WGS84 location, lon/lat axis order */
    public Optional<Point> getLocation() {
        return Optional.fromNullable(location);
    }

    /**
     * @param location point location, in lon/lat ordinate order
     */
    void setLocation(Point location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(",location:").append(location)
                .append("]").toString();
    }
}
