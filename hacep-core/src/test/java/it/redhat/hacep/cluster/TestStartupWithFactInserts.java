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
import it.redhat.hacep.configuration.DataGridManager;
import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.configuration.Router;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.rules.model.GameplayKey;
import org.infinispan.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

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
    public void setup() throws InterruptedException, ExecutionException {
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
    public void cleanup() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(hacep1::stop);
        executorService.submit(hacep2::stop);
        executorService.submit(hacep3::stop);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testStopStart() throws InterruptedException {
        reset(router1, additionsChannel1, replayChannel1, router2, additionsChannel2, replayChannel2, router3, additionsChannel3, replayChannel3);

        DataGridManager dataGridManager1 = hacep1.getDataGridManager();

        Thread.sleep(5000);

        ExecutorService factExecutorService = Executors.newFixedThreadPool(1);
        factExecutorService.submit( () -> {
            for(int i = 0; i<=Integer.MAX_VALUE; i++){
                try {
                    Cache<Key, Fact> factCache = dataGridManager1.getFactCache();
                    Key key = new GameplayKey(Integer.toString(i), "" +(i % 20) );
                    factCache.put(key, new TestFact(i % 20, 100L, new Date(now.toInstant().toEpochMilli()), key));
                    LOGGER.info("Message [{}] sent!", i);
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    //
                } catch (Exception e) {
                    throw new RuntimeException( "This should not happen!" );
                }
            }
        } );

        //let's have some messages flowing through
        Thread.sleep(5000);

        //stop
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(hacep3::stop);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        //let's have some more messages flowing through
        Thread.sleep(5000);

        //start
        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(hacep3::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        factExecutorService.shutdownNow();
    }
}
