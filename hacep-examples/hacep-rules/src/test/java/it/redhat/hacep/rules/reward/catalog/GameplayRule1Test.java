package it.redhat.hacep.rules.reward.catalog;

import it.redhat.hacep.rules.model.Gameplay;
import it.redhat.hacep.rules.model.util.GameplayBuilder;
import it.redhat.hacep.rules.model.util.GameplayGenerator;
import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.time.SessionPseudoClock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message.Level;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class GameplayRule1Test {
    
    private static KieBase kieBase;
    
    private KieSession session;
    
    private SessionPseudoClock clock;
    
    private RulesFiredAgendaEventListener rulesFired;
    
    @BeforeClass
    public static void init() throws Exception {
        kieBase = setupKieBase("rules/reward-catalogue.drl", "rules/reward-catalogue-rule1.drl");
    }
    
    @Before
    public void setupTest() throws Exception {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        session = kieBase.newKieSession(sessionConf, null);
        session.registerChannel(SysoutChannel.CHANNEL_ID, new SysoutChannel());
        session.registerChannel(AuditChannel.CHANNEL_ID, new AuditChannel());
        rulesFired = new RulesFiredAgendaEventListener();
        session.addEventListener(rulesFired);
        clock = session.getSessionClock();
    }
    
    @Test
    public void testUserPlayGameNTimesInDDays() {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        //generate 100 gameplay events within 30d window
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(100);
        int count = 0;
        for (Gameplay g : generator.generate()) {
            count++;
            long gts = g.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g);
            session.fireAllRules();
            if (count < 100) {
                assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
            }
        }
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDDays2() {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        //generate 100 gameplay events within 30d window
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(100);
        int count = 0;
        for (Gameplay g : generator.generate()) {
            count++;
            long gts = g.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g);
            session.fireAllRules();
            if (count < 100) {
                assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
            }
        }
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //add another gameplay within the original time window
        long current = clock.getCurrentTime();
        long end = now + TimeUnit.DAYS.toMillis(30);
        clock.advanceTime((end-current)/2, TimeUnit.MILLISECONDS);
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDDays3() {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(100);
        int count = 0;
        long firstTimestamp = 0;
        long secondTimestamp = 0;
        for (Gameplay g : generator.generate()) {
            count++;
            long gts = g.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g);
            session.fireAllRules();
            if (count == 1) {
                firstTimestamp = gts;
            }
            if (count == 2) {
                secondTimestamp = gts;
            }
            if (count < 100) {
                assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
            }
        }
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //add another gameplay after the first gameplay expired but before the second expires
        //the rule should fire
        long current = clock.getCurrentTime();
        long endFirst = firstTimestamp + TimeUnit.DAYS.toMillis(30);
        long endSecond = secondTimestamp + TimeUnit.DAYS.toMillis(30);
        long delta = (((endSecond - endFirst)/2) + endFirst) - current;
        clock.advanceTime(delta, TimeUnit.MILLISECONDS);
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(2, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDDays4() {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(100);
        int count = 0;
        long secondTimestamp = 0;
        long thirdTimestamp = 0;
        for (Gameplay g : generator.generate()) {
            count++;
            long gts = g.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g);
            session.fireAllRules();
            if (count == 2) {
                secondTimestamp = gts;
            }
            if (count == 3) {
                thirdTimestamp = gts;
            }
            if (count < 100) {
                assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
            }
        }
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //add another gameplay after the first two gameplays expired, but before the third expires
        //the rule should not fire
        long current = clock.getCurrentTime();
        long endSecond = secondTimestamp + TimeUnit.DAYS.toMillis(30);
        long endThird = thirdTimestamp + TimeUnit.DAYS.toMillis(30);
        long delta = (((endThird - endSecond)/2) + endSecond) - current;
        clock.advanceTime(delta, TimeUnit.MILLISECONDS);
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDDays5() {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        //generate 99 gameplay events within 30d window
        GameplayGenerator generator = new GameplayGenerator();
        generator.playerId(100l).timestamp(System.currentTimeMillis(), 30, TimeUnit.DAYS).count(100);
        int count = 0;
        for (Gameplay g : generator.generate()) {
            count++;
            long gts = g.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g);
            session.fireAllRules();
            if (count < 100) {
                assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
            }
        }
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //add another gameplay, but for a different gamecode within the original time window
        //the rule should not fire
        long current = clock.getCurrentTime();
        long end = now + TimeUnit.DAYS.toMillis(30);
        clock.advanceTime((end-current)/2, TimeUnit.MILLISECONDS);
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    private static KieBase setupKieBase(String... resources) throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieBaseConfiguration config = ks.newKieBaseConfiguration();
        config.setOption( EventProcessingOption.STREAM );
        KieFileSystem kfs = ks.newKieFileSystem();
        KieRepository kr = ks.getRepository();
        
        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }
        
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb);
        
        KieContainer kc = ks.newKieContainer(kr.getDefaultReleaseId());
        
        return kc.newKieBase(config);
    }
    
    private static void hasErrors(KieBuilder kbuilder) throws Exception {
        if (kbuilder.getResults().hasMessages(Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + kbuilder.getResults().toString());
        }
    }

}
