package it.redhat.hacep.configuration;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.ConnectionFactory;

@ApplicationScoped
public class JmsConfigurationImpl implements JmsConfiguration {

    @Resource(name = "java:/HACEPConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Override
    public String getQueueName() {
        try {
            return System.getProperty("queue.name", "facts");
        } catch (IllegalArgumentException e) {
            return "facts";
        }
    }

    @Override
    public int getMaxConsumers() {
        try {
            return Integer.valueOf(System.getProperty("queue.consumers", "5"));
        } catch (IllegalArgumentException e) {
            return 5;
        }
    }

}
