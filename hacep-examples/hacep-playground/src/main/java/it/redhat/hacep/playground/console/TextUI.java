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

package it.redhat.hacep.playground.console;

import it.redhat.hacep.playground.console.commands.ConsoleCommand;
import it.redhat.hacep.playground.console.support.ConsoleCommandComparator;
import it.redhat.hacep.playground.console.support.IllegalParametersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class TextUI implements UI {

    private final static Logger log = LoggerFactory.getLogger(TextUI.class);

    private final Map<Pattern, ConsoleCommand> commands = new HashMap<Pattern, ConsoleCommand>();

    private final BufferedReader in;
    private final PrintStream out;

    public TextUI(InputStream in, PrintStream out) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
    }


    @Override
    public void start() throws IOException {
        boolean keepRunning = true;
        while (keepRunning) {
            out.print("> ");
            out.flush();
            String line = in.readLine();
            if (line == null) {
                break;
            }
            keepRunning = processLine(line);
        }
    }


    private boolean processLine(String line) {
        Scanner scanner = new Scanner(line);
        try {
            String name = scanner.next();
            for (Pattern key : commands.keySet()) {
                if (key.matcher(name).matches()) {
                    ConsoleCommand command = commands.get(key);
                    return command.execute(this, scanner);
                }
            }
        } catch (NoSuchElementException e) {
            out.println("> ");
        } catch (IllegalParametersException e) {
            println(e.getMessage());
        }
        return true;
    }

    public Map<Pattern, ConsoleCommand> getCommands() {
        return commands;
    }

    @Override
    public void print(Object message) {
        out.print(message);
    }

    @Override
    public void println(Object message) {
        out.println(message);
    }

    @Override
    public void print(String message) {
        out.print(message);
    }

    @Override
    public void println(String message) {
        out.println(message);
    }

    @Override
    public void println() {
        out.println();
    }

    @Override
    public void printUsage() {
        TreeSet<ConsoleCommand> orderedSet = new TreeSet<ConsoleCommand>(new ConsoleCommandComparator());
        orderedSet.addAll(commands.values());

        out.println("Commands:");
        for (ConsoleCommand command : orderedSet) {
            command.usage(this);
        }
    }

    @Override
    public UI register(ConsoleCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("Command argument cannot be null");
        }
        String name = cmd.command();
        commands.put(Pattern.compile(name), cmd);
        return this;
    }
}