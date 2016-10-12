package it.redhat.hacep.rules;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cluster.AbstractClusterTest;
import it.redhat.hacep.cluster.TestDroolsConfiguration;
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.rules.model.Gameplay;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestPassivationRetractRules extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestPassivationRetractRules.class);

    private final static TestDroolsConfiguration droolsConfiguration = TestDroolsConfiguration.buildRulesWithGamePlayRetract();

    private final static KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer(droolsConfiguration);

    private final static ExecutorService executorService = Executors.newFixedThreadPool(4);
    public static final String CACHE_NAME = "application";

    private ZonedDateTime now = ZonedDateTime.now();

    @Mock
    private Channel replayChannel;

    @Mock
    private Channel outcomesChannel;

    @Override
    public ConfigurationBuilder extendDefaultConfiguration(ConfigurationBuilder builder) {
        builder
                .persistence()
                .passivation(false)
                .addSingleFileStore()
                .shared(false)
                .preload(true)
                .fetchPersistentState(true)
                .purgeOnStartup(false)
                .location("./target")
                .async().enabled(true)
                .threadPoolSize(5).singleton().enabled(false)
                .eviction()
                .strategy(EvictionStrategy.LRU).type(EvictionType.COUNT).size(1024);
        return builder;
    }

    @Test
    public void testPassivation() {
        logger.info("Start test serialized rules");
        logger.info("Start test modified rules");

        droolsConfiguration.registerChannel("outcomes", outcomesChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, Object> cache = startNodes(1).getCache(CACHE_NAME);

        String key = "1";
        Object object = cache.get(key);
        HAKieSession session1 = new HAKieSession(droolsConfiguration, serializer, executorService);

        reset(outcomesChannel);

        session1.insert(generateFactTenSecondsAfter(1));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1));
        cache.put(key, session1);

        session1.insert(generateFactTenSecondsAfter(1));
        cache.put(key, session1);

        verify(outcomesChannel, times(5)).send(any());
        verifyNoMoreInteractions(outcomesChannel);

        reset(outcomesChannel);

        stopNodes();
        Cache<String, Object> cacheDeserialized = startNodes(1).getCache(CACHE_NAME);

        Object o = cacheDeserialized.get(key);
        Assert.assertTrue(o instanceof HAKieSerializedSession);
        HAKieSerializedSession haKieSerializedSession = (HAKieSerializedSession) o;
        HAKieSession sessionRebuilt = haKieSerializedSession.rebuild();

        sessionRebuilt.insert(generateFactTenSecondsAfter(1));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(1));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(1));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(1));
        cacheDeserialized.put(key, sessionRebuilt);

        sessionRebuilt.insert(generateFactTenSecondsAfter(1));
        cacheDeserialized.put(key, sessionRebuilt);

        verify(outcomesChannel, times(5)).send(any());
        verifyNoMoreInteractions(outcomesChannel);

        logger.info("End test serialized rules");
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    @Override
    protected DroolsConfiguration getKieBaseConfiguration() {
        return droolsConfiguration;
    }

    private Fact generateFactTenSecondsAfter(long ppid) {
        now = now.plusSeconds(10);
        return new Gameplay(ppid, ppid, new Date(now.toInstant().toEpochMilli()));
    }
}
