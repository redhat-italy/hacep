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

package it.redhat.hacep.rules;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cluster.AbstractClusterTest;
import it.redhat.hacep.cluster.RulesConfigurationTestImpl;
import it.redhat.hacep.cluster.TestFact;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TestPersistence extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestPersistence.class);

    private final static ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static final String CACHE_NAME = "application";

    private ZonedDateTime now = ZonedDateTime.now();

    @Mock
    private Channel replayChannel;

    @Mock
    private Channel additionsChannel;

    @Mock
    private Channel locksChannel;

    private int nodeSelector = 1;
    private String persistenceLocation;
    private String persistenceLocation2;
    private String persistenceLocation3;

    @Before
    public void createTemporaryLocationName() {
        nodeSelector = 1;
        persistenceLocation = "./target/" + UUID.randomUUID().toString();
        persistenceLocation2 = "./target/" + UUID.randomUUID().toString();
        persistenceLocation3 = "./target/" + UUID.randomUUID().toString();
    }

    @Override
    public ConfigurationBuilder extendDefaultConfiguration(ConfigurationBuilder builder) {

        SingleFileStoreConfigurationBuilder sfcb = builder
                .persistence()
                .passivation(false)
                .addSingleFileStore()
                .shared(false)
                .preload(true)
                .fetchPersistentState(true)
                .purgeOnStartup(false);

        if( nodeSelector == 1 ){
            sfcb.location(persistenceLocation);
        } else if( nodeSelector == 2) {
            sfcb.location(persistenceLocation2);
        } else if( nodeSelector == 3) {
            sfcb.location(persistenceLocation3);
        } else {
            throw new UnsupportedOperationException("Node id unknown");
        }

        sfcb.singleton().enabled(false)
                .eviction()
                .strategy(EvictionStrategy.LRU).type(EvictionType.COUNT).size(1024);

        return builder;
    }

    @Test
    public void testPersistence() {
        System.setProperty("grid.buffer", "10");

        reset(additionsChannel, replayChannel, locksChannel);

        logger.info("Start test serialized rules");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesConfigurationTestImpl.RulesTestBuilder.buildRulesWithRetract();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);
        rulesConfigurationTest.registerChannel("locks", locksChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, Object> cache = startNodes(1, rulesManager).getCache(CACHE_NAME);

        String key = "1";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        session1.insert(generateFactTenSecondsAfter(1, 1L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 2L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 3L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 4L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 5L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 6L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 7L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 8L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 9L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 10L));
        // LOCK inserted expires in 25 sec.
        cache.put(key, session1);


        session1.insert(generateFactTenSecondsAfter(1, 1L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 2L));
        cache.put(key, session1);

        // 30 sec after - lock should be expired
        session1.insert(generateFactTenSecondsAfter(1, 3L));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1, 10L));
        // LOCK inserted expires in 25 sec.
        cache.put(key, session1);

        stopNodes();

        Cache<String, Object> cacheDeserialized = startNodes(1, rulesManager).getCache(CACHE_NAME);

        Object o = cacheDeserialized.get(key);
        Assert.assertTrue(o instanceof HAKieSerializedSession);
        HAKieSerializedSession haKieSerializedSession = (HAKieSerializedSession) o;
        HAKieSession sessionRebuilt = haKieSerializedSession.rebuild();

        sessionRebuilt.insert(generateFactTenSecondsAfter(1, 0L));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(1, 0L));
        cacheDeserialized.put(key, sessionRebuilt);

        // 30 sec after - lock should be expired
        // And inserted again by this fact (expires in 25 sec)
        sessionRebuilt.insert(generateFactTenSecondsAfter(1, 10L));
        cacheDeserialized.put(key, sessionRebuilt);

        InOrder order = Mockito.inOrder(additionsChannel, locksChannel);
        order.verify(additionsChannel).send(eq(1L));
        order.verify(additionsChannel).send(eq(2L));
        order.verify(additionsChannel).send(eq(3L));
        order.verify(additionsChannel).send(eq(4L));
        order.verify(additionsChannel).send(eq(5L));
        order.verify(additionsChannel).send(eq(6L));
        order.verify(additionsChannel).send(eq(7L));
        order.verify(additionsChannel).send(eq(8L));
        order.verify(additionsChannel).send(eq(9L));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verify(locksChannel).send(eq("REMOVED"));
        order.verify(additionsChannel).send(eq(3L));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verify(locksChannel).send(eq("REMOVED"));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verifyNoMoreInteractions();

        logger.info("End test serialized rules");
        rulesManager.stop();
    }

    @Test
    public void testMultiNodePersistence() throws InterruptedException {
        System.setProperty("grid.buffer", "10");

        reset(additionsChannel, replayChannel, locksChannel);

        logger.info("Start test Multi Node Persistence");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesConfigurationTestImpl.RulesTestBuilder.buildRulesWithRetract();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);
        rulesConfigurationTest.registerChannel("locks", locksChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        nodeSelector = 1;
        Cache<String, Object> cache = startNodes(2, rulesManager, "A", persistenceLocation).getCache(CACHE_NAME);
        nodeSelector = 2;
        Cache<String, Object> cache2 = startNodes(2, rulesManager, "B", persistenceLocation2).getCache(CACHE_NAME);
        nodeSelector = 3;
        Cache<String, Object> cache3 = startNodes(2, rulesManager, "C", persistenceLocation3).getCache(CACHE_NAME);

        String key3 = "3";
        HAKieSession session3 = new HAKieSession(rulesManager, executorService);
        // LOCK inserted expires in 25 sec.
        session3.insert(generateFactTenSecondsAfter(3, 10L));
        cache3.put(key3, session3);

        String key2 = "2";
        HAKieSession session2 = new HAKieSession(rulesManager, executorService);
        // LOCK inserted expires in 25 sec.
        session2.insert(generateFactTenSecondsAfter(2, 10L));
        cache2.put(key2, session2);

        String key = "1";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);
        // LOCK inserted expires in 25 sec.
        session1.insert(generateFactTenSecondsAfter(1, 10L));
        cache.put(key, session1);

        InOrder order2 = Mockito.inOrder(additionsChannel, locksChannel);
        order2.verify(locksChannel, times(3)).send(eq("INSERTED"));
        order2.verifyNoMoreInteractions();

        stopNodes();

        nodeSelector = 1;
        Cache<String, Object> cacheDeserialized = startNodes(2, rulesManager, "A", persistenceLocation).getCache(CACHE_NAME);
        nodeSelector = 2;
        Cache<String, Object> cacheDeserialized2 = startNodes(2, rulesManager, "B", persistenceLocation2).getCache(CACHE_NAME);
        nodeSelector = 3;
        Cache<String, Object> cacheDeserialized3 = startNodes(2, rulesManager, "C", persistenceLocation3).getCache(CACHE_NAME);


        checkKey(key, cacheDeserialized, cacheDeserialized2, cacheDeserialized3);
        checkKey(key2, cacheDeserialized, cacheDeserialized2, cacheDeserialized3);
        checkKey(key3, cacheDeserialized, cacheDeserialized2, cacheDeserialized3);


        Mockito.verify( locksChannel, times(6) ).send(eq("INSERTED"));
        Mockito.verify( locksChannel, times(3) ).send(eq("REMOVED"));
        Mockito.verifyNoMoreInteractions(locksChannel);

        logger.info("Multi Node Persistence");
        rulesManager.stop();
    }

    private void checkKey(String key, Cache<String, Object> cacheDeserialized, Cache<String, Object> cacheDeserialized2, Cache<String, Object> cacheDeserialized3) {

        logger.info("Checking key: "+key);

        long keyLong = Long.parseLong(key);

        Object o = cacheDeserialized.get(key);
        //XXX: needed waiting for https://issues.jboss.org/browse/ISPN-9200
        if( o == null){
            o = cacheDeserialized2.get(key);
        }
        if( o == null){
            o = cacheDeserialized3.get(key);
        }
        ///////////////////////////////////////////////////////////////////

        Assert.assertTrue(o instanceof HAKieSerializedSession);
        HAKieSerializedSession haKieSerializedSession = (HAKieSerializedSession) o;
        HAKieSession sessionRebuilt = haKieSerializedSession.rebuild();

        sessionRebuilt.insert(generateFactTenSecondsAfter(keyLong, 0L));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(keyLong, 0L));
        cacheDeserialized.put(key, sessionRebuilt);

        // 30 sec after - lock should be expired
        sessionRebuilt.insert(generateFactTenSecondsAfter(keyLong, 0L));
        cacheDeserialized.put(key, sessionRebuilt);

        // And inserted again by this fact (expires in 25 sec)
        sessionRebuilt.insert(generateFactTenSecondsAfter(keyLong, 10L));
        cacheDeserialized.put(key, sessionRebuilt);
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()), null);
    }
}
