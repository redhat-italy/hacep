package it.redhat.hacep.rules;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cluster.AbstractClusterTest;
import it.redhat.hacep.cluster.TestDroolsConfiguration;
import it.redhat.hacep.cluster.TestFact;
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
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

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestModifiedRules extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestModifiedRules.class);

    private static TestDroolsConfiguration droolsConfiguration = TestDroolsConfiguration.buildV1();

    private static KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer(droolsConfiguration);

    private static ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Mock
    private Channel replayChannel;
    @Mock
    private Channel additionsChannel;

    private ZonedDateTime now = ZonedDateTime.now();

    @Test
    public void testNonEmptyHASession() {
        logger.info("Start test modified rules");

        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, Object> cache1 = startNodes(2).getCache();
        Cache<String, Object> cache2 = startNodes(2).getCache();

        String key = "2";
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

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

        Object serializedSessionCopy = cache2.get(key);

        Assert.assertNotNull(serializedSessionCopy);
        Assert.assertTrue(HAKieSerializedSession.class.isAssignableFrom(serializedSessionCopy.getClass()));

        reset(replayChannel, additionsChannel);


        ((HAKieSerializedSession) serializedSessionCopy).createSnapshot();
        HAKieSession session2 = ((HAKieSerializedSession) serializedSessionCopy).rebuild();

        droolsConfiguration.upgradeToV2();

        session2.insert(generateFactTenSecondsAfter(1L, 30L));

        inOrder = inOrder(replayChannel);
        inOrder.verify(replayChannel, times(1)).send(eq(30L));
        inOrder.verifyNoMoreInteractions();
        // Double check on total number of calls to the method send
        verify(replayChannel, times(1)).send(any());

        verify(additionsChannel, atMost(1)).send(any());
        verify(additionsChannel, times(1)).send(eq(120L));

        logger.info("End test modified rules");
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    @Override
    protected DroolsConfiguration getKieBaseConfiguration() {
        return droolsConfiguration;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
