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

import it.redhat.hacep.cache.GameplayKey;
import it.redhat.hacep.console.UI;
import it.redhat.hacep.console.support.IllegalParametersException;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class GetConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "get";
    private final DefaultCacheManager cacheManager;

    public GetConsoleCommand(DefaultCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        try {
            String cacheName = args.next();
            Cache<Key, Object> cache = cacheManager.getCache(cacheName, false);

            if (cache != null) {
                String id = args.next();
                String player = args.next();
                Key k = new GameplayKey(id, player);

                Object o = cache.get(k);

                if (o != null) {
                    console.println(o);
                } else {
                    console.println("Not found");
                }
            } else {
                console.println("Cache " + cacheName + " not existent");
            }
        } catch (NumberFormatException e) {
            throw new IllegalParametersException("Expected usage: get <cache> <key> <player>\nValue for key has to be a number. Example:\n get 10 2");
        } catch (NoSuchElementException e) {
            throw new IllegalParametersException("Expected usage: get cache> <key> <player>");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <cache> <key> <player>");
        console.println("\t\tGet an object from the <cache>.");
    }
}
