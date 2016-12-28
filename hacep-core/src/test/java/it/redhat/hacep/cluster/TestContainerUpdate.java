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

import it.redhat.hacep.cache.listeners.FactListenerPost;
import it.redhat.hacep.cache.listeners.UpdateVersionListener;
import it.redhat.hacep.cache.session.HAKieSessionBuilder;
import it.redhat.hacep.cache.session.KieSessionSaver;
import it.redhat.hacep.configuration.DataGridManager;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.Channel;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Mockito.*;

public class TestContainerUpdate {

    private ZonedDateTime now = ZonedDateTime.now();

    private Channel additionsChannel1 = mock(Channel.class);
    private Channel additionsChannel2 = mock(Channel.class);
    private Channel replayChannel1 = mock(Channel.class);
    private Channel replayChannel2 = mock(Channel.class);
    private Router router1 = mock(Router.class);
    private Router router2 = mock(Router.class);

    @Test
    public void testUpdate() {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        RulesConfigurationTestImpl rulesConfigurationTest1 = RulesTestBuilder.buildV1();
        rulesConfigurationTest1.registerChannel("additions", additionsChannel1, replayChannel1);
        RulesManager rulesManager1 = new RulesManager(rulesConfigurationTest1);
        rulesManager1.start(rulesConfigurationTest1.getGroupId(), rulesConfigurationTest1.getArtifactId(), rulesConfigurationTest1.getVersion());
        DataGridManager dataGridManager1 = buildDataGridManager(rulesManager1, router1);

        RulesConfigurationTestImpl rulesConfigurationTest2 = RulesTestBuilder.buildV1();
        rulesConfigurationTest2.registerChannel("additions", additionsChannel2, replayChannel2);
        RulesManager rulesManager2 = new RulesManager(rulesConfigurationTest2);
        rulesManager2.start(rulesConfigurationTest2.getGroupId(), rulesConfigurationTest2.getArtifactId(), rulesConfigurationTest2.getVersion());
        DataGridManager dataGridManager2 = buildDataGridManager(rulesManager2, router2);

        String rulesV2 = RulesTestBuilder.buildV2();
        String rulesV3 = RulesTestBuilder.buildV3();

        Assert.assertTrue(dataGridManager1.waitForMinimumOwners(1, TimeUnit.MINUTES));
        Assert.assertTrue(dataGridManager2.waitForMinimumOwners(1, TimeUnit.MINUTES));

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

        factCache.put(keyForDatagrid1, generateFactTenSecondsAfter(1L, 10L));

        verify(additionsChannel1, times(1)).send(eq(10L));
        verify(replayChannel1, never()).send(any());

        verify(additionsChannel2, never()).send(any());
        verify(replayChannel2, never()).send(any());

        Assert.assertEquals(1, sessionCache1.size());
        Assert.assertEquals(1, sessionCache2.size());

        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        // Rules Update
        dataGridManager1.getReplicatedCache().put(RulesManager.RULES_VERSION, rulesV2);

        InOrder inOrder = Mockito.inOrder(router1);
        inOrder.verify(router1, times(1)).suspend();
        inOrder.verify(router1, times(1)).resume();
        inOrder.verifyNoMoreInteractions();

        inOrder = Mockito.inOrder(router2);
        inOrder.verify(router2, times(1)).suspend();
        inOrder.verify(router2, times(1)).resume();
        inOrder.verifyNoMoreInteractions();

        Assert.assertEquals(rulesV2, rulesManager1.getReleaseId().getVersion());
        Assert.assertEquals(rulesV2, rulesManager2.getReleaseId().getVersion());

        factCache.remove(keyForDatagrid1);
        factCache.put(keyForDatagrid1, generateFactTenSecondsAfter(1L, 20L));

        verify(additionsChannel1, times(1)).send(eq(60L));
        verify(replayChannel1, never()).send(any());

        verify(additionsChannel2, never()).send(any());
        verify(replayChannel2, times(1)).send(eq(10L));

        Assert.assertEquals(1, sessionCache1.size());
        Assert.assertEquals(1, sessionCache2.size());

        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        // Rules Update
        dataGridManager1.getReplicatedCache().put(RulesManager.RULES_VERSION, rulesV3);

        inOrder = Mockito.inOrder(router1);
        inOrder.verify(router1, times(1)).suspend();
        inOrder.verify(router1, times(1)).resume();
        inOrder.verifyNoMoreInteractions();

        inOrder = Mockito.inOrder(router2);
        inOrder.verify(router2, times(1)).suspend();
        inOrder.verify(router2, times(1)).resume();
        inOrder.verifyNoMoreInteractions();

        Assert.assertEquals(rulesV3, rulesManager1.getReleaseId().getVersion());
        Assert.assertEquals(rulesV3, rulesManager2.getReleaseId().getVersion());

        factCache.remove(keyForDatagrid1);
        factCache.put(keyForDatagrid1, generateFactTenSecondsAfter(1L, 30L));

        verify(additionsChannel1, times(1)).send(eq(180L));
        verify(replayChannel1, never()).send(any());

        verify(additionsChannel2, never()).send(any());
        verify(replayChannel2, times(1)).send(eq(60L));

        Assert.assertEquals(1, sessionCache1.size());
        Assert.assertEquals(1, sessionCache2.size());


        dataGridManager1.stop();
        dataGridManager2.stop();
        rulesManager1.stop();
        rulesManager2.stop();
    }

    private DataGridManager buildDataGridManager(RulesManager rulesManager, Router router) {
        DataGridManager dataGridManager2 = new DataGridManager();
        HAKieSessionBuilder sessionBuilder2 = new HAKieSessionBuilder(rulesManager, Executors.newFixedThreadPool(4));
        dataGridManager2.start(sessionBuilder2);
        dataGridManager2.getFactCache().addListener(new FactListenerPost(new KieSessionSaver(sessionBuilder2, dataGridManager2.getSessionCache())));

        Cache<String, String> infoCache = dataGridManager2.getReplicatedCache();
        ReleaseId releaseId = rulesManager.getReleaseId();
        infoCache.putIfAbsent(RulesManager.RULES_GROUP_ID, releaseId.getGroupId());
        infoCache.putIfAbsent(RulesManager.RULES_ARTIFACT_ID, releaseId.getArtifactId());
        infoCache.putIfAbsent(RulesManager.RULES_VERSION, releaseId.getVersion());
        infoCache.addListener(new UpdateVersionListener(router, rulesManager));

        return dataGridManager2;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
