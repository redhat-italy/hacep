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

package it.redhat.hacep.cluster;

import it.redhat.hacep.configuration.RulesConfiguration;
import it.redhat.hacep.playground.rules.reward.catalog.KieAPITestUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.Channel;

import java.util.HashMap;
import java.util.Map;

public class RulesConfigurationTestImpl implements RulesConfiguration {

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, Channel> replayChannels = new HashMap<>();

    private final String kieSessionName;
    private final String kieBaseName;
    private final String version;
    private final String artifactId;
    private final String groupId;

    public RulesConfigurationTestImpl(String kieBaseName, String kieSessionName,
                                      String groupId, String artifactId, String version) {
        this.kieSessionName = kieSessionName;
        this.kieBaseName = kieBaseName;
        this.version = version;
        this.artifactId = artifactId;
        this.groupId = groupId;
    }

    @Override
    public String getKieSessionName() {
        return kieSessionName;
    }

    @Override
    public String getKieBaseName() {
        return kieBaseName;
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
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void registerChannel(String name, Channel channel, Channel replayChannel) {
        channels.put(name, channel);
        replayChannels.put(name, replayChannel);
    }

    public static class RulesTestBuilder {

        public static RulesConfigurationTestImpl buildRulesWithGamePlayRetract() {
            try {
                ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg.gameplay.retract", "rules", "1.0.0");
                KieAPITestUtils.buildReleaseFromTemplates(releaseId, "rules/gameplay_retract.drl");
                return new RulesConfigurationTestImpl("kbase-test", "ksession-test", "it.redhat.jdg.gameplay.retract", "rules", "1.0.0");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static RulesConfigurationTestImpl buildRulesWithRetract() {
            try {
                ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg.retract", "rules", "1.0.0");
                KieAPITestUtils.buildReleaseFromTemplates(releaseId, "rules/complex-retract-rule.drl");
                return new RulesConfigurationTestImpl("kbase-test", "ksession-test", "it.redhat.jdg.retract", "rules", "1.0.0");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static RulesConfigurationTestImpl buildV1() {
            try {
                ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg", "rules-update", "1.0.0");
                KieAPITestUtils.buildReleaseFromTemplates(releaseId, "rules/simple-rule.drl");
                return new RulesConfigurationTestImpl("kbase-test", "ksession-test", "it.redhat.jdg", "rules-update", "1.0.0");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static String buildV2() {
            try {
                String version = "2.0.0";
                ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg", "rules-update", version);
                KieAPITestUtils.buildReleaseFromTemplates(releaseId, "rules/simple-rule_modified.drl");
                return version;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
