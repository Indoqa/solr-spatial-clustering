package com.indoqa.solr.spatial.clustering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTest {

    private static final int TOTAL_DOC_COUNT = 100_000;

    private static final String SOLR_FIELD_ID = "id";
    private static final String SOLR_FIELD_LAT = "lat";
    private static final String SOLR_FIELD_LON = "lon";

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);

    @ClassRule
    public static EmbeddedSolrInfrastructureRule infrastructureRule = new EmbeddedSolrInfrastructureRule();

    @BeforeClass
    public static void setup() throws SolrServerException, IOException {
        LOGGER.info("Preparing documents ...");

        infrastructureRule.getSolrClient().deleteByQuery("*:*");

        List<SolrInputDocument> documents = new ArrayList<>();

        for (int i = 0; i < TOTAL_DOC_COUNT; i++) {
            SolrInputDocument solrDocument = new SolrInputDocument();
            solrDocument.addField(SOLR_FIELD_ID, String.valueOf(i + 1));
            solrDocument.addField(SOLR_FIELD_LAT, String.valueOf(getRandomLat()));
            solrDocument.addField(SOLR_FIELD_LON, String.valueOf(getRandomLon()));

            documents.add(solrDocument);

            if (documents.size() == 1000) {
                infrastructureRule.getSolrClient().add(documents);
                documents.clear();
            }
        }

        LOGGER.info("Committing ...");
        infrastructureRule.getSolrClient().commit();
        LOGGER.info("Committed");
    }

    private static double getRandomLat() {
        return 46 + Math.random() * 3;
    }

    private static double getRandomLon() {
        return 9 + Math.random() * 8;
    }

    @Test
    public void performance() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(0);
        query.set("spatial-clustering", true);

        long totalTime = 0;
        for (int i = 5; i <= 100; i += 5) {
            query.set("spatial-clustering.size", i);

            QueryResponse response = infrastructureRule.getSolrClient().query(query);
            LOGGER.info("Results: {}, Clusters: {}, QTime: {}", response.getResults().getNumFound(), i, response.getQTime());

            totalTime += response.getQTime();
        }

        LOGGER.info("Total time: {} ms", totalTime);
    }
}
