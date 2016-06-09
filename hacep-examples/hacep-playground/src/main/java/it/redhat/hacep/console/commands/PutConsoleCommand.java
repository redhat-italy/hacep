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
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.rules.model.Gameplay;
import it.redhat.hacep.rules.model.util.GameplayBuilder;
import org.infinispan.Cache;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PutConsoleCommand implements ConsoleCommand {

    private static final String COMMAND_NAME = "put";
    private final Cache<Key, Fact> cache;

    public PutConsoleCommand(Cache<Key, Fact> cache) {
        this.cache = cache;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {
        try {
            Long id = Long.parseLong(args.next());
            Long player = Long.parseLong(args.next());

            Gameplay g = new GameplayBuilder().playerId(player).timestamp(System.currentTimeMillis()).build();
            g.setId(id);


            cache.put(new GameplayKey(id.toString(), player.toString()), g);

            console.println("Written (" + id + "," + g + ")");
        } catch (NumberFormatException e) {
            throw new IllegalParametersException("Expected usage: put <key> <player>\nValue for key has to be a number. Example:\n put 10 2 test");
        } catch (NoSuchElementException e) {
            throw new IllegalParametersException("Expected usage: put <key> <player>");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <key> <player>");
        console.println("\t\tPut a fact (key:player) in the Facts cache.");
    }
}
