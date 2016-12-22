package it.redhat.hacep.cluster;

import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.configuration.AbstractBaseDroolsConfiguration;
import it.redhat.hacep.distributed.Snapshotter;
import it.redhat.hacep.model.Fact;
import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.*;

import static org.mockito.Mockito.reset;

@RunWith(Arquillian.class)
public class TestSnapshotter extends AbstractClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(TestSnapshotter.class);

    private static TestDroolsConfiguration droolsConfiguration = TestDroolsConfiguration.buildV1();

    private static ExecutorService executorService = Executors.newFixedThreadPool(4);

    private Channel replayChannel = Mockito.mock(Channel.class);

    private Channel additionsChannel = Mockito.mock(Channel.class);

    private ZonedDateTime now = ZonedDateTime.now();

    @Deployment
    public static Archive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testDistributedSnapshots() throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("Start test Distributed Snapshots");

        droolsConfiguration.registerChannel("additions", additionsChannel, replayChannel);
        droolsConfiguration.setMaxBufferSize(10);

        Cache<String, HAKieSession> cache1 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache2 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache3 = startNodes(2).getCache();
        Cache<String, HAKieSession> cache4 = startNodes(2).getCache();

        reset(replayChannel, additionsChannel);

        HAKieSession session1 = new HAKieSession(droolsConfiguration, executorService);

        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 10L));
        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 20L));
        cache1.put("1", session1);

        session1.insert(generateFactTenSecondsAfter(1L, 30L));
        cache1.put("1", session1);

        ExecutorService des = new DefaultExecutorService(cache1);
        Future<Boolean> future = des.submit(new Snapshotter());
        des.shutdown();
        Assert.assertEquals(true, future.get(10, TimeUnit.SECONDS));

        logger.info("End test Distributed Snapshots");
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }

    @Override
    protected AbstractBaseDroolsConfiguration getKieBaseConfiguration() {
        return droolsConfiguration;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }

}
