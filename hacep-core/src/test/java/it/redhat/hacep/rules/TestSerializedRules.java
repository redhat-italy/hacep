package it.redhat.hacep.rules;

import it.redhat.hacep.cluster.RulesConfigurationTestImpl;
import it.redhat.hacep.cluster.TestFact;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieSession;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;

@RunWith(MockitoJUnitRunner.class)
public class TestSerializedRules {

    private final static Logger logger = LoggerFactory.getLogger(TestSerializedRules.class);

    private ZonedDateTime now;

    @Mock
    private Channel additionsChannel;

    @Mock
    private Channel locksChannel;

    @Before
    public void init() {
        now = ZonedDateTime.now();
    }

    @Test
    public void testSessionSerialization() {
        System.setProperty("grid.buffer", "10");

        logger.info("Start test serialized rules");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesConfigurationTestImpl.RulesTestBuilder.buildRulesWithRetract();

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);


        KieSession kieSession = rulesManager.newKieSession();
        kieSession.registerChannel("additions", additionsChannel);
        kieSession.registerChannel("locks", locksChannel);

        kieSession.insert(generateFactTenSecondsAfter(1, 1L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 2L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 3L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 4L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 5L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 6L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 7L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 8L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 9L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 10L));
        // LOCK inserted expires in 25 sec.
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 1L));
        kieSession.fireAllRules();

        kieSession.insert(generateFactTenSecondsAfter(1, 2L));
        kieSession.fireAllRules();

        // 30 sec after - lock should be expired
        kieSession.insert(generateFactTenSecondsAfter(1, 3L));
        kieSession.fireAllRules();

        byte[] kieSessionBytes = rulesManager.serialize(kieSession);
        Assert.assertTrue(kieSessionBytes.length > 0);
        kieSession.dispose();

        KieSession kieSessionDeserialized = rulesManager.deserializeOrCreate(kieSessionBytes);
        kieSessionDeserialized.registerChannel("additions", additionsChannel);
        kieSessionDeserialized.registerChannel("locks", locksChannel);

        kieSessionDeserialized.insert(generateFactTenSecondsAfter(1, 10L));
        // LOCK inserted expires in 25 sec.
        kieSessionDeserialized.fireAllRules();

        kieSessionDeserialized.insert(generateFactTenSecondsAfter(1, 0L));
        kieSessionDeserialized.fireAllRules();

        kieSessionDeserialized.insert(generateFactTenSecondsAfter(1, 0L));
        kieSessionDeserialized.fireAllRules();

        // 30 sec after - lock should be expired
        // And inserted again by this fact (expires in 25 sec)
        kieSessionDeserialized.insert(generateFactTenSecondsAfter(1, 10L));
        kieSessionDeserialized.fireAllRules();


        InOrder order = Mockito.inOrder(additionsChannel, locksChannel);
        order.verify(additionsChannel).send(eq(1L));
        order.verify(additionsChannel).send(eq(2L));
        order.verify(additionsChannel).send(eq(3L));
        order.verify(additionsChannel).send(eq(4L));
        order.verify(additionsChannel).send(eq(5L));
        order.verify(additionsChannel).send(eq(6L));
        order.verify(additionsChannel).send(eq(7L));
        order.verify(additionsChannel).send(eq(8L));
        order.verify(additionsChannel).send(eq(9L));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verify(locksChannel).send(eq("REMOVED"));
        order.verify(additionsChannel).send(eq(3L));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verify(locksChannel).send(eq("REMOVED"));
        order.verify(locksChannel).send(eq("INSERTED"));
        order.verifyNoMoreInteractions();

        logger.info("End test serialized rules");
        rulesManager.stop();
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
