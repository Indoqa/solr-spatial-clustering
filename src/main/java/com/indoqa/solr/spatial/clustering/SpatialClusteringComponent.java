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
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.util.plugin.PluginInfoInitialized;

import com.tomgibara.cluster.gvm.dbl.DblClusters;
import com.tomgibara.cluster.gvm.dbl.DblResult;

public class SpatialClusteringComponent extends SearchComponent implements PluginInfoInitialized {

    private static final String FIELD_NAME_ID = "fieldId";
    private static final String FIELD_NAME_LON = "fieldLon";
    private static final String FIELD_NAME_LAT = "fieldLat";

    private static final String PARAM_SPATIALCLUSTERING = "spatial-clustering";
    private static final String PARAM_SPATIALCLUSTERING_SIZE = "spatial-clustering.size";

    private static final int DEFAULT_CLUSTER_SIZE = 10;

    private static final String PIN_TYPE_SINGLE = "single";
    private static final String PIN_TYPE_CLUSTER = "cluster";

    private String fieldNameId;
    private String fieldNameLon;
    private String fieldNameLat;

    @Override
    public String getDescription() {
        return "indoqa-spatial-clustering";
    }

    @Override
    public String getSource() {
        return "indoqa-spatial-clustering";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public void init(PluginInfo info) {
        this.fieldNameId = this.getStringArgument(info.initArgs, FIELD_NAME_ID);
        this.fieldNameLon = this.getStringArgument(info.initArgs, FIELD_NAME_LON);
        this.fieldNameLat = this.getStringArgument(info.initArgs, FIELD_NAME_LAT);
    }

    @Override
    public void prepare(ResponseBuilder rb) throws IOException {
        if (rb.req.getParams().getBool(PARAM_SPATIALCLUSTERING, false)) {
            rb.setNeedDocSet(true);
        }
    }

    @Override
    public void process(ResponseBuilder responseBuilder) throws IOException {
        if (!responseBuilder.req.getParams().getBool(PARAM_SPATIALCLUSTERING, false)) {
            return;
        }

        int maxCount = responseBuilder.req.getParams().getInt(PARAM_SPATIALCLUSTERING_SIZE, DEFAULT_CLUSTER_SIZE);
        Set<String> fields = this.createFieldList();

        DblClusters<Document> clusters = this.createDocumentClusters(responseBuilder, maxCount, fields);
        NamedList<Object> spatialClusteringRoot = this.createClusterResult(clusters);

        responseBuilder.rsp.add("spatial-clustering", spatialClusteringRoot);
    }

    private NamedList<Object> createClusterResult(DblClusters<Document> clusters) {
        NamedList<Object> spatialClusteringRoot = new NamedList<>();

        for (DblResult<Document> cluster : clusters.results()) {
            NamedList<Object> clusterNode = new NamedList<>();

            clusterNode.add("type", this.getType(cluster.getCount()));
            clusterNode.add("size", cluster.getCount());
            clusterNode.add("longitude", cluster.getCoords()[0]);
            clusterNode.add("latitude", cluster.getCoords()[1]);

            if (cluster.getCount() == 1) {
                clusterNode.add("reference", this.getFieldString(cluster, this.fieldNameId));
            }

            spatialClusteringRoot.add("pin", clusterNode);
        }
        return spatialClusteringRoot;
    }

    private DblClusters<Document> createDocumentClusters(ResponseBuilder rb, int maxCount, Set<String> fields) throws IOException {
        DblClusters<Document> clusters = new DblClusters<>(2, maxCount);

        DocSet docSet = rb.getResults().docSet;
        DocIterator iterator = docSet.iterator();

        while (iterator.hasNext()) {
            Integer docId = iterator.next();
            Document doc = rb.req.getSearcher().doc(docId, fields);

            IndexableField latitudeField = doc.getField(this.fieldNameLat);
            IndexableField longitudeField = doc.getField(this.fieldNameLon);

            if (latitudeField == null || longitudeField == null) {
                continue;
            }

            String latitudeString = latitudeField.stringValue();
            String longitudeString = longitudeField.stringValue();

            if (!this.isNumeric(latitudeString) || !this.isNumeric(longitudeString)) {
                continue;
            }

            clusters.add(1, new double[] {Double.valueOf(latitudeString), Double.valueOf(longitudeString)}, doc);
        }
        return clusters;
    }

    private Set<String> createFieldList() {
        Set<String> fields = new HashSet<>();

        fields.add(this.fieldNameId);
        fields.add(this.fieldNameLon);
        fields.add(this.fieldNameLat);

        return fields;
    }

    private String getFieldString(DblResult<Document> dblResult, String name) {
        IndexableField fieldable = dblResult.getKey().getField(name);

        if (fieldable == null) {
            return null;
        }

        return fieldable.stringValue();
    }

    private String getStringArgument(NamedList<?> values, String fieldName) {
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

    private Object getType(int count) {
        if (count == 1) {
            return PIN_TYPE_SINGLE;
        }

        return PIN_TYPE_CLUSTER;
    }

    private boolean isNumeric(String s) {
        return s != null && Pattern.matches("-?\\d+(\\.\\d+)?", s);
    }
}
