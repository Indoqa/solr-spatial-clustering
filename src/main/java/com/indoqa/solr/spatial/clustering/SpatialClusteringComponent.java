/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indoqa.solr.spatial.clustering;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.util.plugin.PluginInfoInitialized;

import com.tomgibara.cluster.gvm.dbl.DblClusters;
import com.tomgibara.cluster.gvm.dbl.DblResult;

public class SpatialClusteringComponent extends SearchComponent implements PluginInfoInitialized {

    public static final String PARAMETER_SPATIALCLUSTERING = "spatial-clustering";
    public static final String PARAMETER_SIZE = PARAMETER_SPATIALCLUSTERING + ".size";
    public static final String PARAMETER_MIN_RESULT_COUNT = PARAMETER_SPATIALCLUSTERING + ".min-result-count";

    private static final String PARAMETER_FIELD_NAME_ID = "fieldId";
    private static final String PARAMETER_FIELD_NAME_LON = "fieldLon";
    private static final String PARAMETER_FIELD_NAME_LAT = "fieldLat";
    private static final String PARAMETER_MAX_SIZE = "maxSize";

    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_MAX_SIZE = 1_000_000;
    private static final int MIN_SIZE = 1;

    private static final String PIN_TYPE_SINGLE = "single";
    private static final String PIN_TYPE_CLUSTER = "cluster";

    private String fieldNameId;
    private String fieldNameLon;
    private String fieldNameLat;

    private int maxSize;

    private Set<String> fields;

    private static Number getFieldNumber(Document document, String name) {
        IndexableField field = document.getField(name);
        if (field == null) {
            return null;
        }

        return field.numericValue();
    }

    private static String getFieldString(Document document, String name) {
        IndexableField field = document.getField(name);
        if (field == null) {
            return null;
        }

        return field.stringValue();
    }

    private static int getIntArgument(NamedList<?> values, String name, int minValue, int defaultValue) {
        Object value = values.get(name);
        if (value == null) {
            return defaultValue;
        }

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Value for parameter '" + name + "' must be a number.");
        }

        int result = ((Number) value).intValue();
        if (result < minValue) {
            throw new IllegalArgumentException(
                "Value for parameter '" + name + "' must be at least " + minValue + ", but it was " + result + ".");
        }

        return result;

    }

    private static String getStringArgument(NamedList<?> values, String fieldName) {
        Object value = values.get(fieldName);
        if (value == null) {
            throw new IllegalStateException("No value for parameter '" + fieldName + "' specified in solrconfig.xml!");
        }

        String result = String.valueOf(value);
        if (result.trim().length() == 0) {
            throw new IllegalStateException("Value for parameter '" + fieldName + "' specified in solrconfig.xml was empty!");
        }

        return result;
    }

    private static String getType(int count) {
        if (count == 1) {
            return PIN_TYPE_SINGLE;
        }

        return PIN_TYPE_CLUSTER;
    }

    private static boolean isEnabled(ResponseBuilder responseBuilder) {
        return responseBuilder.req.getParams().getBool(PARAMETER_SPATIALCLUSTERING, false);
    }

    @Override
    public String getDescription() {
        return "indoqa-spatial-clustering";
    }

    @Override
    public void init(PluginInfo info) {
        this.fieldNameId = getStringArgument(info.initArgs, PARAMETER_FIELD_NAME_ID);
        this.fieldNameLon = getStringArgument(info.initArgs, PARAMETER_FIELD_NAME_LON);
        this.fieldNameLat = getStringArgument(info.initArgs, PARAMETER_FIELD_NAME_LAT);

        this.fields = new HashSet<>();
        this.fields.add(this.fieldNameId);
        this.fields.add(this.fieldNameLon);
        this.fields.add(this.fieldNameLat);

        this.maxSize = getIntArgument(info.initArgs, PARAMETER_MAX_SIZE, MIN_SIZE, DEFAULT_MAX_SIZE);
    }

    @Override
    public void prepare(ResponseBuilder responseBuilder) throws IOException {
        if (!isEnabled(responseBuilder)) {
            return;
        }

        responseBuilder.setNeedDocSet(true);
    }

    @Override
    public void process(ResponseBuilder responseBuilder) throws IOException {
        if (!isEnabled(responseBuilder)) {
            return;
        }

        int minResultCount = responseBuilder.req.getParams().getInt(PARAMETER_MIN_RESULT_COUNT, 1);
        if (responseBuilder.getResults().docSet.size() < minResultCount) {
            return;
        }

        int size = responseBuilder.req.getParams().getInt(PARAMETER_SIZE, DEFAULT_SIZE);
        if (size > this.maxSize) {
            throw new IllegalArgumentException(
                "The requested size is larger than " + this.maxSize + ". Consider changing " + PARAMETER_MAX_SIZE
                    + " in the plugin configuration.");
        }

        if (size < MIN_SIZE) {
            throw new IllegalArgumentException("The requested size must be at least " + MIN_SIZE + ".");
        }

        DblClusters<String> clusters = this.createClusters(responseBuilder, size);
        NamedList<Object> spatialClusteringRoot = this.createClusterResult(clusters);

        responseBuilder.rsp.add("spatial-clustering", spatialClusteringRoot);
    }

    private NamedList<Object> createClusterResult(DblClusters<String> clusters) {
        NamedList<Object> result = new NamedList<>();

        for (DblResult<String> cluster : clusters.results()) {
            NamedList<Object> clusterNode = new NamedList<>();

            clusterNode.add("type", getType(cluster.getCount()));
            clusterNode.add("size", cluster.getCount());
            clusterNode.add("longitude", cluster.getCoords()[0]);
            clusterNode.add("latitude", cluster.getCoords()[1]);

            if (cluster.getCount() == 1) {
                clusterNode.add("reference", cluster.getKey());
            }

            result.add("pin", clusterNode);
        }

        return result;
    }

    private DblClusters<String> createClusters(ResponseBuilder responseBuilder, int size) throws IOException {
        DblClusters<String> result = new DblClusters<>(2, size);

        for (Iterator<Integer> iterator = responseBuilder.getResults().docSet.iterator(); iterator.hasNext();) {
            Document doc = responseBuilder.req.getSearcher().doc(iterator.next(), this.fields);

            Number latitude = getFieldNumber(doc, this.fieldNameLat);
            if (latitude == null) {
                continue;
            }

            Number longitude = getFieldNumber(doc, this.fieldNameLon);
            if (longitude == null) {
                continue;
            }

            result.add(1, new double[] {longitude.doubleValue(), latitude.doubleValue()}, getFieldString(doc, this.fieldNameId));
        }

        return result;
    }
}
