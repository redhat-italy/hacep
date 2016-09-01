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

import it.redhat.hacep.playground.cache.GameNameKey;
import it.redhat.hacep.playground.rules.model.Gameplay;
import it.redhat.hacep.playground.rules.model.outcome.GamePlayingStats;
import it.redhat.hacep.playground.rules.model.util.GameplayBetGenerator;
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

public class GameStatsDemoTest {

    private static KieBase kieBase;

    private KieSession session;

    private SessionPseudoClock clock;

    private RulesFiredAgendaEventListener rulesFired;

    private Channel outcomes;

    @BeforeClass
    public static void init() throws Exception {
        kieBase = KieAPITestUtils.setupKieBase("rules/reward-catalogue.drl", "rules/game-stats-demo.drl");
    }

    @Before
    public void setupTest() throws Exception {
        outcomes = mock(Channel.class);
        this.session = KieAPITestUtils.buildKieSession(kieBase);
        this.session.registerChannel("gameStats", outcomes);

        this.rulesFired = new RulesFiredAgendaEventListener();
        this.session.addEventListener(rulesFired);
        this.clock = this.session.getSessionClock();
    }

    @Test
    public void testGameStatsOutcome() {
        int gamePlayer1Generated = 50;
        int gamePlayer2Generated = 50;
        int gamesGenerated = gamePlayer1Generated + gamePlayer2Generated;
        long betAmount = 100;

        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);

        reset(outcomes);

        GameplayBetGenerator generatorPlayer1 = new GameplayBetGenerator();

        AtomicInteger counterPlayer1 = new AtomicInteger(1);

        generatorPlayer1
                .playerId(001l)
                .eventKey(new GameNameKey("txshldm", "Texas Holdem"))
                .gameName("Texas Holdem")
                .amount(betAmount)
                .timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS)
                .count(gamePlayer1Generated);


        Map<Integer, Integer> rulesFiredPlayer1 = generatorPlayer1.generate().stream()
                .map(this::insertGame)
                .collect(Collectors.toMap(i -> counterPlayer1.getAndIncrement(), Function.identity()));

        GameplayBetGenerator generatorPlayer2 = new GameplayBetGenerator();
        AtomicInteger counterPlayer2 = new AtomicInteger(1);

        generatorPlayer2
                .playerId(002l)
                .eventKey(new GameNameKey("txshldm", "Texas Holdem"))
                .gameName("Texas Holdem")
                .amount(betAmount)
                .timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS)
                .count(gamePlayer2Generated);

        Map<Integer, Integer> rulesFiredPlayer2 = generatorPlayer2.generate().stream()
                .map(this::insertGame)
                .collect(Collectors.toMap(i -> counterPlayer2.getAndIncrement(), Function.identity()));

        assertEquals(gamesGenerated, rulesFiredPlayer1.size() + rulesFiredPlayer2.size());

        ArgumentCaptor<GamePlayingStats> pplCaptor = ArgumentCaptor.forClass(GamePlayingStats.class);
        verify(outcomes, times(gamesGenerated)).send(pplCaptor.capture());

        List<GamePlayingStats> capturedPPL = pplCaptor.getAllValues();

        Assert.assertEquals(gamesGenerated, capturedPPL.size());

        GamePlayingStats gamePlayingStats = capturedPPL.get(gamesGenerated - 1);

        System.out.println(gamePlayingStats);

        Assert.assertEquals(gamesGenerated, gamePlayingStats.getNumberOfPlays().intValue());
        Assert.assertEquals(betAmount * gamesGenerated, gamePlayingStats.getAmountPlayed().intValue());
        Assert.assertEquals(2, gamePlayingStats.getNumberOfPlayers().intValue());
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
