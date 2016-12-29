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

import it.redhat.hacep.HACEPImpl;
import it.redhat.hacep.cache.listeners.UpdateVersionListener;
import it.redhat.hacep.configuration.DataGridManager;
import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.configuration.Router;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.rules.model.GameplayKey;
import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.Channel;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Mockito.*;

public class TestContainerUpdate {

    private final Logger LOGGER = LoggerFactory.getLogger(TestContainerUpdate.class);

    private ZonedDateTime now = ZonedDateTime.now();

    private Channel additionsChannel1 = mock(Channel.class);
    private Channel additionsChannel2 = mock(Channel.class);
    private Channel replayChannel1 = mock(Channel.class);
    private Channel replayChannel2 = mock(Channel.class);
    private Router router1 = mock(Router.class);
    private Router router2 = mock(Router.class);
    private JmsConfiguration jmsConfig1 = mock(JmsConfiguration.class);
    private JmsConfiguration jmsConfig2 = mock(JmsConfiguration.class);

    @Before
    public void setSetsystemProperties() {
        System.setProperty("jgroups.configuration", "jgroups-test-tcp.xml");
    }

    @Test
    public void testUpdate() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        RulesConfigurationTestImpl rulesConfigurationTest1 = RulesTestBuilder.buildV1();
        rulesConfigurationTest1.registerChannel("additions", additionsChannel1, replayChannel1);

        HACEPImpl hacep1 = new HACEPImpl();
        hacep1.setRouter(router1);
        hacep1.setJmsConfiguration(jmsConfig1);
        hacep1.setRulesConfiguration(rulesConfigurationTest1);

        RulesConfigurationTestImpl rulesConfigurationTest2 = RulesTestBuilder.buildV1();
        rulesConfigurationTest2.registerChannel("additions", additionsChannel2, replayChannel2);

        HACEPImpl hacep2 = new HACEPImpl();
        hacep2.setRouter(router2);
        hacep2.setJmsConfiguration(jmsConfig2);
        hacep2.setRulesConfiguration(rulesConfigurationTest2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(hacep1::start);
        executorService.submit(hacep2::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        try {
            DataGridManager dataGridManager2 = hacep2.getDataGridManager();
            DataGridManager dataGridManager1 = hacep1.getDataGridManager();

            ReleaseId rulesV2 = RulesTestBuilder.buildV2();
            ReleaseId rulesV3 = RulesTestBuilder.buildV3();

            Cache<Key, Fact> factCache = dataGridManager1.getFactCache();

            KeyAffinityService<Key> affinityService = KeyAffinityServiceFactory.newKeyAffinityService(factCache, Executors.newSingleThreadExecutor(), new KeyGenerator<Key>() {
                @Override
                public Key getKey() {
                    long random = Math.round(Math.random() * 100000);
                    return new GameplayKey("1", "" + random);
                }
            }, 10, true);
            Address address1 = dataGridManager1.getCacheManager().getAddress();
            Address address2 = dataGridManager2.getCacheManager().getAddress();
            Key keyForDatagrid1 = affinityService.getKeyForAddress(address1);

            Cache<String, Object> sessionCache1 = dataGridManager1.getSessionCache();
            Cache<String, Object> sessionCache2 = dataGridManager2.getSessionCache();
            Assert.assertEquals(0, sessionCache1.size());
            Assert.assertEquals(0, sessionCache2.size());

            hacep1.insertFact(generateFactTenSecondsAfter(1L, 10L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(10L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, never()).send(any());

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());

            reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

            // Rules Update
            hacep1.update(rulesV2.toExternalForm());

            InOrder inOrder = Mockito.inOrder(router1);
            inOrder.verify(router1, times(1)).suspend();
            inOrder.verify(router1, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            inOrder = Mockito.inOrder(router2);
            inOrder.verify(router2, times(1)).suspend();
            inOrder.verify(router2, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            Assert.assertEquals(rulesV2.getVersion(), hacep1.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals(rulesV2.getVersion(), hacep2.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals(rulesV2.getVersion(), dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));

            factCache.remove(keyForDatagrid1);
            hacep1.insertFact(generateFactTenSecondsAfter(1L, 20L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(60L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, times(1)).send(eq(10L));

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());

            reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

            // Rules Update
            hacep2.update(rulesV3.toExternalForm());

            inOrder = Mockito.inOrder(router1);
            inOrder.verify(router1, times(1)).suspend();
            inOrder.verify(router1, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            inOrder = Mockito.inOrder(router2);
            inOrder.verify(router2, times(1)).suspend();
            inOrder.verify(router2, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            Assert.assertEquals(rulesV3.getVersion(), hacep1.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals(rulesV3.getVersion(), hacep2.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals(rulesV3.getVersion(), dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));

            factCache.remove(keyForDatagrid1);
            hacep1.insertFact(generateFactTenSecondsAfter(1L, 30L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(180L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, times(1)).send(eq(60L));

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());

            reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

            //Add another fact on version 3 to check everything is working as before version upgrade
            factCache.remove(keyForDatagrid1);
            hacep1.insertFact(generateFactTenSecondsAfter(2L, 30L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(90L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, never()).send(any());

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());
        } finally {
            hacep1.stop();
            hacep2.stop();
        }
    }

    @Test
    public void testFailedUpdateOnAllNodes() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        RulesConfigurationTestImpl rulesConfigurationTest1 = RulesTestBuilder.buildV1();
        rulesConfigurationTest1.registerChannel("additions", additionsChannel1, replayChannel1);

        HACEPImpl hacep1 = new HACEPImpl();
        hacep1.setRouter(router1);
        hacep1.setJmsConfiguration(jmsConfig1);
        hacep1.setRulesConfiguration(rulesConfigurationTest1);

        RulesConfigurationTestImpl rulesConfigurationTest2 = RulesTestBuilder.buildV1();
        rulesConfigurationTest2.registerChannel("additions", additionsChannel2, replayChannel2);

        HACEPImpl hacep2 = new HACEPImpl();
        hacep2.setRouter(router2);
        hacep2.setJmsConfiguration(jmsConfig2);
        hacep2.setRulesConfiguration(rulesConfigurationTest2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(hacep1::start);
        executorService.submit(hacep2::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        try {
            DataGridManager dataGridManager1 = hacep1.getDataGridManager();
            DataGridManager dataGridManager2 = hacep2.getDataGridManager();

            for (Object listner : dataGridManager1.getReplicatedCache().getListeners()) {
                if (listner instanceof UpdateVersionListener)
                    dataGridManager1.getReplicatedCache().removeListener(listner);
            }
            dataGridManager1.getReplicatedCache().addListener(new UpdateVersionListnerError(router1, hacep1.getRulesManager()));

            for (Object listner : dataGridManager2.getReplicatedCache().getListeners()) {
                if (listner instanceof UpdateVersionListener)
                    dataGridManager2.getReplicatedCache().removeListener(listner);
            }
            dataGridManager2.getReplicatedCache().addListener(new UpdateVersionListnerError(router2, hacep2.getRulesManager()));

            ReleaseId rulesV2 = RulesTestBuilder.buildV2();

            Cache<Key, Fact> factCache = dataGridManager1.getFactCache();

            KeyAffinityService<Key> affinityService = KeyAffinityServiceFactory.newKeyAffinityService(factCache, Executors.newSingleThreadExecutor(), new KeyGenerator<Key>() {
                @Override
                public Key getKey() {
                    long random = Math.round(Math.random() * 100000);
                    return new GameplayKey("1", "" + random);
                }
            }, 10, true);
            Address address1 = dataGridManager1.getCacheManager().getAddress();
            Address address2 = dataGridManager2.getCacheManager().getAddress();
            Key keyForDatagrid1 = affinityService.getKeyForAddress(address1);

            Cache<String, Object> sessionCache1 = dataGridManager1.getSessionCache();
            Cache<String, Object> sessionCache2 = dataGridManager2.getSessionCache();
            Assert.assertEquals(0, sessionCache1.size());
            Assert.assertEquals(0, sessionCache2.size());

            hacep1.insertFact(generateFactTenSecondsAfter(1L, 10L, keyForDatagrid1));

            reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

            // Rules Update
            try {
                hacep1.update(rulesV2.toExternalForm());
            } catch (Exception e) {
                //let's pretend everything is ok
                LOGGER.info("TestFailedUpdate: let's pretend everything is ok");
            }

            InOrder inOrder = Mockito.inOrder(router1);
            inOrder.verify(router1, times(1)).suspend();
            inOrder.verify(router1, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            inOrder = Mockito.inOrder(router2);
            inOrder.verify(router2, times(1)).suspend();
            inOrder.verify(router2, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            Assert.assertEquals("1.0.0", hacep1.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals("1.0.0", hacep2.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals("1.0.0", dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));

            factCache.remove(keyForDatagrid1);
            hacep1.insertFact(generateFactTenSecondsAfter(1L, 20L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(30L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, never()).send(any());

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());
        } finally {
            hacep1.stop();
            hacep2.stop();
        }
    }

    @Test
    public void testFailedUpdateOnOneNode() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        RulesConfigurationTestImpl rulesConfigurationTest1 = RulesTestBuilder.buildV1();
        rulesConfigurationTest1.registerChannel("additions", additionsChannel1, replayChannel1);

        HACEPImpl hacep1 = new HACEPImpl();
        hacep1.setRouter(router1);
        hacep1.setJmsConfiguration(jmsConfig1);
        hacep1.setRulesConfiguration(rulesConfigurationTest1);

        RulesConfigurationTestImpl rulesConfigurationTest2 = RulesTestBuilder.buildV1();
        rulesConfigurationTest2.registerChannel("additions", additionsChannel2, replayChannel2);

        HACEPImpl hacep2 = new HACEPImpl();
        hacep2.setRouter(router2);
        hacep2.setJmsConfiguration(jmsConfig2);
        hacep2.setRulesConfiguration(rulesConfigurationTest2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(hacep1::start);
        executorService.submit(hacep2::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        try {
            DataGridManager dataGridManager1 = hacep1.getDataGridManager();
            DataGridManager dataGridManager2 = hacep2.getDataGridManager();

            for (Object listner : dataGridManager2.getReplicatedCache().getListeners()) {
                if (listner instanceof UpdateVersionListener)
                    dataGridManager2.getReplicatedCache().removeListener(listner);
            }
            dataGridManager2.getReplicatedCache().addListener(new UpdateVersionListnerError(router2, hacep2.getRulesManager()));

            ReleaseId rulesV2 = RulesTestBuilder.buildV2();

            Cache<Key, Fact> factCache = dataGridManager1.getFactCache();

            KeyAffinityService<Key> affinityService = KeyAffinityServiceFactory.newKeyAffinityService(factCache, Executors.newSingleThreadExecutor(), new KeyGenerator<Key>() {
                @Override
                public Key getKey() {
                    long random = Math.round(Math.random() * 100000);
                    return new GameplayKey("1", "" + random);
                }
            }, 10, true);
            Address address1 = dataGridManager1.getCacheManager().getAddress();
            Address address2 = dataGridManager2.getCacheManager().getAddress();
            Key keyForDatagrid1 = affinityService.getKeyForAddress(address1);

            Cache<String, Object> sessionCache1 = dataGridManager1.getSessionCache();
            Cache<String, Object> sessionCache2 = dataGridManager2.getSessionCache();
            Assert.assertEquals(0, sessionCache1.size());
            Assert.assertEquals(0, sessionCache2.size());

            hacep1.insertFact(generateFactTenSecondsAfter(1L, 10L, keyForDatagrid1));

            reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

            // Rules Update
            try {
                hacep1.update(rulesV2.toExternalForm());
            } catch (Exception e) {
                //let's pretend everything is ok
                LOGGER.info("TestFailedUpdate: let's pretend everything is ok");
            }

            InOrder inOrder = Mockito.inOrder(router1);
            inOrder.verify(router1, times(1)).suspend();
            inOrder.verify(router1, times(1)).resume();
            inOrder.verify(router1, times(1)).suspend();
            inOrder.verify(router1, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            inOrder = Mockito.inOrder(router2);
            inOrder.verify(router2, times(1)).suspend();
            inOrder.verify(router2, times(1)).resume();
            inOrder.verifyNoMoreInteractions();

            Assert.assertEquals("1.0.0", hacep1.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals("1.0.0", hacep2.getRulesManager().getReleaseId().getVersion());
            Assert.assertEquals("1.0.0", dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));

            factCache.remove(keyForDatagrid1);
            hacep1.insertFact(generateFactTenSecondsAfter(1L, 20L, keyForDatagrid1));

            verify(additionsChannel1, times(1)).send(eq(30L));
            verify(replayChannel1, never()).send(any());

            verify(additionsChannel2, never()).send(any());
            verify(replayChannel2, never()).send(any());

            Assert.assertEquals(1, sessionCache1.size());
            Assert.assertEquals(1, sessionCache2.size());
        } finally {
            hacep1.stop();
            hacep2.stop();
        }
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount, Key key) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()), key);
    }
}
