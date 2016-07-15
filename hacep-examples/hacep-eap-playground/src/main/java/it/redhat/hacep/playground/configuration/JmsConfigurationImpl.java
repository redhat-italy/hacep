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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.ConnectionFactory;

@ApplicationScoped
public class JmsConfigurationImpl implements JmsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JmsConfigurationImpl.class);

    @Resource(lookup = "java:/HACEPConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory getConnectionFactory() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Provide connection factory [%s]", connectionFactory));
        }
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
