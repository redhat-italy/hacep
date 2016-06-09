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

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class GameplayRule2Test {
    
    private static KieBase kieBase;
    
    private KieSession session;
    
    private SessionPseudoClock clock;
    
    private RulesFiredAgendaEventListener rulesFired;
    
    @BeforeClass
    public static void init() throws Exception {
        kieBase = setupKieBase("rules/reward-catalogue.drl", "rules/reward-catalogue-rule2.drl");
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
    public void testUserPlayGameNTimesInDConsecutiveDays() throws Exception {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        
        //Generate 7 events per day on days 1-7 (starting from the timestamp of event 1)
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(now, 1, TimeUnit.HOURS).count(6).generate();
        for (int i = 1; i < 7; i++) {
            long start = now + TimeUnit.DAYS.toMillis(i);
            generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(start, 1, TimeUnit.HOURS).count(7).generate();
        }
        for (Gameplay g1 : generator.getGenerated()) {
            long gts = g1.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
        }
        //Add another event on day 7
        long current = clock.getCurrentTime();
        long target = (now + TimeUnit.DAYS.toMillis(7) - current)/2;
        clock.advanceTime(target, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDConsecutiveDays2() throws Exception {
        long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        
        //Generate 7 events per day on days 1-7 (starting from the timestamp of event 1)
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(now, 1, TimeUnit.HOURS).count(6).generate();
        for (int i = 1; i < 7; i++) {
            long start = now + TimeUnit.DAYS.toMillis(i);
            generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(start, 1, TimeUnit.HOURS).count(7).generate();
        }
        for (Gameplay g1 : generator.getGenerated()) {
            long gts = g1.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
        }
        //Add another event on day 7
        long current = clock.getCurrentTime();
        long target = (now + TimeUnit.DAYS.toMillis(7) - current)/2;
        clock.advanceTime(target, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //Add another event on day 8
        //rules should not fire again - total count = <50
        current = clock.getCurrentTime();
        target = (now + TimeUnit.DAYS.toMillis(8) + TimeUnit.HOURS.toMillis(1));
        clock.advanceTime(target-current, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDConsecutiveDays3() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        long now = sdf.parse("01-01-2016").getTime();
                
        //long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        
        //Generate 7 events per day on days 1-7 (starting from the timestamp of event 1)
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(now, 1, TimeUnit.HOURS).count(6).generate();
        for (int i = 1; i < 7; i++) {
            long start = now + TimeUnit.DAYS.toMillis(i);
            generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(start, 1, TimeUnit.HOURS).count(7).generate();
        }
        for (Gameplay g1 : generator.getGenerated()) {
            long gts = g1.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
        }
        //Add another event on day 7
        long current = clock.getCurrentTime();
        long target = (now + TimeUnit.DAYS.toMillis(7) - current)/2;
        clock.advanceTime(target, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //Add 6 events on day 8
        long start = now + TimeUnit.DAYS.toMillis(7);
        generator.reset().timestamp(start, 1, TimeUnit.HOURS).count(6);
        for (Gameplay g1: generator.generate()) {
            long gts = g1.getTimestamp().getTime();
            current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        }
        
        //Add another event on day 8
        //rules should fire again - total count = 50
        target = TimeUnit.SECONDS.toMillis(5);
        clock.advanceTime(target, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(2, rulesFired.getAfterMatchFiredEvents().size());
    }
    
    @Test
    public void testUserPlayGameNTimesInDConsecutiveDays4() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        long now = sdf.parse("01-01-2016").getTime();
                
        //long now = System.currentTimeMillis();
        clock.advanceTime(now, TimeUnit.MILLISECONDS);
        
        GameplayGenerator generator = new GameplayGenerator();
        
        //Generate 7 events per day on days 1-7 (starting from the timestamp of event 1)
        Gameplay g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(now, 1, TimeUnit.HOURS).count(6).generate();
        for (int i = 1; i < 7; i++) {
            long start = now + TimeUnit.DAYS.toMillis(i);
            generator.playerId(100l).mode(GameplayGenerator.MODE_INTERVAL).timestamp(start, 1, TimeUnit.HOURS).count(7).generate();
        }
        for (Gameplay g1 : generator.getGenerated()) {
            long gts = g1.getTimestamp().getTime();
            long current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(0, rulesFired.getAfterMatchFiredEvents().size());
        }
        //Add another event on day 7
        long current = clock.getCurrentTime();
        long target = (now + TimeUnit.DAYS.toMillis(7) - current)/2;
        clock.advanceTime(target, TimeUnit.MILLISECONDS);
        g = new GameplayBuilder().playerId(100l).timestamp(clock.getCurrentTime()).build();
        session.insert(g);
        session.fireAllRules();
        assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        
        //Add 10 events on day 9 - rule should not fire, not 7 consecutive days
        long start = now + TimeUnit.DAYS.toMillis(8);
        generator.reset().timestamp(start, 1, TimeUnit.HOURS).count(10);
        for (Gameplay g1: generator.generate()) {
            long gts = g1.getTimestamp().getTime();
            current = clock.getCurrentTime();
            clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
            session.insert(g1);
            session.fireAllRules();
            assertEquals(1, rulesFired.getAfterMatchFiredEvents().size());
        }
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
