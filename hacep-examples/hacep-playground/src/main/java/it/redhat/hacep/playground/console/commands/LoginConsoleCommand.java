/**
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

import it.redhat.hacep.configuration.JmsConfiguration;
import it.redhat.hacep.playground.console.UI;
import it.redhat.hacep.playground.console.support.IllegalParametersException;
import it.redhat.hacep.playground.rules.model.LoginEvent;
import it.redhat.hacep.playground.event.send.Sender;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

public class LoginConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "login";

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        try {
            String usr = args.next();
            String pwd = args.next();
            Random rnd = new Random(System.currentTimeMillis());
            LoginEvent login = new LoginEvent(rnd.nextLong(), ZonedDateTime.now().toInstant(), usr, pwd);
            Sender sender = new Sender(jmsConfiguration.getConnectionFactory(), jmsConfiguration.getQueueName());
            sender.send(login);

        } catch (NoSuchElementException e) {
            throw new IllegalParametersException("Expected usage: login <user> <password>");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <user> <password>");
        console.println("\t\tLogin the <user>");
    }
}
