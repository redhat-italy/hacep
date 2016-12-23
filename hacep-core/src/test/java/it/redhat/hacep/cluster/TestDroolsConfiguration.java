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

import it.redhat.hacep.configuration.AbstractBaseDroolsConfiguration;
import it.redhat.hacep.playground.rules.reward.catalog.KieAPITestUtils;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class TestDroolsConfiguration extends AbstractBaseDroolsConfiguration {

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, Channel> replayChannels = new HashMap<>();

    private int maxBufferSize = 10;

    private KieContainer kieContainer;
    private boolean upgradable = false;

    private TestDroolsConfiguration() {
    }

    public TestDroolsConfiguration(KieContainer kieContainer, boolean upgradable) {
        this.kieContainer = kieContainer;
        this.upgradable = upgradable;
    }

    public static TestDroolsConfiguration buildRulesWithGamePlayRetract() {
        try {
            ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg.gameplay.retract", "rules", "1.0.0");
            KieContainer kieContainer = KieAPITestUtils.setupKieContainerFromTemplates(releaseId, "rules/gameplay_retract.drl");
            return new TestDroolsConfiguration(kieContainer, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDroolsConfiguration buildRulesWithRetract() {
        try {
            ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg.retract", "rules", "1.0.0");
            KieContainer kieContainer = KieAPITestUtils.setupKieContainerFromTemplates(releaseId, "rules/complex-retract-rule.drl");
            return new TestDroolsConfiguration(kieContainer, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDroolsConfiguration buildV1() {
        try {
            ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg", "rules-update", "1.0.0");
            KieContainer kieContainer = KieAPITestUtils.setupKieContainerFromTemplates(releaseId, "rules/simple-rule.drl");
            return new TestDroolsConfiguration(kieContainer, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void upgradeToV2() throws IOException, URISyntaxException {
        if (!upgradable) {
            throw new IllegalStateException();
        }
        ReleaseId releaseId = KieServices.Factory.get().newReleaseId("it.redhat.jdg", "rules-update", "2.0.0");
        KieAPITestUtils.setupKieContainerFromTemplates(releaseId, "rules/simple-rule_modified.drl");
        KieAPITestUtils.hasErrors(kieContainer.updateToVersion(releaseId));

        Results results = kieContainer.verify();
        results.getMessages().forEach(m -> System.out.println(m.toString()));
        KieAPITestUtils.hasErrors(results);
    }

    public void dispose() {
        KieAPITestUtils.cleanUp();
        kieContainer = null;
    }

    public void registerChannel(String name, Channel channel, Channel replay) {
        channels.put(name, channel);
        replayChannels.put(name, replay);
    }

    @Override
    public KieSession newKieSession() {
        return kieContainer.newKieSession("ksession-test");
    }

    @Override
    public KieBase getKieBase() {
        return kieContainer.getKieBase("kbase-test");
    }

    @Override
    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public Map<String, Channel> getReplayChannels() {
        return replayChannels;
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    @Override
    public int getMaxBufferSize() {
        return maxBufferSize;
    }
}
