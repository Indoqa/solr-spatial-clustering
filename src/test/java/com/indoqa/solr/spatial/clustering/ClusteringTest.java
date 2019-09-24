package com.indoqa.solr.spatial.clustering;

import static com.indoqa.solr.spatial.clustering.SpatialClusteringComponent.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

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

    private static final int ROWS = 0;

    @BeforeClass
    public static void setup() throws SolrServerException, IOException {
        infrastructureRule.getSolrClient().deleteByQuery("*:*");

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

    private static int getTotalSize(NamedList<NamedList<?>> clusters) {
        int result = 0;

        List<NamedList<?>> pins = clusters.getAll("pin");
        for (NamedList<?> eachPin : pins) {
            result += ((Number) eachPin.get("size")).intValue();
        }

        return result;
    }

    @Test
    public void belowMinResultCount() throws Exception {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(ROWS);
        query.set(PARAMETER_SPATIALCLUSTERING, true);
        query.set(PARAMETER_SIZE, TOTAL_DOC_COUNT);
        query.set(PARAMETER_MIN_RESULT_COUNT, TOTAL_DOC_COUNT + 1);

        QueryResponse response = infrastructureRule.getSolrClient().query(query);
        assertNull(response.getResponse().get("spatial-clustering"));
    }

    @Test
    public void belowMinSize() throws Exception {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(ROWS);
        query.set(PARAMETER_SPATIALCLUSTERING, true);
        query.set(PARAMETER_SIZE, 0);

        try {
            infrastructureRule.getSolrClient().query(query);
        } catch (SolrServerException e) {
            Throwable rootCause = e.getRootCause();

            assertTrue(rootCause instanceof IllegalArgumentException);
            assertEquals("The requested size must be at least 1.", rootCause.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void clustering() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(ROWS);
        query.set(PARAMETER_SPATIALCLUSTERING, true);

        for (int i = 10; i < 100; i++) {
            query.set(PARAMETER_SIZE, i);

            QueryResponse response = infrastructureRule.getSolrClient().query(query);

            assertEquals(TOTAL_DOC_COUNT, response.getResults().getNumFound());
            assertEquals(ROWS, response.getResults().size());

            NamedList<NamedList<?>> clusters = (NamedList<NamedList<?>>) response.getResponse().get("spatial-clustering");

            assertEquals(Math.min(i, TOTAL_DOC_COUNT), clusters.size());
            assertEquals(TOTAL_DOC_COUNT, getTotalSize(clusters));
        }

        query.set(PARAMETER_SIZE, TOTAL_DOC_COUNT);
        assertEquals(TOTAL_DOC_COUNT, this.getClustering(infrastructureRule.getSolrClient().query(query)).size());

        query.set(PARAMETER_SIZE, 2 * TOTAL_DOC_COUNT);
        assertEquals(TOTAL_DOC_COUNT, this.getClustering(infrastructureRule.getSolrClient().query(query)).size());
    }

    @Test
    public void exceedMaxSize() throws Exception {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(ROWS);
        query.set(PARAMETER_SPATIALCLUSTERING, true);
        query.set(PARAMETER_SIZE, Integer.MAX_VALUE);

        try {
            infrastructureRule.getSolrClient().query(query);
        } catch (SolrServerException e) {
            Throwable rootCause = e.getRootCause();

            assertTrue(rootCause instanceof IllegalArgumentException);
            assertEquals(
                "The requested size is larger than 10000. Consider changing maxSize in the plugin configuration.",
                rootCause.getMessage());
        }
    }

    private NamedList<?> getClustering(QueryResponse response) {
        return (NamedList<?>) response.getResponse().get("spatial-clustering");
    }
}
