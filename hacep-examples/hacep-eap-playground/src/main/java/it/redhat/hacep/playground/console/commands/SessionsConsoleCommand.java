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

package it.redhat.hacep.playground.console.commands;

import it.redhat.hacep.configuration.annotations.HACEPSessionCache;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.playground.JDGUtility;
import it.redhat.hacep.playground.console.ReSTUI;
import it.redhat.hacep.playground.console.UI;
import it.redhat.hacep.playground.console.commands.dto.NodeType;
import it.redhat.hacep.playground.console.commands.dto.SessionDataObjectInformation;
import it.redhat.hacep.playground.console.support.IllegalParametersException;
import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

public class SessionsConsoleCommand implements ConsoleCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReSTUI.class);

    private static final String COMMAND_NAME = "sessions";

    @Inject
    private JDGUtility jdgUtility;

    @Inject
    @HACEPSessionCache
    private Cache<Key, Object> sessionCache;

    public SessionsConsoleCommand() {
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Start execute command 'sessions'");
        }
        Map<Address, List<SessionDataObjectInformation>> sessions = new HashMap<>();

        for (Map.Entry<Key, List<Address>> entry : jdgUtility.getKeysAddresses(sessionCache).entrySet()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Key [" + entry.getKey() + "] List{" + entry.getValue() + "}");
            }
            List<Address> addresses = entry.getValue() != null ? entry.getValue() : Collections.emptyList();
            for (int i = 0; i < addresses.size(); i++) {
                boolean isPrimary = (i == 0);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Key [" + entry.getKey() + "] Address{" + addresses.get(i) + "] isPrimary [" + isPrimary + "]");
                }
                sessions.compute(addresses.get(i), (a, l) -> {
                    if (l == null) {
                        l = new ArrayList<>();
                    }
                    SessionDataObjectInformation object = new SessionDataObjectInformation(entry.getKey().toString(), isPrimary ? NodeType.PRIMARY : NodeType.REPLICA);
                    l.add(object);
                    return l;
                });
            }
        }

        console.print(sessions);
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME);
        console.println("\t\tReturn statistics about sessions in all grid.");
    }

}
