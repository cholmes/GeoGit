/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import javax.annotation.Nullable;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.expression.Literal;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 *
 */
class ExtractBounds extends DefaultFilterVisitor {

    @Override
    public @Nullable
    Envelope visit(Literal literal, @Nullable Object data) {

        Envelope env = (Envelope) data;
        Object value = literal.getValue();
        if (value instanceof Geometry) {
            if (env == null) {
                env = new Envelope();
            }
            Envelope literalEnvelope = ((Geometry) value).getEnvelopeInternal();
            env.expandToInclude(literalEnvelope);
        }
        return env;
    }
}
