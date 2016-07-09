package it.redhat.hacep.playground.configuration;

import it.redhat.hacep.configuration.JmsConfiguration;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.ConnectionFactory;

@ApplicationScoped
public class JmsConfigurationImpl implements JmsConfiguration {

    @Override
    public ConnectionFactory getConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory;
        if (getQueueSecurity()) {
            connectionFactory = new ActiveMQConnectionFactory(getQueueUsername(), getQueuePassword(), getQueueBrokerUrl());
        } else {
            connectionFactory = new ActiveMQConnectionFactory(getQueueBrokerUrl());
        }
        return new PooledConnectionFactory(connectionFactory);
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

    private String getQueueBrokerUrl() {
        try {
            return System.getProperty("queue.url", "tcp://localhost:61616");
        } catch (IllegalArgumentException e) {
            return "tcp://localhost:61616";
        }
    }

    private boolean getQueueSecurity() {
        try {
            return Boolean.valueOf(System.getProperty("queue.security", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getQueueUsername() {
        try {
            return System.getProperty("queue.usr", "admin");
        } catch (IllegalArgumentException e) {
            return "admin";
        }
    }

    private String getQueuePassword() {
        try {
            return System.getProperty("queue.pwd", "admin");
        } catch (IllegalArgumentException e) {
            return "admin";
        }
    }

}
