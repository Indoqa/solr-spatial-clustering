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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.rules.ExternalResource;

import com.indoqa.solr.spring.client.EmbeddedSolrServerBuilder;

public class EmbeddedSolrInfrastructureRule extends ExternalResource {

    private SolrClient solrClient;
    private boolean initialized;

    public SolrClient getSolrClient() {
        return this.solrClient;
    }

    @Override
    protected void after() {
        try {
            this.solrClient.close();
        } catch (Exception e) {
            // ignore
        }

        if (this.solrClient instanceof EmbeddedSolrServer) {
            EmbeddedSolrServer embeddedSolrServer = (EmbeddedSolrServer) this.solrClient;
            embeddedSolrServer.getCoreContainer().shutdown();
        }
    }

    @Override
    protected void before() throws Throwable {
        if (this.initialized) {
            return;
        }

        this.solrClient = EmbeddedSolrServerBuilder.build("file://./target/test-core", "solr/test");

        this.initialized = true;
    }
}
