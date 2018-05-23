/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.redhat.hacep.cluster;


import it.redhat.hacep.cache.session.*;
import it.redhat.hacep.configuration.RulesManager;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalJmxStatisticsConfigurationBuilder;
import org.infinispan.configuration.global.GlobalStateConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClusterTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClusterTest.class);

    private List<EmbeddedCacheManager> nodes = new ArrayList<>();

    protected abstract Channel getReplayChannel();

    protected EmbeddedCacheManager startNodes(int owners, RulesManager droolsConfiguration) {
        return startNodes(owners, droolsConfiguration, null, null);
    }

    protected EmbeddedCacheManager startNodes(int owners, RulesManager droolsConfiguration, String nodeName, String globalStateLocation) {
        LOGGER.info("Start node with owners({})", owners);
        DefaultCacheManager cacheManager = clusteredCacheManager(CacheMode.DIST_SYNC, owners, droolsConfiguration, nodeName, globalStateLocation);
        nodes.add(cacheManager);
        return cacheManager;
    }

    protected void stopNodes() {
        nodes.forEach((e) -> LOGGER.info("Stopping cache manager: [{}]", e.getAddress()));
        nodes.forEach(EmbeddedCacheManager::stop);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            LOGGER.info("Await for {} nodes shutdown", nodes.size());
            while (nodes.size() > 0) {
                if (nodes.get(0).getStatus().isTerminated()) {
                    nodes.remove(0);
                }
                Thread.sleep(100);
                LOGGER.info("Still waiting for {} nodes shutdown", nodes.size());
            }
            LOGGER.info("Nodes shutdown complete", nodes.size());
            return true;
        });
        try {
            LOGGER.info("Waiting for termination");
            service.shutdown();
            LOGGER.info("Terminated");
            service.awaitTermination(1, TimeUnit.MINUTES);
            LOGGER.info("Shutdown");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private DefaultCacheManager clusteredCacheManager(CacheMode mode, int owners, RulesManager rulesManager, String nodeName, String globalStateLocation) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        HAKieSessionBuilder sessionBuilder = new HAKieSessionBuilder(rulesManager, executorService);

         TransportConfigurationBuilder tcb = new GlobalConfigurationBuilder().clusteredDefault()
                .transport().addProperty("configurationFile", System.getProperty("jgroups.configuration", "jgroups-test-tcp.xml"))
                .clusterName("HACEP");

                if(nodeName != null){
                    tcb.nodeName(nodeName);
                }

        GlobalJmxStatisticsConfigurationBuilder gjscb = tcb.globalJmxStatistics().allowDuplicateDomains(true).enable();

        GlobalStateConfigurationBuilder gscb;
                if(globalStateLocation!=null){
                    gscb = gjscb.globalState().enable().persistentLocation(globalStateLocation);
                } else {
                    gscb = gjscb.globalState().disable();
                }

        GlobalConfiguration glob = gscb.serialization()
                .addAdvancedExternalizer(new HAKieSession.HASessionExternalizer(sessionBuilder))
                .addAdvancedExternalizer(new HAKieSerializedSession.HASerializedSessionExternalizer(sessionBuilder))
                .addAdvancedExternalizer(new HAKieSessionDeltaEmpty.HASessionDeltaEmptyExternalizer(sessionBuilder))
                .addAdvancedExternalizer(new HAKieSessionDeltaFact.HASessionDeltaFactExternalizer(sessionBuilder))
                .build();

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching().enable();
        configureCacheMode(configurationBuilder, mode, owners);

        org.infinispan.configuration.cache.Configuration loc = extendDefaultConfiguration(configurationBuilder).build();
        return new DefaultCacheManager(glob, loc, true);
    }

    public ConfigurationBuilder extendDefaultConfiguration(ConfigurationBuilder builder) {
        return builder;
    }

    private static void configureCacheMode(ConfigurationBuilder configurationBuilder, CacheMode mode, int owners) {
        configurationBuilder
                .clustering().cacheMode(mode)
                .hash().numOwners(owners).groups().enabled();
    }

    @After
    public void close() throws Exception {
        stopNodes();
    }
}
