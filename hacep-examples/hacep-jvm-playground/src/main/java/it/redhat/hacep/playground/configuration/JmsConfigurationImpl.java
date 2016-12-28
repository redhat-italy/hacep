/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    @Override
    public String getCommandsQueueName() {
        try {
            return System.getProperty("commands.queue.name", "commands");
        } catch (IllegalArgumentException e) {
            return "commands";
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
