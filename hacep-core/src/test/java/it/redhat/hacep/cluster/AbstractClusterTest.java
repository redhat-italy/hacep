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


import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cache.session.HAKieSessionDeltaEmpty;
import it.redhat.hacep.cache.session.HAKieSessionDeltaFact;
import it.redhat.hacep.configuration.AbstractBaseDroolsConfiguration;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractClusterTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClusterTest.class);

    private List<EmbeddedCacheManager> nodes = null;

    protected abstract Channel getReplayChannel();

    protected abstract AbstractBaseDroolsConfiguration getKieBaseConfiguration();

    protected EmbeddedCacheManager startNodes(int owners) {
        LOGGER.info("Start node with owners({})", owners);
        DefaultCacheManager cacheManager = clusteredCacheManager(CacheMode.DIST_SYNC, owners);
        nodes.add(cacheManager);
        return cacheManager;
    }

    protected void stopNodes() {
        nodes.forEach((e) -> LOGGER.info("Stopping cache manager: [{}]", e.getAddress()));
        nodes.forEach(EmbeddedCacheManager::stop);
        nodes.clear();
    }

    private DefaultCacheManager clusteredCacheManager(CacheMode mode, int owners) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        GlobalConfiguration glob = new GlobalConfigurationBuilder().clusteredDefault()
                .transport().addProperty("configurationFile", System.getProperty("jgroups.configuration", "jgroups-test-tcp.xml"))
                .clusterName("HACEP")
                .globalJmxStatistics().allowDuplicateDomains(true).enable()
                .serialization()
                .addAdvancedExternalizer(new HAKieSession.HASessionExternalizer(getKieBaseConfiguration(), executorService))
                .addAdvancedExternalizer(new HAKieSerializedSession.HASerializedSessionExternalizer(getKieBaseConfiguration(), executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaEmpty.HASessionDeltaEmptyExternalizer(getKieBaseConfiguration(), executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaFact.HASessionDeltaFactExternalizer())
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

    @Before
    public void init() {
        nodes = new ArrayList<>();
    }

    @After
    public void close() throws Exception {
        stopNodes();
    }
}
