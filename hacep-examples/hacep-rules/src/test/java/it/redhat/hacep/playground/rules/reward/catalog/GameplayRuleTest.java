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
        session = kieContainer.newKieSession(KSESSION_NAME);
        session.registerChannel(SysoutChannel.CHANNEL_ID, new SysoutChannel());
        session.registerChannel(AuditChannel.CHANNEL_ID, new AuditChannel());
        rulesFired = new RulesFiredAgendaEventListener();
        session.addEventListener(rulesFired);
        clock = session.getSessionClock();
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
