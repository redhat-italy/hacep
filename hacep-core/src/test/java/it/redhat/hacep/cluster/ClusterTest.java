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
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.distributed.Snapshotter;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTest extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(ClusterTest.class);

    private static TestDroolsConfiguration droolsConfiguration = TestDroolsConfiguration.buildV1();

    private static KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer(droolsConfiguration);

    private static ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Mock
    private Channel replayChannel;
    @Mock
    private Channel additionsChannel;

    private ZonedDateTime now = ZonedDateTime.now();

    @Test
    public void testClusterSize() {
        System.out.println("Start test cluster size");
        logger.info("Start test cluster size");
        Cache<Key, HAKieSession> cache1 = startNodes(2).getCache();
        Cache<Key, HAKieSession> cache2 = startNodes(2).getCache();

        assertEquals(2, ((DefaultCacheManager) cache1.getCacheManager()).getClusterSize());
        assertEquals(2, ((DefaultCacheManager) cache2.getCacheManager()).getClusterSize());
        System.out.println("End test cluster size");
        logger.info("End test cluster size");
    }

    @Test
    public void testEmptyHASession() {
        logger.info("Start test empty HASessionID");
        System.out.println("Start test empty HASessionID");
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, Object> cache1 = startNodes(2).getCache();
        Cache<String, Object> cache2 = startNodes(2).getCache();

        reset(replayChannel);

        String key = "1";
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

        cache1.put(key, session1);
        Object serializedSessionCopy = cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(HAKieSerializedSession.class.isAssignableFrom(serializedSessionCopy.getClass()));

        reset(replayChannel, additionsChannel);

        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();
        Assert.assertNotNull(session2);
        System.out.println("End test empty HASessionID");
        logger.info("End test empty HASessionID");
    }

    @Test
    public void testNonEmptyHASession() {
        logger.info("Start test non empty HASessionID");
        System.out.println("Start test non empty HASessionID");
        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, Object> cache1 = startNodes(2).getCache();
        Cache<String, Object> cache2 = startNodes(2).getCache();

        String key = "2";
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

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
        System.out.println("End test non empty HASessionID");
        logger.info("End test non empty HASessionID");
    }

    @Test
    public void testHASessionWithMaxBuffer() {
        logger.info("Start test HASessionID with max buffer 2");
        System.out.println("Start test HASessionID with max buffer 2");
        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(2);

        Cache<String, HAKieSession> cache1 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache2 = startNodes(2).getCache();

        reset(replayChannel, additionsChannel);

        String key = "3";
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

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
        System.out.println("End test HASessionID with max buffer 2");
        logger.info("End test HASessionID with max buffer 2");
    }

    @Test
    public void testDistributedSnapshots() {
        logger.info("Start test Distributed Snapshots");
        System.out.println("Start test Distributed Snapshots");

        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, HAKieSession> cache1 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache2 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache3 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache4 = startNodes(2).getCache();

        reset(replayChannel, additionsChannel);

        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));
        cache1.put("1", session1);

        ExecutorService des = new DefaultExecutorService(cache1);
        des.submit(new Snapshotter());
        des.shutdown();

        System.out.println("End test Distributed Snapshots");
        logger.info("End test Distributed Snapshots");

    }



    @Test
    public void testHASessionAddNode() {
        logger.info("Start test HASessionID add node");
        System.out.println("Start test HASessionID add node");
        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, HAKieSession> cache1 = startNodes(2).getCache();

        reset(replayChannel, additionsChannel);

        String key = "3";
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

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

        Cache<Key, HAKieSession> cache2 = startNodes(2).getCache();
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

        System.out.println("End test HASessionID add node");
        logger.info("End test HASessionID add node");
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    @Override
    protected DroolsConfiguration getKieBaseConfiguration() {
        return droolsConfiguration;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }

}
