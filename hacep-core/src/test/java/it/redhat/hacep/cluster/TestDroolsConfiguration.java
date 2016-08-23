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

import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.playground.rules.reward.catalog.KieAPITestUtils;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.kie.api.KieBase;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.HashMap;
import java.util.Map;

public class TestDroolsConfiguration implements DroolsConfiguration {

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, Channel> replayChannels = new HashMap<>();
    private KieBase kieBase;
    private int maxBufferSize;
    private final KieContainer kieContainer;

    public TestDroolsConfiguration() {
        try {
            ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "2.0.0");
            kieContainer = KieAPITestUtils.setupKieContainer(releaseId, "pom/pom-2.0.0.xml", "rules/simple-rule.drl");
            kieBase = kieContainer.getKieBase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void upgradeContainer() {
        ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "2.0.0");
        KieAPITestUtils.setupKieContainer(releaseId, "pom/pom-2.0.0.xml", "rules/simple-rule_modified.drl");
        kieContainer.updateToVersion(releaseId);
    }

    public void registerChannel(String name, Channel channel, Channel replay) {
        channels.put(name, channel);
        replayChannels.put(name, replay);
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    @Override
    public KieSession getKieSession() {
        try {
            return KieAPITestUtils.buildKieSession(kieBase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return maxBufferSize;
    }

}
