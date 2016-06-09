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

package it.redhat.hacep.console.commands;

import it.redhat.hacep.console.UI;
import it.redhat.hacep.console.support.IllegalParametersException;
import org.apache.camel.CamelContext;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CamelContextCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "camel";
    private static final String SUBCOMMAND_START_NAME = "start";
    private static final String SUBCOMMAND_STOP_NAME = "stop";

    private final CamelContext context;

    public CamelContextCommand(CamelContext context) {
        this.context = context;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        try {
            String subCommand = args.next();
            switch (subCommand) {
                case SUBCOMMAND_START_NAME:
                    try {
                        context.start();
                    } catch (Exception e) {
                        console.println("Can't stop camel route");
                    }
                    break;
                case SUBCOMMAND_STOP_NAME:
                    try {
                        context.stop();
                    } catch (Exception e) {
                        console.println("Can't stop camel route");
                    }
                    break;
            }

        } catch (NoSuchElementException e) {
            throw new IllegalParametersException("Expected usage: camelcontext start|stop");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " start");
        console.println("\t\tStarts the camel context.");
        console.println(COMMAND_NAME + " stop");
        console.println("\t\tStops the camel context.");
    }
}
