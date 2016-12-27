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

package it.redhat.hacep.playground.configuration;

import it.redhat.hacep.configuration.RulesConfiguration;
import it.redhat.hacep.drools.channels.NullChannel;
import it.redhat.hacep.playground.drools.channels.AuditChannel;
import it.redhat.hacep.playground.drools.channels.PlayerPointLevelChannel;
import it.redhat.hacep.playground.drools.channels.SysoutChannel;
import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RulesConfigurationImpl implements RulesConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesConfigurationImpl.class);

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final Map<String, Channel> replayChannels = new ConcurrentHashMap<>();

    private static final String KSESSION_RULES = "hacep-sessions";
    private static final String KBASE_NAME = "hacep-rules";

    @Inject
    private SysoutChannel sysoutChannel;
    @Inject
    private AuditChannel auditChannel;
    @Inject
    private PlayerPointLevelChannel playerPointLevelChannel;

    @PostConstruct
    public void registerChannels() {
        channels.put(SysoutChannel.CHANNEL_ID, sysoutChannel);
        channels.put(AuditChannel.CHANNEL_ID, auditChannel);
        channels.put(PlayerPointLevelChannel.CHANNEL_ID, playerPointLevelChannel);

        replayChannels.put(SysoutChannel.CHANNEL_ID, new NullChannel());
        replayChannels.put(AuditChannel.CHANNEL_ID, new NullChannel());
        replayChannels.put(PlayerPointLevelChannel.CHANNEL_ID, new NullChannel());
    }

    @Override
    public String getKieSessionName() {
        return KSESSION_RULES;
    }

    @Override
    public String getKieBaseName() {
        return KBASE_NAME;
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
    public String getGroupId() {
        return "it.redhat.jdg.examples";
    }

    @Override
    public String getArtifactId() {
        return "hacep-rules";
    }

    @Override
    public String getVersion() {
        return "1.0-SNAPSHOT";
    }

}