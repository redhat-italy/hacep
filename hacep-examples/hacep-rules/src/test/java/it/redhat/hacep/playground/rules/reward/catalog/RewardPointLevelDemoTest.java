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

import it.redhat.hacep.playground.rules.model.Gameplay;
import it.redhat.hacep.playground.rules.model.outcome.PlayerPointLevel;
import it.redhat.hacep.playground.rules.model.util.GameplayGenerator;
import org.drools.core.time.SessionPseudoClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieSession;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RewardPointLevelDemoTest {

    private static KieBase kieBase;

    private KieSession session;

    private SessionPseudoClock clock;

    private RulesFiredAgendaEventListener rulesFired;

    private Channel outcomes;

    @BeforeClass
    public static void init() throws Exception {
        kieBase = KieAPITestUtils.setupKieBase("rules/reward-catalogue.drl", "rules/reward-point-level-demo.drl");
    }

    @Before
    public void setupTest() throws Exception {
        outcomes = mock(Channel.class);
        this.session = KieAPITestUtils.buildKieSession(kieBase);
        this.session.registerChannel("playerPointsLevel", outcomes);

        this.rulesFired = new RulesFiredAgendaEventListener();
        this.session.addEventListener(rulesFired);
        this.clock = this.session.getSessionClock();
    }

    @Test
    public void testUserGainPointsAndLevels() {
        int gameGenerated = 202;

        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);

        reset(outcomes);

        //generate 100 gameplay events within 30d window
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(gameGenerated);
        AtomicInteger counter = new AtomicInteger(1);
        Map<Integer, Integer> rulesFired = generator.generate().stream()
                .map(this::insertGame)
                .collect(Collectors.toMap(i -> counter.getAndIncrement(), Function.identity()));
        assertEquals(gameGenerated, rulesFired.size());

        ArgumentCaptor<PlayerPointLevel> pplCaptor = ArgumentCaptor.forClass(PlayerPointLevel.class);
        verify(outcomes, times(gameGenerated)).send(pplCaptor.capture());

        List<PlayerPointLevel> capturedPPL = pplCaptor.getAllValues();

        Assert.assertEquals(gameGenerated, capturedPPL.size());

        PlayerPointLevel playerPointLevel = capturedPPL.get(gameGenerated - 1);
        System.out.println(playerPointLevel);
        Assert.assertEquals(20, playerPointLevel.getLevel().intValue());
        Assert.assertEquals(2, playerPointLevel.getPoints().intValue());
    }

    private int insertGame(Gameplay g) {
        long gts = g.getTimestamp().getTime();
        long current = clock.getCurrentTime();
        clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
        session.insert(g);
        session.fireAllRules();
        int firedEventsCount = this.rulesFired.getAfterMatchFiredEvents().size();
        this.rulesFired.clear();
        return firedEventsCount;
    }
}
