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

package it.redhat.hacep.playground.rules.reward.catalog;

import org.drools.core.time.SessionPseudoClock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertNotNull;

public class GameplayRuleTest {

    public static final String KSESSION_NAME = "hacep-sessions";
    private static KieContainer kieContainer;
    
    private KieSession session;
    
    private SessionPseudoClock clock;
    
    private RulesFiredAgendaEventListener rulesFired;
    
    @BeforeClass
    public static void init() throws Exception {
        kieContainer = setupKieContainer();
    }
    
    @Before
    public void setupTest() throws Exception {
        this.session = kieContainer.newKieSession(KSESSION_NAME);
        this.session.registerChannel(SysoutChannel.CHANNEL_ID, new SysoutChannel());
        this.session.registerChannel(AuditChannel.CHANNEL_ID, new AuditChannel());
        this.rulesFired = new RulesFiredAgendaEventListener();
        this.session.addEventListener(rulesFired);
        this.clock = session.getSessionClock();
    }
    
    @Test
    public void testCorrectSetup() {
        // test to verify correct initialization of kiecontainer and kiesession
        assertNotNull(session);
    }
    
    private static KieContainer setupKieContainer() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieContainer kieContainer = ks.getKieClasspathContainer();
        return kieContainer;
    }

}
