package it.redhat.hacep.configuration;

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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class CamelConfiguration {

    private final static Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

    public static final String CAMEL_ROUTE = "facts";

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Inject
    private DroolsConfiguration droolsConfiguration;

    @Inject
    @HACEPFactCache
    private Cache<Key, Fact> factCache;

    private CamelContext camelContext;

    public CamelConfiguration() {
    }

    @PostConstruct
    public void createCamelContext() {
        camelContext = new DefaultCamelContext();
        try {

            JmsComponent component = JmsComponent.jmsComponent(jmsConfiguration.getConnectionFactory());
            camelContext.addComponent("jms", component);
            camelContext.addRoutes(new RouteBuilder() {

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

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:putInGrid")
                            .bean(new Putter(droolsConfiguration.getKeyBuilder(), factCache), "put(${body})");
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Produces
    @ApplicationScoped
    @HACEPCamelContext
    public CamelContext getCamelContext() {
        return camelContext;
    }
}
