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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.solr.common.util.NamedList;

import com.tomgibara.cluster.gvm.dbl.DblClusters;
import com.tomgibara.cluster.gvm.dbl.DblResult;

public final class ResultFormatter {

    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_REFERENCE = "reference";
    private static final String KEY_SIZE = "size";
    private static final String KEY_TYPE = "type";

    private static final String PIN_TYPE_SINGLE = "single";
    private static final String PIN_TYPE_CLUSTER = "cluster";

    private ResultFormatter() {
        // hide utility class constructor
    }

    public static NamedList<Object> createClusterResult(DblClusters<String> clusters) {
        NamedList<Object> result = new NamedList<>();

        for (DblResult<String> cluster : clusters.results()) {
            NamedList<Object> clusterNode = new NamedList<>();

            clusterNode.add(KEY_TYPE, getType(cluster.getCount()));
            clusterNode.add(KEY_SIZE, cluster.getCount());
            clusterNode.add(KEY_LONGITUDE, cluster.getCoords()[0]);
            clusterNode.add(KEY_LATITUDE, cluster.getCoords()[1]);

            if (cluster.getCount() == 1) {
                clusterNode.add(KEY_REFERENCE, cluster.getKey());
            }

            result.add("pin", clusterNode);
        }

        return result;
    }

    public static List<Map<String, Object>> createCompactClusterResult(DblClusters<String> clusters) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (DblResult<String> cluster : clusters.results()) {
            Map<String, Object> clusterNode = new TreeMap<>();

            clusterNode.put(KEY_TYPE, getType(cluster.getCount()));
            clusterNode.put(KEY_SIZE, cluster.getCount());
            clusterNode.put(KEY_LONGITUDE, cluster.getCoords()[0]);
            clusterNode.put(KEY_LATITUDE, cluster.getCoords()[1]);

            if (cluster.getCount() == 1) {
                clusterNode.put(KEY_REFERENCE, cluster.getKey());
            }

            result.add(clusterNode);
        }

        return result;
    }

    private static String getType(int count) {
        if (count == 1) {
            return PIN_TYPE_SINGLE;
        }

        return PIN_TYPE_CLUSTER;
    }

}
