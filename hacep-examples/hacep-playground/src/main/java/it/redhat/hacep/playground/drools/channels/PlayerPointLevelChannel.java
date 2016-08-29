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

import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.playground.MessageSender;
import it.redhat.hacep.playground.rules.model.outcome.PlayerPointLevel;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PlayerPointLevelChannel implements Channel {

    public static final String CHANNEL_ID = "playerPointsLevel";
    private static final Logger LOGGER = LoggerFactory.getLogger("it.redhat.hacep.logger");

    public static final String queueName = "events";

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Inject
    private MessageSender sender;

    @Override
    public void send(Object object) {
        if (object != null && object.getClass().isAssignableFrom(PlayerPointLevel.class)) {
            PlayerPointLevel ppl = (PlayerPointLevel) object;
            sender.send(queueName, String.format("P%05d", ppl.getPlayerId()), ppl);
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unrecognized object input for channel PlaeryLevelPoints [" + object + "]");
            }
        }
    }

}
