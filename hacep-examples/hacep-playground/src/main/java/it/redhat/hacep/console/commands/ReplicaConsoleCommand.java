/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.redhat.hacep.console.commands;

import it.redhat.hacep.JDG;
import it.redhat.hacep.configuration.HACEPApplication;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.console.UI;
import it.redhat.hacep.console.support.IllegalParametersException;
import org.infinispan.Cache;

import java.util.Iterator;
import java.util.Set;

public class ReplicaConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "replica";

    private final HACEPApplication application;

    public ReplicaConsoleCommand(HACEPApplication application) {
        this.application = application;
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
            Set<String> replicas = JDG.replicaValuesFromKeys(cache);
            replicas.forEach(console::println);
            console.println("Cache Size: " + cache.size() + "\n");
            console.println("Replica Size: " + replicas.size() + "\n");
        } else {
            console.println("Cache " + cacheName + " not existent");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <cache>");
        console.println("\t\tList all local values for which this node is a replica.");
    }
}
