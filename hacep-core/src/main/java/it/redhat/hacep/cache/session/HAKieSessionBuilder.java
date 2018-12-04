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

package it.redhat.hacep.cache.session;

import it.redhat.hacep.configuration.RulesManager;
import org.kie.api.runtime.KieSession;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class HAKieSessionBuilder {

    private final RulesManager rulesManager;
    private final Executor executor;

    public HAKieSessionBuilder(RulesManager rulesManager, Executor executor) {
        this.rulesManager = rulesManager;
        this.executor = executor;
    }

    public HAKieSession build() {
        return new HAKieSession(rulesManager, executor);
    }

    public HAKieSession build(KieSession session) {
        return new HAKieSession(rulesManager, executor, session);
    }

    public HAKieSerializedSession buildSerialized() {
        return new HAKieSerializedSession(rulesManager, executor);
    }

    public HAKieSerializedSession buildSerialized(String version, byte[] buffer) {
        return new HAKieSerializedSession(rulesManager, executor, version, buffer);
    }

    public String getVersion() {
        return rulesManager.getReleaseId().getVersion();
    }
}
