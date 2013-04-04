/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geogit.repository.WorkingTree;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.base.Preconditions;

/**
 *
 */
class GeoGitFeatureWriter implements FeatureWriter<SimpleFeatureType, SimpleFeature> {

    private FeatureReader<SimpleFeatureType, SimpleFeature> reader;

    private WorkingTree workingTree;

    private SimpleFeature last;

    private String typePath;

    private GeoGitFeatureWriter(FeatureReader<SimpleFeatureType, SimpleFeature> reader,
            String typePath) {
        this.reader = reader;
        this.typePath = typePath;
    }

    public static GeoGitFeatureWriter create(
            FeatureReader<SimpleFeatureType, SimpleFeature> reader, String typePath) {
        return new GeoGitFeatureWriter(reader, typePath);
    }

    public static GeoGitFeatureWriter createAppendable(
            FeatureReader<SimpleFeatureType, SimpleFeature> reader, String typePath) {
        return new GeoGitFeatureWriter(new InfiniteFeatureReader(reader), typePath);
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return reader.getFeatureType();
    }

    @Override
    public boolean hasNext() throws IOException {
        return reader.hasNext();
    }

    @Override
    public SimpleFeature next() throws IOException {
        this.last = reader.next();
        return last;
    }

    @Override
    public void remove() throws IOException {
        Preconditions.checkState(last != null, "next() hasn't been called");
        String path = typePath;
        String featureId = last.getID();
        workingTree.delete(path, featureId);
    }

    @Override
    public void write() throws IOException {
        Preconditions.checkState(last != null, "next() hasn't been called");
        String parentTreePath = typePath;
        workingTree.insert(parentTreePath, last);
    }

    @Override
    public void close() throws IOException {
        //
    }

    private static final class InfiniteFeatureReader implements
            FeatureReader<SimpleFeatureType, SimpleFeature> {

        private FeatureReader<SimpleFeatureType, SimpleFeature> reader;

        private SimpleFeatureBuilder newFeaturesBuilder;

        public InfiniteFeatureReader(FeatureReader<SimpleFeatureType, SimpleFeature> reader) {
            this.reader = reader;
            this.newFeaturesBuilder = new SimpleFeatureBuilder(reader.getFeatureType());
        }

        @Override
        public boolean hasNext() throws IOException {
            return reader.hasNext();
        }

        @Override
        public SimpleFeatureType getFeatureType() {
            return reader.getFeatureType();
        }

        @Override
        public SimpleFeature next() throws IOException, IllegalArgumentException,
                NoSuchElementException {
            if (reader.hasNext()) {
                return reader.next();
            }
            SimpleFeature feature = this.newFeaturesBuilder.buildFeature(null);
            return feature;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
