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
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTest extends AbstractClusterTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterTest.class);


    private static ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Mock
    private Channel replayChannel;
    @Mock
    private Channel additionsChannel;

    private ZonedDateTime now = ZonedDateTime.now();

    @Test
    public void testClusterSize() {
        System.setProperty("grid.buffer", "10");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        LOGGER.info("Start test cluster size");
        Cache<Key, HAKieSession> cache1 = startNodes(2, rulesManager).getCache();
        Cache<Key, HAKieSession> cache2 = startNodes(2, rulesManager).getCache();

        assertEquals(2, ((DefaultCacheManager) cache1.getCacheManager()).getClusterSize());
        assertEquals(2, ((DefaultCacheManager) cache2.getCacheManager()).getClusterSize());
        LOGGER.info("End test cluster size");
        rulesManager.stop();
    }

    @Test
    public void testEmptyHASession() {
        LOGGER.info("Start test empty HASessionID");
        System.setProperty("grid.buffer", "10");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, Object> cache1 = startNodes(2, rulesManager).getCache();
        Cache<String, Object> cache2 = startNodes(2, rulesManager).getCache();

        reset(replayChannel);

        String key = "1";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        cache1.put(key, session1);
        HAKieSession serializedSessionCopy = (HAKieSession) cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(serializedSessionCopy.isSerialized());

        reset(replayChannel, additionsChannel);

        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();
        Assert.assertNotNull(session2);
        LOGGER.info("End test empty HASessionID");
        rulesManager.stop();
    }

    @Test
    public void testNonEmptyHASession() {
        System.setProperty("grid.buffer", "10");

        LOGGER.info("Start test non empty HASessionID");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, Object> cache1 = startNodes(2, rulesManager).getCache();
        Cache<String, Object> cache2 = startNodes(2, rulesManager).getCache();

        String key = "2";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));
        cache1.put(key, session1);

        verify(replayChannel, never()).send(any());

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verify(additionsChannel, times(1)).send(eq(60L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(3)).send(any());

        Object serializedSessionCopy = cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(HAKieSerializedSession.class.isAssignableFrom(serializedSessionCopy.getClass()));

        reset(replayChannel, additionsChannel);

        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();

        session2.insert(generateFactTenSecondsAfter(1L, 40L));

        inOrder = inOrder(replayChannel);
        inOrder.verify(replayChannel, times(1)).send(eq(60L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(replayChannel, times(1)).send(any());

        verify(additionsChannel, atMost(1)).send(any());
        verify(additionsChannel, times(1)).send(eq(100L));
        LOGGER.info("End test non empty HASessionID");
        rulesManager.stop();
    }

    @Test
    public void testHASessionWithMaxBuffer() {
        System.setProperty("grid.buffer", "2");

        LOGGER.info("Start test HASessionID with max buffer 2");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, HAKieSession> cache1 = startNodes(2, rulesManager).getCache();
        Cache<String, HAKieSession> cache2 = startNodes(2, rulesManager).getCache();

        reset(replayChannel, additionsChannel);

        String key = "3";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));
        cache1.put(key, session1);

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verify(additionsChannel, times(1)).send(eq(60L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(3)).send(any());

        Object serializedSessionCopy = cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(HAKieSerializedSession.class.isAssignableFrom(serializedSessionCopy.getClass()));

        reset(replayChannel, additionsChannel);

        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();

        session2.insert(generateFactTenSecondsAfter(1L, 40L));

        verify(additionsChannel, times(1)).send(eq(100L));
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(1)).send(any());
        LOGGER.info("End test HASessionID with max buffer 2");
        rulesManager.stop();
    }

    @Test
    public void testHASessionAddNode() {
        System.setProperty("grid.buffer", "10");

        LOGGER.info("Start test HASessionID add node");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, HAKieSession> cache1 = startNodes(2, rulesManager).getCache();

        reset(replayChannel, additionsChannel);

        String key = "3";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));
        cache1.put(key, session1);

        verify(replayChannel, never()).send(any());

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verify(additionsChannel, times(1)).send(eq(60L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(3)).send(any());

        Cache<Key, HAKieSession> cache2 = startNodes(2, rulesManager).getCache();
        Object serializedSessionCopy = cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(HAKieSession.class.isAssignableFrom(serializedSessionCopy.getClass()));

        reset(replayChannel, additionsChannel);

        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();

        session2.insert(generateFactTenSecondsAfter(1L, 40L));

        verify(replayChannel, never()).send(any());

        verify(additionsChannel, times(1)).send(eq(100L));
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(1)).send(any());

        LOGGER.info("End test HASessionID add node");
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
