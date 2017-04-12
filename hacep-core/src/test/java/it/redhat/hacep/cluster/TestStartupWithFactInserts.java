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
import org.junit.After;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestStartupWithFactInserts {

    private final Logger LOGGER = LoggerFactory.getLogger(TestStartupWithFactInserts.class);

    private ZonedDateTime now = ZonedDateTime.now();

    private Channel additionsChannel1;
    private Channel additionsChannel2;
    private Channel additionsChannel3;
    private Channel replayChannel1;
    private Channel replayChannel2;
    private Channel replayChannel3;
    private Router router1;
    private Router router2;
    private Router router3;
    private JmsConfiguration jmsConfig1;
    private JmsConfiguration jmsConfig2;
    private JmsConfiguration jmsConfig3;

    private HACEPImpl hacep1;
    private HACEPImpl hacep2;
    private HACEPImpl hacep3;

    @Before
    public void setup() throws InterruptedException {
        System.setProperty("jgroups.configuration", "jgroups-test-tcp.xml");

        additionsChannel1 = mock(Channel.class);
        additionsChannel2 = mock(Channel.class);
        additionsChannel3 = mock(Channel.class);
        replayChannel1 = mock(Channel.class);
        replayChannel2 = mock(Channel.class);
        replayChannel3 = mock(Channel.class);
        router1 = mock(Router.class);
        router2 = mock(Router.class);
        router3 = mock(Router.class);
        jmsConfig1 = mock(JmsConfiguration.class);
        jmsConfig2 = mock(JmsConfiguration.class);
        jmsConfig3 = mock(JmsConfiguration.class);

        RulesConfigurationTestImpl rulesConfigurationTest1 = RulesTestBuilder.buildV1();
        rulesConfigurationTest1.registerChannel("additions", additionsChannel1, replayChannel1);

        hacep1 = new HACEPImpl("node1");
        hacep1.setRouter(router1);
        hacep1.setJmsConfiguration(jmsConfig1);
        hacep1.setRulesConfiguration(rulesConfigurationTest1);

        RulesConfigurationTestImpl rulesConfigurationTest2 = RulesTestBuilder.buildV1();
        rulesConfigurationTest2.registerChannel("additions", additionsChannel2, replayChannel2);

        hacep2 = new HACEPImpl("node2");
        hacep2.setRouter(router2);
        hacep2.setJmsConfiguration(jmsConfig2);
        hacep2.setRulesConfiguration(rulesConfigurationTest2);

        RulesConfigurationTestImpl rulesConfigurationTest3 = RulesTestBuilder.buildV1();
        rulesConfigurationTest3.registerChannel("additions", additionsChannel3, replayChannel3);

        hacep3 = new HACEPImpl("node3");
        hacep3.setRouter(router3);
        hacep3.setJmsConfiguration(jmsConfig3);
        hacep3.setRulesConfiguration(rulesConfigurationTest3);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(hacep1::start);
        executorService.submit(hacep2::start);
        executorService.submit(hacep3::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @After
    public void cleanup() {
        hacep1.stop();
        hacep2.stop();
        hacep3.stop();
    }

    @Test
    public void testStopStart() throws InterruptedException {
        reset(router1, additionsChannel1, replayChannel1, router2, additionsChannel2, replayChannel2, router3, additionsChannel3, replayChannel3);

        DataGridManager dataGridManager1 = hacep1.getDataGridManager();
//        DataGridManager dataGridManager2 = hacep2.getDataGridManager();
//        DataGridManager dataGridManager3 = hacep3.getDataGridManager();

        Thread.sleep(5000);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit( () -> {
            for(int i = 0; i<=Integer.MAX_VALUE; i++){
                Cache<Key, Fact> factCache = dataGridManager1.getFactCache();
                Key key =new GameplayKey(Integer.toString(i), "" +(i % 20) );
                factCache.put(key, new TestFact(i % 20, 100L, new Date(now.toInstant().toEpochMilli()), key));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        } );

        Thread.sleep(5000);

        hacep3.stop();
        Thread.sleep(5000);
        hacep3.start();

        try {
            executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            executorService.shutdown();
        } catch ( InterruptedException e){
            //
        } finally {
            executorService.shutdownNow();
        }


//        ReleaseId rulesV2 = RulesTestBuilder.buildV2();
//        ReleaseId rulesV3 = RulesTestBuilder.buildV3();

//        Cache<Key, Fact> factCache = dataGridManager1.getFactCache();
//
//        KeyAffinityService<Key> affinityService = getKeyAffinityService(factCache);
//        Address address1 = dataGridManager1.getCacheManager().getAddress();
//        Key keyForDatagrid1 = affinityService.getKeyForAddress(address1);
//
//        Cache<String, Object> sessionCache1 = dataGridManager1.getSessionCache();
//        Cache<String, Object> sessionCache2 = dataGridManager2.getSessionCache();
//        Assert.assertEquals(0, sessionCache1.size());
//        Assert.assertEquals(0, sessionCache2.size());
//
//        hacep1.insertFact(generateFactTenSecondsAfter(1L, 10L, keyForDatagrid1));
//
//        verify(additionsChannel1, times(1)).send(eq(10L));
//        verify(replayChannel1, never()).send(any());
//
//        verify(additionsChannel2, never()).send(any());
//        verify(replayChannel2, never()).send(any());
//
//        Assert.assertEquals(1, sessionCache1.size());
//        Assert.assertEquals(1, sessionCache2.size());
//
//        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);
//
//        // Rules Update
//        hacep1.update(rulesV2.toExternalForm());
//
//        InOrder inOrder = Mockito.inOrder(router1);
//        inOrder.verify(router1, times(1)).suspend();
//        inOrder.verify(router1, times(1)).resume();
//        inOrder.verifyNoMoreInteractions();
//
//        inOrder = Mockito.inOrder(router2);
//        inOrder.verify(router2, times(1)).suspend();
//        inOrder.verify(router2, times(1)).resume();
//        inOrder.verifyNoMoreInteractions();
//
//        Assert.assertEquals(rulesV2.getVersion(), hacep1.getRulesManager().getReleaseId().getVersion());
//        Assert.assertEquals(rulesV2.getVersion(), hacep2.getRulesManager().getReleaseId().getVersion());
//        Assert.assertEquals(rulesV2.getVersion(), dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));
//
//        factCache.remove(keyForDatagrid1);
//        hacep1.insertFact(generateFactTenSecondsAfter(1L, 20L, keyForDatagrid1));
//
//        verify(additionsChannel1, times(1)).send(eq(60L));
//        verify(replayChannel1, never()).send(any());
//
//        verify(additionsChannel2, never()).send(any());
//        verify(replayChannel2, times(1)).send(eq(10L));
//
//        Assert.assertEquals(1, sessionCache1.size());
//        Assert.assertEquals(1, sessionCache2.size());
//
//        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);
//
//        // Rules Update
//        hacep2.update(rulesV3.toExternalForm());
//
//        inOrder = Mockito.inOrder(router1);
//        inOrder.verify(router1, times(1)).suspend();
//        inOrder.verify(router1, times(1)).resume();
//        inOrder.verifyNoMoreInteractions();
//
//        inOrder = Mockito.inOrder(router2);
//        inOrder.verify(router2, times(1)).suspend();
//        inOrder.verify(router2, times(1)).resume();
//        inOrder.verifyNoMoreInteractions();
//
//        Assert.assertEquals(rulesV3.getVersion(), hacep1.getRulesManager().getReleaseId().getVersion());
//        Assert.assertEquals(rulesV3.getVersion(), hacep2.getRulesManager().getReleaseId().getVersion());
//        Assert.assertEquals(rulesV3.getVersion(), dataGridManager1.getReplicatedCache().get(RulesManager.RULES_VERSION));
//
//        factCache.remove(keyForDatagrid1);
//        hacep1.insertFact(generateFactTenSecondsAfter(1L, 30L, keyForDatagrid1));
//
//        verify(additionsChannel1, times(1)).send(eq(180L));
//        verify(replayChannel1, never()).send(any());
//
//        verify(additionsChannel2, never()).send(any());
//        verify(replayChannel2, times(1)).send(eq(60L));
//
//        Assert.assertEquals(1, sessionCache1.size());
//        Assert.assertEquals(1, sessionCache2.size());
//
//        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);
//
//        //Add another fact on version 3 to check everything is working as before version upgrade
//        factCache.remove(keyForDatagrid1);
//        hacep1.insertFact(generateFactTenSecondsAfter(2L, 30L, keyForDatagrid1));
//
//        verify(additionsChannel1, times(1)).send(eq(90L));
//        verify(replayChannel1, never()).send(any());
//
//        verify(additionsChannel2, never()).send(any());
//        verify(replayChannel2, never()).send(any());
//
//        Assert.assertEquals(1, sessionCache1.size());
//        Assert.assertEquals(1, sessionCache2.size());
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount, Key key) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()), key);
    }

    private KeyAffinityService<Key> getKeyAffinityService(Cache<Key, Fact> factCache) {
        return KeyAffinityServiceFactory.newKeyAffinityService(factCache, Executors.newSingleThreadExecutor(), new KeyGenerator<Key>() {
            @Override
            public Key getKey() {
                long random = Math.round(Math.random() * 100000);
                return new GameplayKey("1", "" + random);
            }
        }, 10, true);
    }
}
