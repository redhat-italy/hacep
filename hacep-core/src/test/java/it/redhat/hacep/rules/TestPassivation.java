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

@RunWith(MockitoJUnitRunner.class)
public class TestPassivation extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestPassivation.class);

    private final static ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static final String CACHE_NAME = "application";

    private ZonedDateTime now = ZonedDateTime.now();

    @Mock
    private Channel replayChannel;

    @Mock
    private Channel additionsChannel;

    @Mock
    private Channel locksChannel;

    private String passivationLocation;

    @Before
    public void createTemporaryLocationName() {
        passivationLocation = "./target/" + UUID.randomUUID().toString();
    }

    @Override
    public ConfigurationBuilder extendDefaultConfiguration(ConfigurationBuilder builder) {
        builder
                .persistence()
                .passivation(false)
                .addSingleFileStore()
                .shared(false)
                .preload(true)
                .fetchPersistentState(true)
                .purgeOnStartup(false)
                .location(passivationLocation)
//                .async().enabled(true)
//                .threadPoolSize(5)
                .singleton().enabled(false)
                .eviction()
                .strategy(EvictionStrategy.LRU).type(EvictionType.COUNT).size(1024);
        return builder;
    }

    @Test
    public void testPassivation() {
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

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
