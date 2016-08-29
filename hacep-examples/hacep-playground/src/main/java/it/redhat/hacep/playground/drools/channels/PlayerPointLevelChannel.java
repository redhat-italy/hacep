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

package it.redhat.hacep.playground.drools.channels;

import com.google.gson.Gson;
import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.playground.rules.model.outcome.PlayerPointLevel;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jms.*;

public class PlayerPointLevelChannel implements Channel {

    public static final String CHANNEL_ID = "playerPointsLevel";
    private static final Logger LOGGER = LoggerFactory.getLogger("it.redhat.hacep.logger");

    public static final String queueName = "events";

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Override
    public void send(Object object) {
        if (object != null && object.getClass().isAssignableFrom(PlayerPointLevel.class)) {
            PlayerPointLevel ppl = (PlayerPointLevel) object;
            Gson gson = new Gson();
            String json = gson.toJson(object);
            Connection connection = null;
            Session session = null;
            try {
                connection = jmsConfiguration.getConnectionFactory().createConnection();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue destination = session.createQueue(queueName);
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                TextMessage message = session.createTextMessage(json);
                message.setStringProperty("JMSXGroupID", String.format("P%05d", ppl.getPlayerId()));
                producer.send(message);
            } catch (JMSException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (session != null) try {
                    session.close();
                } catch (JMSException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
                if (connection != null) try {
                    connection.close();
                } catch (JMSException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }

        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unrecognized object input for channel PlaeryLevelPoints [" + object + "]");
            }
        }
    }

}
