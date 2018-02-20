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
import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.configuration.Router;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Mockito.*;

public class TestSuspendAndResume {

    private final Logger LOGGER = LoggerFactory.getLogger(TestSuspendAndResume.class);

    private ZonedDateTime now = ZonedDateTime.now();

    private Channel additionsChannel1;
    private Channel additionsChannel2;
    private Channel replayChannel1;
    private Channel replayChannel2;
    private Router router1;
    private Router router2;
    private JmsConfiguration jmsConfig1;
    private JmsConfiguration jmsConfig2;

    private HACEPImpl hacep2;
    private HACEPImpl hacep1;

    @Before
    public void setup() throws InterruptedException {
        System.setProperty("jgroups.configuration", "jgroups-test-tcp.xml");

        additionsChannel1 = mock(Channel.class);
        additionsChannel2 = mock(Channel.class);
        replayChannel1 = mock(Channel.class);
        replayChannel2 = mock(Channel.class);
        router1 = mock(Router.class);
        router2 = mock(Router.class);
        jmsConfig1 = mock(JmsConfiguration.class);
        jmsConfig2 = mock(JmsConfiguration.class);

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

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(hacep1::start);
        executorService.submit(hacep2::start);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @After
    public void cleanup() {
        hacep1.stop();
        hacep2.stop();
    }

    @Test
    public void testSuspend_from_Node1() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        hacep1.suspend();

        verify(router1, times(1)).suspend();
        verify(router2, times(1)).suspend();

    }

    @Test
    public void testSuspend_from_Node2() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        hacep2.suspend();

        verify(router1, times(1)).suspend();
        verify(router2, times(1)).suspend();
        verify(router1, times(0)).resume();
        verify(router2, times(0)).resume();
    }

    @Test
    public void testSuspend_and_resume_from_Node1() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        hacep1.suspend();
        hacep1.resume();

        verify(router1, times(1)).suspend();
        verify(router2, times(1)).suspend();

        verify(router1, times(1)).resume();
        verify(router2, times(1)).resume();
    }

    @Test
    public void testSuspend_and_resume_from_Node2() throws InterruptedException {
        reset(router1, router2, additionsChannel1, replayChannel1, additionsChannel2, replayChannel2);

        hacep1.suspend();
        hacep2.resume();

        verify(router1, times(1)).suspend();
        verify(router2, times(1)).suspend();

        verify(router1, times(1)).resume();
        verify(router2, times(1)).resume();
    }
}
