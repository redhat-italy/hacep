package it.redhat.hacep.playground;


import it.redhat.hacep.camel.CamelRouter;
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.configuration.HACEP;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.playground.drools.channels.PlayerPointLevelChannel;
import it.redhat.hacep.playground.rules.model.Gameplay;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.Channel;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repeatable.Repeat;
import repeatable.RepeatRule;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestSerializationRetractRule {

    @Rule
    public RepeatRule repeatRule = new RepeatRule();

    private final static Logger logger = LoggerFactory.getLogger(TestSerializationRetractRule.class);

    private ZonedDateTime now = ZonedDateTime.now();

    @Mock
    private Channel replayChannel;

    @Mock
    private Channel playerPointLevelChannel;

    @Mock
    private Channel outcomesChannel;

    private HACEP hacep;

    private DroolsConfiguration droolsConfiguration;

    private ModelCamelContext camelContext;

    @Before
    public void init() throws Exception {
        System.setProperty("grid.persistence.evictionSize", "5");
        System.setProperty("grid.persistence.location", "./target");
        System.setProperty("grid.persistence", "true");
        System.setProperty("grid.persistence.passivation", "false");

        Weld weld = new Weld();
        WeldContainer container = weld.initialize();

        droolsConfiguration = container.instance().select(DroolsConfiguration.class).get();
        droolsConfiguration.getChannels().put("outcomes", outcomesChannel);
        droolsConfiguration.getChannels().put(PlayerPointLevelChannel.CHANNEL_ID, playerPointLevelChannel);
        droolsConfiguration.getReplayChannels().put("outcomes", replayChannel);
        droolsConfiguration.getReplayChannels().put(PlayerPointLevelChannel.CHANNEL_ID, playerPointLevelChannel);

        CamelRouter camelRouter = container.instance().select(CamelRouter.class).get();
        camelContext = camelRouter.getCamelContext();
        camelContext.getRouteDefinition(CamelRouter.CAMEL_ROUTE).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:test");
            }
        });

        hacep = container.instance().select(HACEP.class).get();
        hacep.start();
    }

    @After
    public void destroy() {
        if (hacep != null) {
            hacep.stop();
        }
    }

    @Test
    @Repeat(value = 2)
    public void testPassivation() {
        logger.info("Start test serialized rules");

        reset(outcomesChannel);
        camelContext
                .createProducerTemplate()
                .sendBody("direct:test", generateFactTenSecondsAfter(1));

        verify(outcomesChannel).send(any());
        verifyNoMoreInteractions(outcomesChannel);
    }

    private Fact generateFactTenSecondsAfter(long ppid) {
        now = now.plusSeconds(10);
        return new Gameplay(ppid, ppid, new Date(now.toInstant().toEpochMilli()));
    }
}
