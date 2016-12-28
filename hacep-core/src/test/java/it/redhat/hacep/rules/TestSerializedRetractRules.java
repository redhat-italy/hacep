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

import it.redhat.hacep.cluster.RulesConfigurationTestImpl;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.rules.model.Gameplay;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieSession;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestSerializedRetractRules {

    private final static Logger logger = LoggerFactory.getLogger(TestSerializedRetractRules.class);

    private ZonedDateTime now;

    @Mock
    private Channel outcomesChannel;

    @Before
    public void init() {
        now = ZonedDateTime.now();
    }

    @Test
    public void testSessionSerialization() {
        logger.info("Start test serialized rules");

        System.setProperty("grid.buffer", "10");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesConfigurationTestImpl.RulesTestBuilder.buildRulesWithGamePlayRetract();

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        reset(outcomesChannel);

        KieSession kieSession = rulesManager.newKieSession();
        kieSession.registerChannel("outcomes", outcomesChannel);

        kieSession.insert(generateFactTenSecondsAfter(1));
        kieSession.fireAllRules();

        verify(outcomesChannel, times(1)).send(any());
        verifyNoMoreInteractions(outcomesChannel);

        reset(outcomesChannel);

        byte[] kieSessionBytes = rulesManager.serialize(kieSession);
        Assert.assertTrue(kieSessionBytes.length > 0);
        kieSession.dispose();

        KieSession kieSessionDeserialized = rulesManager.deserializeOrCreate(kieSessionBytes);
        kieSessionDeserialized.registerChannel("outcomes", outcomesChannel);

        kieSessionDeserialized.insert(generateFactTenSecondsAfter(1));
        kieSessionDeserialized.fireAllRules();

        verify(outcomesChannel, times(1)).send(any());
        verifyNoMoreInteractions(outcomesChannel);

        logger.info("End test serialized rules");
        rulesManager.stop();
    }

    private Fact generateFactTenSecondsAfter(long ppid) {
        now = now.plusSeconds(10);
        return new Gameplay(ppid, ppid, new Date(now.toInstant().toEpochMilli()));
    }
}
