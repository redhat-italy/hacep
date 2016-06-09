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

import it.redhat.hacep.drools.channels.AuditChannel;
import it.redhat.hacep.drools.channels.NullChannel;
import it.redhat.hacep.drools.channels.SysoutChannel;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DroolsConfigurationImpl implements DroolsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DroolsConfigurationImpl.class);
    private static final DroolsConfiguration INSTANCE = new DroolsConfigurationImpl();

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final Map<String, Channel> replayChannels = new ConcurrentHashMap<>();

    private final KieContainer kieContainer;
    private final KieBase kieBase;

    private static final String KSESSION_RULES = "hacep-sessions";
    private static final String KBASE_NAME = "hacep-rules";

    private DroolsConfigurationImpl() {
        KieServices kieServices = KieServices.Factory.get();
        kieContainer = kieServices.getKieClasspathContainer();
        kieBase = kieContainer.getKieBase(KBASE_NAME);

        channels.put(SysoutChannel.CHANNEL_ID, new SysoutChannel());
        channels.put(AuditChannel.CHANNEL_ID, new AuditChannel());

        replayChannels.put(SysoutChannel.CHANNEL_ID, new NullChannel());
        replayChannels.put(AuditChannel.CHANNEL_ID, new NullChannel());

        logger.info("[Kie Container] successfully initialized.");
    }

    @Override
    public KieSession getKieSession() {
        return kieContainer.newKieSession(KSESSION_RULES);
    }

    @Override
    public KieBase getKieBase() {
        return kieBase;
    }

    @Override
    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public Map<String, Channel> getReplayChannels() {
        return replayChannels;
    }

    @Override
    public int getMaxBufferSize() {
        try {
            return Integer.valueOf(System.getProperty("grid.buffer", "10"));
        } catch (IllegalArgumentException e) {
            return 10;
        }
    }

    public static DroolsConfiguration get() {
        return INSTANCE;
    }
}