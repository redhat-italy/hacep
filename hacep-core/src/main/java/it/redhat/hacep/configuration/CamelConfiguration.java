package it.redhat.hacep.configuration;

import it.redhat.hacep.camel.KeyBuilder;
import it.redhat.hacep.camel.Putter;
import it.redhat.hacep.configuration.annotations.HACEPCamelContext;
import it.redhat.hacep.configuration.annotations.HACEPFactCache;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class CamelConfiguration {

    private final static Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

    public static final String CAMEL_ROUTE = "facts";

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Inject
    private KeyBuilder keyBuilder;

    @Inject
    @HACEPFactCache
    private Cache<Key, Fact> factCache;

    public CamelConfiguration() {
    }

    @Produces
    @ApplicationScoped
    @HACEPCamelContext
    public CamelContext createCamelContext() {
        CamelContext context = new DefaultCamelContext();
        try {

            JmsComponent component = JmsComponent.jmsComponent(jmsConfiguration.getConnectionFactory());
            context.addComponent("jms", component);
            context.addRoutes(new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    String uri = "jms:" + jmsConfiguration.getQueueName()
                            + "?concurrentConsumers=" + jmsConfiguration.getMaxConsumers()
                            + "&maxConcurrentConsumers=" + jmsConfiguration.getMaxConsumers();

                    from(uri)
                            .routeId(CAMEL_ROUTE)
                            .to("direct:putInGrid");
                }
            });

            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:putInGrid")
                            .bean(new Putter(keyBuilder, factCache), "put(${body})");
                }
            });

            context.start();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return context;
    }

}
