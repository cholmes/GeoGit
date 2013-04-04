/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.api;

import com.vividsolutions.jts.geom.Envelope;

/**
 *
 */
public interface Bounded {

    public boolean intersects(Envelope env);

    public void expand(Envelope env);
}
