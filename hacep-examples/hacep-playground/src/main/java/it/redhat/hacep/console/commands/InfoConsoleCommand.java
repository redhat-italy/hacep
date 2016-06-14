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

import it.redhat.hacep.configuration.HACEPApplication;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.console.UI;
import it.redhat.hacep.console.support.IllegalParametersException;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class InfoConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "info";

    private final HACEPApplication application;

    public InfoConsoleCommand(HACEPApplication application) {
        this.application = application;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {

        try {
            String cacheName = args.next();
            Cache<Key, Object> cache = application.getCacheManager().getCache(cacheName, false);

            if (cache != null) {
                console.println(buildInfo(cache));
            } else {
                console.println("Cache " + cacheName + " not existent");
            }

        } catch (NoSuchElementException e) {
            console.println(generalInfo());
        }
        return true;

    }

    private String generalInfo() {
        DefaultCacheManager cacheManager = application.getCacheManager();
        StringBuilder info = new StringBuilder();
        info.append("Cache Manager Status: ").append(cacheManager.getStatus()).append("\n");
        info.append("Cache Manager Address: ").append(cacheManager.getAddress()).append("\n");
        info.append("Coordinator address: ").append(cacheManager.getCoordinator()).append("\n");
        info.append("Is Coordinator: ").append(cacheManager.isCoordinator()).append("\n");
        info.append("Cluster Name: ").append(cacheManager.getClusterName()).append("\n");
        info.append("Cluster Size: ").append(cacheManager.getClusterSize()).append("\n");
        info.append("Member list: ").append(cacheManager.getMembers()).append("\n");
        info.append("Caches: ").append(cacheManager.getCacheNames()).append("\n");
        return info.toString();
    }

    private String buildInfo(Cache<Key, Object> cache) {

        StringBuilder info = new StringBuilder();

        info.append("Cache: ").append(cache).append("\n");
        info.append("Cache Mode: ").append(cache.getCacheConfiguration().clustering().cacheModeString()).append("\n");
        info.append("Cache Size: ").append(cache.size()).append("\n");
        info.append("Cache Status: ").append(cache.getStatus()).append("\n");
        info.append("Number of Owners: ").append(cache.getAdvancedCache().getDistributionManager().getConsistentHash().getNumOwners()).append("\n");
        info.append("Number of Segments: ").append(cache.getAdvancedCache().getDistributionManager().getConsistentHash().getNumSegments()).append("\n");

        return info.toString();
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " [<cache>]");
        console.println("\t\tGeneral information or specific information on cache.");
    }

}
