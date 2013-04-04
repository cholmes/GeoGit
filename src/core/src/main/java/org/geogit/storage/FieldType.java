/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved. 
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Enumeration with types supported for attribute values
 * 
 * 
 */
public enum FieldType {

    NULL(0x00, Void.class), //
    BOOLEAN(0x01, Boolean.class), //
    BYTE(0x02, Byte.class), //
    SHORT(0x03, Short.class), //
    INTEGER(0x04, Integer.class), //
    LONG(0x05, Long.class), //
    FLOAT(0x06, Float.class), //
    DOUBLE(0x07, Double.class), //
    STRING(0x08, String.class), //
    BOOLEAN_ARRAY(0x09, boolean[].class), //
    BYTE_ARRAY(0x0A, byte[].class), //
    SHORT_ARRAY(0x0B, short[].class), //
    INTEGER_ARRAY(0x0C, int[].class), //
    LONG_ARRAY(0x0D, long[].class), //
    FLOAT_ARRAY(0x0E, float[].class), //
    DOUBLE_ARRAY(0x0F, double[].class), //
    STRING_ARRAY(0x10, String[].class), //
    POINT(0x11, Point.class), //
    LINESTRING(0x12, LineString.class), //
    POLYGON(0x13, Polygon.class), //
    MULTIPOINT(0x14, MultiPoint.class), //
    MULTILINESTRING(0x15, MultiLineString.class), //
    MULTIPOLYGON(0x16, MultiPolygon.class), //
    GEOMETRYCOLLECTION(0x17, GeometryCollection.class), //
    GEOMETRY(0x18, Geometry.class), //
    UUID(0x19, java.util.UUID.class), //
    BIG_INTEGER(0x1A, BigInteger.class), //
    BIG_DECIMAL(0x1B, BigDecimal.class), //
    DATETIME(0x1C, Date.class), //
    DATE(0x1D, java.sql.Date.class), //
    TIME(0x1E, java.sql.Time.class), //
    TIMESTAMP(0x1F, java.sql.Timestamp.class);

    private final byte tagValue;

    private final Class<?> binding;

    private static final Map<Class<?>, FieldType> BINDING_MAPPING = Maps.newHashMap();
    static {
        for (FieldType t : FieldType.values()) {
            BINDING_MAPPING.put(t.getBinding(), t);
        }
    }

    private FieldType(int tagValue, Class<?> binding) {
        this.tagValue = (byte) tagValue;
        this.binding = binding;
    }

    public Class<?> getBinding() {
        return binding;
    }

    public byte getTag() {
        return tagValue;
    }

    public static FieldType valueOf(int i) {
        return values()[i];
    }

    public static FieldType forValue(Optional<Object> field) {
        if (field.isPresent()) {
            Object realField = field.get();
            Class<?> fieldClass = realField.getClass();
            return forBinding(fieldClass);
        } else {
            return NULL;
        }
    }

    public static FieldType forBinding(Class<?> binding) {
        // try a hash lookup first
        FieldType fieldType = BINDING_MAPPING.get(binding);
        if (fieldType != null) {
            return fieldType;
        }
        // not in the map, lets check one by one anyways
        // beware for this to work properly FieldTypes for super classes must be defined _after_
        // any subclass (i.e. Point before Geometry)
        for (FieldType t : values()) {
            if (t.getBinding().isAssignableFrom(binding)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Attempted to write unsupported field type " + binding);
    }

}
