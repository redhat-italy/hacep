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

import it.redhat.hacep.playground.JDGUtility;
import it.redhat.hacep.configuration.HACEP;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.playground.console.UI;
import it.redhat.hacep.playground.console.support.IllegalParametersException;
import org.infinispan.Cache;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;

public class PrimaryConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "primary";
    private final HACEP application;
    private final JDGUtility jdgUtility;

    @Inject
    public PrimaryConsoleCommand(HACEP application, JDGUtility jdgUtility) {
        this.application = application;
        this.jdgUtility = jdgUtility;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {

        String cacheName = args.next();
        Cache<Key, Object> cache = application.getCacheManager().getCache(cacheName, false);

        if (cache != null) {
            Set<String> primaryVals = jdgUtility.primaryValuesFromKeys(cache);
            primaryVals.forEach(console::println);
            console.println("Cache Size: " + cache.size() + "\n");
            console.println("Primary Size: " + primaryVals.size() + "\n");
        } else {
            console.println("Cache " + cacheName + " not existent");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <cache>");
        console.println("\t\tList all local valuesFromKeys for which this node is primary.");
    }
}
