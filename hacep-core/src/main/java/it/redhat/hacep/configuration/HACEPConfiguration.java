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

package it.redhat.hacep.configuration;

import it.redhat.hacep.cache.session.SessionSaver;
import it.redhat.hacep.cache.externalizers.KieSessionExternalizer;
import it.redhat.hacep.cache.listeners.FactListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPre;
import it.redhat.hacep.cache.session.*;
import it.redhat.hacep.camel.Putter;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.drools.channels.NullChannel;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.camel.KeyBuilder;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HACEPConfiguration {

    public static final String CAMEL_ROUTE = "facts";
    private final static Logger log = LoggerFactory.getLogger(HACEPConfiguration.class);
    private final DefaultCacheManager manager;
    private final Cache<Key, Fact> factCache;
    private final Cache<Key, Object> sessionCache;
    private final CamelContext camelContext;
    private final KeyBuilder keyBuilder;

    public HACEPConfiguration(DroolsConfiguration droolsConfiguration, KeyBuilder keyBuilder) {
        this.keyBuilder = keyBuilder;
        NullChannel replayChannel = new NullChannel();

        this.manager = clusteredCacheManager(replayChannel, droolsConfiguration);
        this.factCache = this.manager.getCache("fact", true);
        this.sessionCache = this.manager.getCache("session", true);

        this.camelContext = createCamelContext();

        SessionSaver sessionSaver = new SessionSaver(this.sessionCache, droolsConfiguration);
        this.factCache.addListener(new FactListenerPost(sessionSaver));

        this.sessionCache.addListener(new SessionListenerPre(this.camelContext));
        this.sessionCache.addListener(new SessionListenerPost(this.camelContext));
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }


    private DefaultCacheManager clusteredCacheManager(NullChannel replayChannel, DroolsConfiguration droolsConfiguration) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer(droolsConfiguration, getSessionCompression());

        GlobalConfiguration glob = new GlobalConfigurationBuilder().clusteredDefault()
                .transport().addProperty("configurationFile", System.getProperty("jgroups.configuration", "jgroups-tcp.xml"))
                .clusterName("HACEP")
                .globalJmxStatistics().allowDuplicateDomains(true).enable()
                .serialization()
                .addAdvancedExternalizer(new KieSessionExternalizer(serializer))
                .addAdvancedExternalizer(new HASession.HASessionExternalizer(droolsConfiguration))
                .addAdvancedExternalizer(new HASerializedSession.HASerializedSessionExternalizer(droolsConfiguration, serializer, executorService))
                .addAdvancedExternalizer(new HASessionDeltaEmpty.HASessionDeltaEmptyExternalizer(droolsConfiguration, serializer, executorService))
                .addAdvancedExternalizer(new HASessionDeltaFact.HASessionDeltaFactExternalizer())
                .build();

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        CacheMode cacheMode = getCacheMode();
        if (cacheMode.isDistributed()) {
            configurationBuilder
                    .clustering().cacheMode(cacheMode)
                    .hash().numOwners(getNumOwners()).groups().enabled();
        } else {
            configurationBuilder
                    .clustering().cacheMode(cacheMode);
        }
        Configuration loc = configurationBuilder.build();
        return new DefaultCacheManager(glob, loc, true);
    }

    private CamelContext createCamelContext() {
        CamelContext context = new DefaultCamelContext();
        try {

            context.addComponent("activemq", configureActiveMqCmp());
            context.addRoutes(new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    String uri = "activemq:" + getQueueName()
                            + "?concurrentConsumers=" + getQueueConsumers()
                            + "&includeAllJMSXProperties=true"
                            + "&destination.consumer.prefetchSize=" + getQueuePrefetchSize()
                            + "&destination.consumer.dispatchAsync=false"
                            + "&maxConcurrentConsumers=" + getQueueConsumers();

                    from(uri)
                            .routeId(CAMEL_ROUTE)
                            .to("direct:putInGrid");
                }
            });

            context.addRoutes(new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from("direct:putInGrid").bean(new Putter(keyBuilder, factCache), "put(${body})");
                }
            });

            context.start();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return context;
    }

    private CacheMode getCacheMode() {
        try {
            return CacheMode.valueOf(System.getProperty("grid.mode", "DIST_SYNC"));
        } catch (IllegalArgumentException e) {
            return CacheMode.DIST_SYNC;
        }
    }

    private int getNumOwners() {
        try {
            return Integer.valueOf(System.getProperty("grid.owners", "2"));
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

    private int getCompanionBufferSize() {
        try {
            return Integer.valueOf(System.getProperty("grid.buffer", "10"));
        } catch (IllegalArgumentException e) {
            return 10;
        }
    }

    private String getQueueBrokerUrl() {
        try {
            return System.getProperty("queue.url", "tcp://localhost:61616");
        } catch (IllegalArgumentException e) {
            return "tcp://localhost:61616";
        }
    }

    private String getQueueName() {
        try {
            return System.getProperty("queue.name", "facts");
        } catch (IllegalArgumentException e) {
            return "facts";
        }
    }

    private boolean getQueueSecurity() {
        try {
            return Boolean.valueOf(System.getProperty("queue.security", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getQueueUsername() {
        try {
            return System.getProperty("queue.usr", "admin");
        } catch (IllegalArgumentException e) {
            return "admin";
        }
    }

    private String getQueuepassword() {
        try {
            return System.getProperty("queue.pwd", "admin");
        } catch (IllegalArgumentException e) {
            return "admin";
        }
    }

    private int getQueuePrefetchSize() {
        try {
            return Integer.valueOf(System.getProperty("queue.prefetch", "500"));
        } catch (IllegalArgumentException e) {
            return 500;
        }
    }

    private int getQueueConsumers() {
        try {
            return Integer.valueOf(System.getProperty("queue.consumers", "5"));
        } catch (IllegalArgumentException e) {
            return 5;
        }
    }

    private boolean getSessionCompression() {
        try {
            return Boolean.valueOf(System.getProperty("session.compression", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public int getResequencerSize() {
        try {
            return Integer.valueOf(System.getProperty("resequencer.size", "1000"));
        } catch (IllegalArgumentException e) {
            return 1000;
        }
    }

    public int getResequencerTimeout() {
        try {
            return Integer.valueOf(System.getProperty("resequencer.timeout", "1000"));
        } catch (IllegalArgumentException e) {
            return 1000;
        }
    }

    /**
     * Create ActiveMq Camel Component with {@link #getQueueBrokerUrl()} and trust alla packages
     *
     * @return instance of {@link ActiveMQComponent}
     */
    private ActiveMQComponent configureActiveMqCmp() {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setUsePooledConnection(false);
        activeMQComponent.setBrokerURL(getQueueBrokerUrl());
        if (getQueueSecurity()) {
            activeMQComponent.setUserName(getQueueUsername());
            activeMQComponent.setPassword(getQueuepassword());
        }
        return activeMQComponent;
    }

    public Cache<Key, Fact> getFactCache() {
        return factCache;
    }

    public DefaultCacheManager getManager() {
        return manager;
    }
}
