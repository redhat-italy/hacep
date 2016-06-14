package it.redhat.hacep.configuration;

import javax.jms.ConnectionFactory;

public interface JmsConfiguration {

    ConnectionFactory getConnectionFactory();

    String getQueueName();

    int getMaxConsumers();
}
