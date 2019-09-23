package com.indoqa.solr.spatial.clustering;

import static com.indoqa.solr.spatial.clustering.SpatialClusteringComponent.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ClusteringTest {

    private static final int TOTAL_DOC_COUNT = 1000;

    private static final String SOLR_FIELD_ID = "id";
    private static final String SOLR_FIELD_LAT = "lat";
    private static final String SOLR_FIELD_LON = "lon";

    @ClassRule
    public static EmbeddedSolrInfrastructureRule infrastructureRule = new EmbeddedSolrInfrastructureRule();

    @BeforeClass
    public static void setup() throws SolrServerException, IOException {
        for (int i = 0; i < TOTAL_DOC_COUNT; i++) {
            SolrInputDocument solrDocument = new SolrInputDocument();
            solrDocument.addField(SOLR_FIELD_ID, String.valueOf(i + 1));
            solrDocument.addField(SOLR_FIELD_LAT, String.valueOf(getRandomLat()));
            solrDocument.addField(SOLR_FIELD_LON, String.valueOf(getRandomLon()));
            infrastructureRule.getSolrClient().add(solrDocument);
        }

        infrastructureRule.getSolrClient().commit();
    }

    private static double getRandomLat() {
        return 46 + Math.random() * 3;
    }

    private static double getRandomLon() {
        return 9 + Math.random() * 8;
    }

    @Test
    public void clustering() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(Integer.MAX_VALUE);
        query.set(PARAM_SPATIALCLUSTERING, true);

        for (int i = 10; i < 100; i++) {
            query.set(PARAM_SPATIALCLUSTERING_SIZE, i);

            QueryResponse response = infrastructureRule.getSolrClient().query(query);

            assertEquals(TOTAL_DOC_COUNT, response.getResults().getNumFound());
            assertEquals(TOTAL_DOC_COUNT, response.getResults().size());
            assertEquals(Math.min(i, TOTAL_DOC_COUNT), ((NamedList<?>) response.getResponse().get("spatial-clustering")).size());
        }

        query.set(PARAM_SPATIALCLUSTERING_SIZE, TOTAL_DOC_COUNT);
        assertEquals(TOTAL_DOC_COUNT, this.getClustering(infrastructureRule.getSolrClient().query(query)).size());

        query.set(PARAM_SPATIALCLUSTERING_SIZE, 2 * TOTAL_DOC_COUNT);
        assertEquals(TOTAL_DOC_COUNT, this.getClustering(infrastructureRule.getSolrClient().query(query)).size());
    }

    private NamedList<?> getClustering(QueryResponse response) {
        return (NamedList<?>) response.getResponse().get("spatial-clustering");
    }
}
