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
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.kie.api.runtime.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractClusterTest {

    private List<EmbeddedCacheManager> nodes = null;

    protected abstract Channel getReplayChannel();

    protected abstract DroolsConfiguration getKieBaseConfiguration();

    protected EmbeddedCacheManager startDistSyncNode(int owners) {
        System.out.println("Start node with owners(" + owners + ")");
        DefaultCacheManager cacheManager = clusteredCacheManager(CacheMode.DIST_SYNC, owners);
        nodes.add(cacheManager);
        return cacheManager;
    }

    private DefaultCacheManager clusteredCacheManager(CacheMode mode, int owners) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer(getKieBaseConfiguration());

        GlobalConfiguration glob = new GlobalConfigurationBuilder().clusteredDefault()
                .transport().addProperty("configurationFile", System.getProperty("jgroups.configuration", "jgroups-test-tcp.xml"))
                .clusterName("HACEP")
                .globalJmxStatistics().allowDuplicateDomains(true).enable()
                .serialization()
                .addAdvancedExternalizer(new HAKieSession.HASessionExternalizer(getKieBaseConfiguration(), serializer, executorService))
                .addAdvancedExternalizer(new HAKieSerializedSession.HASerializedSessionExternalizer(getKieBaseConfiguration(), serializer, executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaEmpty.HASessionDeltaEmptyExternalizer(getKieBaseConfiguration(), serializer, executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaFact.HASessionDeltaFactExternalizer())
                .build();

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching().enable();
        configureCacheMode(configurationBuilder, mode, owners);

        org.infinispan.configuration.cache.Configuration loc = configurationBuilder.build();
        return new DefaultCacheManager(glob, loc, true);
    }

    private static void configureCacheMode(ConfigurationBuilder configurationBuilder, CacheMode mode, int owners) {
        configurationBuilder
                .clustering().cacheMode(mode)
                .hash().numOwners(owners).groups().enabled();
    }

    @Before
    public void start() {
        nodes = new ArrayList<>();
    }

    @After
    public void close() throws Exception {
        nodes.forEach((e) -> System.out.println("Stopping cache manager: [" + e.getAddress() + "]"));
        nodes.forEach(EmbeddedCacheManager::stop);
        nodes.clear();
    }
}
