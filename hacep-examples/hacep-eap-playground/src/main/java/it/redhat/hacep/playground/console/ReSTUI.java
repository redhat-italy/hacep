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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class ReSTUI implements UI {

    private final static Logger log = LoggerFactory.getLogger(ReSTUI.class);

    @Inject
    private Instance<ConsoleCommand> commands;

    private ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
    private PrintWriter out = new PrintWriter(os);

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
        out.print(message);
    }

    @Override
    public void println() {
        out.println();
    }

    @Override
    public void printUsage() {
        log.info("Start print usage");
        StreamSupport.stream(commands.spliterator(), true)
                .sorted(new ConsoleCommandComparator())
                .forEachOrdered(c -> {
                    c.usage(this);
                    out.println();out.println();
                });
    }

    @Override
    public String toString() {
        try {
            out.flush();
            return os.toString();
        } finally {
            os.reset();
        }
    }

    @Override
    public void start() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public UI register(ConsoleCommand cmd) {
        throw new IllegalStateException();
    }

    public Optional<ConsoleCommand> findByName(String name) {
        return StreamSupport.stream(commands.spliterator(), false)
                .filter(c -> name.equalsIgnoreCase(c.command()))
                .findFirst();
    }

}
