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

import it.redhat.hacep.configuration.HACEP;
import it.redhat.hacep.playground.JDGUtility;
import it.redhat.hacep.playground.console.UI;
import it.redhat.hacep.playground.console.commands.dto.HACEPNode;
import it.redhat.hacep.playground.console.commands.dto.NodeType;
import it.redhat.hacep.playground.console.commands.dto.SessionData;
import it.redhat.hacep.playground.console.support.IllegalParametersException;
import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class SessionsConsoleCommand implements ConsoleCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(SessionsConsoleCommand.class);

    private static final String COMMAND_NAME = "sessions";

    private final HACEP hacep;

    @Inject
    private JDGUtility jdgUtility;

    @Inject
    public SessionsConsoleCommand(HACEP hacep) {
        this.hacep = hacep;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start execute command 'sessions'");
        }

        Cache<String, Object> sessionCache = hacep.getSessionCache();
        Map<Address, List<SessionData>> sessions = new HashMap<>();
        hacep.getCacheManager().getMembers().forEach(a -> sessions.put(a, new ArrayList<>()));
        for (Map.Entry<String, List<Address>> entry : jdgUtility.getKeysAddresses(sessionCache).entrySet()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Key [" + entry.getKey() + "] List{" + entry.getValue() + "}");
            }
            List<Address> addresses = entry.getValue() != null ? entry.getValue() : Collections.emptyList();
            for (int i = 0; i < addresses.size(); i++) {
                boolean isPrimary = (i == 0);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Key [" + entry.getKey() + "] Address{" + addresses.get(i) + "] isPrimary [" + isPrimary + "]");
                }
                sessions.compute(addresses.get(i), (a, l) -> {
                    SessionData object = new SessionData(entry.getKey().toString(), isPrimary ? NodeType.PRIMARY : NodeType.REPLICA);
                    l.add(object);
                    return l;
                });
            }
        }

        console.print(sessions.entrySet().stream()
                .map(e -> new HACEPNode(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList()));
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME);
        console.println("\t\tReturn statistics about sessions in all grid.");
    }

}
