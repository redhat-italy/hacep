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

package it.redhat.hacep.configuration;

import it.redhat.hacep.cache.GameplayKey;
import it.redhat.hacep.console.commands.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class PlaygroundConfiguration {
    private final static Logger log = LoggerFactory.getLogger(HACEPConfiguration.class);

    private HACEPConfiguration hacepConfiguration;

    public PlaygroundConfiguration configure() {
        DroolsConfiguration droolsConfiguration = DroolsConfigurationImpl.get();
        hacepConfiguration = new HACEPConfiguration(droolsConfiguration, new GameplayKey.Builder());
        return this;
    }

    public List<ConsoleCommand> baseCommands() {
        return Arrays.asList(
                new PersistenceMetricsCommands(hacepConfiguration.getFactCache()),
                new ClearConsoleCommand(hacepConfiguration.getManager()),
                new AddressConsoleCommand(hacepConfiguration.getManager()),
                new GetConsoleCommand(hacepConfiguration.getManager()),
                new HelpConsoleCommand(),
                new InfoConsoleCommand(hacepConfiguration.getManager()),
                new AllConsoleCommand(hacepConfiguration.getManager()),
                new LocalConsoleCommand(hacepConfiguration.getManager()),
                new PrimaryConsoleCommand(hacepConfiguration.getManager()),
                new ReplicaConsoleCommand(hacepConfiguration.getManager()),
                new PutConsoleCommand(hacepConfiguration.getFactCache()),
                new QuitConsoleCommand(hacepConfiguration.getManager(), hacepConfiguration.getCamelContext()),
                new CamelContextCommand(hacepConfiguration.getCamelContext())
        );
    }

}
