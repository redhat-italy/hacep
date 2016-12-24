package it.redhat.hacep.rules;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cluster.AbstractClusterTest;
import it.redhat.hacep.cluster.RulesConfigurationTestImpl;
import it.redhat.hacep.cluster.TestFact;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import org.infinispan.Cache;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static it.redhat.hacep.cluster.RulesConfigurationTestImpl.RulesTestBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestModifiedRules extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestModifiedRules.class);

    private static ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Mock
    private Channel replayChannel;

    @Mock
    private Channel otherReplayChannel;

    @Mock
    private Channel additionsChannel;

    @Mock
    private Channel otherAdditionsChannel;

    private ZonedDateTime now = ZonedDateTime.now();

    @Test
    public void testRuleUpdateLiveSession() throws IOException, URISyntaxException {
        System.setProperty("grid.buffer", "10");

        logger.info("Start test modified rules");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        session1.insert(generateFactTenSecondsAfter(1L, 20L));

        verify(replayChannel, never()).send(any());

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(2)).send(any());

        reset(replayChannel, additionsChannel);

        String version = RulesTestBuilder.buildV2();
        rulesManager.updateToVersion(version);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));

        verify(replayChannel, never()).send(any());

        verify(additionsChannel, times(1)).send(eq(120L));
        verify(additionsChannel, atMost(1)).send(any());

        logger.info("End test modified rules");
        rulesManager.stop();
    }

    @Test
    public void testRuleUpdateSerializedSession() throws IOException, URISyntaxException {
        System.setProperty("grid.buffer", "10");

        logger.info("Start test modified rules");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        session1.insert(generateFactTenSecondsAfter(1L, 20L));

        verify(replayChannel, never()).send(any());

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(2)).send(any());

        reset(replayChannel, additionsChannel);

        //Test update on an other serialized node with a new kieContainer
        byte[] serializedSession = session1.wrapWithSerializedSession().getSerializedSession();

        rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", otherAdditionsChannel, otherReplayChannel);

        rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        HAKieSerializedSession newSerializedSession = new HAKieSerializedSession(rulesManager, executorService, serializedSession);
        HAKieSession rebuiltSession = newSerializedSession.rebuild();

        String version = RulesTestBuilder.buildV2();
        rulesManager.updateToVersion(version);

        rebuiltSession.insert(generateFactTenSecondsAfter(1L, 20L));

        verify(replayChannel, never()).send(any());
        verify(additionsChannel, never()).send(any());

        verify(otherReplayChannel, never()).send(any());
        inOrder = inOrder(otherAdditionsChannel);
        inOrder.verify(otherAdditionsChannel, times(1)).send(eq(100L));
        inOrder.verifyNoMoreInteractions();

        logger.info("End test modified rules");
        rulesManager.stop();
    }

    @Test
    public void testNonEmptyHASession() throws IOException, URISyntaxException {
        System.setProperty("grid.buffer", "10");

        logger.info("Start test modified rules");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, Object> cache1 = startNodes(2, rulesManager).getCache();
        Cache<String, Object> cache2 = startNodes(2, rulesManager).getCache();

        String key = "2";
        HAKieSession session1 = new HAKieSession(rulesManager, executorService);

        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put(key, session1);

        verify(replayChannel, never()).send(any());

        InOrder inOrder = inOrder(additionsChannel);
        inOrder.verify(additionsChannel, times(1)).send(eq(10L));
        inOrder.verify(additionsChannel, times(1)).send(eq(30L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(additionsChannel, times(2)).send(any());

        HAKieSession serializedSessionCopy = (HAKieSession) cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(serializedSessionCopy.isSerialized());

        reset(replayChannel, additionsChannel);

        ((HAKieSerializedSession) serializedSessionCopy).createSnapshot();
        ((HAKieSerializedSession) serializedSessionCopy).waitForSnapshotToComplete();

        inOrder = inOrder(replayChannel);
        inOrder.verify(replayChannel, times(1)).send(eq(30L));
        inOrder.verifyNoMoreInteractions();

        reset(replayChannel, additionsChannel);

        rulesConfigurationTest = RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", otherAdditionsChannel, otherReplayChannel);

        rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        byte[] serializedSession = ((HAKieSerializedSession) serializedSessionCopy).getSerializedSession();
        HAKieSession session2 = new HAKieSerializedSession(rulesManager, executorService, serializedSession).rebuild();

        String version = RulesTestBuilder.buildV2();
        rulesManager.updateToVersion(version);

        session2.insert(generateFactTenSecondsAfter(1L, 30L));

        verify(replayChannel, never()).send(any());
        verify(additionsChannel, never()).send(any());

        verify(otherReplayChannel, never()).send(any());
        verify(otherAdditionsChannel, atMost(1)).send(any());
        verify(otherAdditionsChannel, times(1)).send(eq(120L));

        logger.info("End test modified rules");
        rulesManager.stop();
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
