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
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.io.impl.ClassPathResource;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.scanner.MavenRepository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class TestDroolsConfiguration extends AbstractBaseDroolsConfiguration {

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, Channel> replayChannels = new HashMap<>();
    private KieBase kieBase;
    private int maxBufferSize = 10;
    private KieContainer kieContainer;
    private boolean upgradable = false;

    private TestDroolsConfiguration() {
    }

    public static TestDroolsConfiguration buildRulesWithGamePlayRetract() {
        try {
            TestDroolsConfiguration droolsConfiguration = new TestDroolsConfiguration();
            ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "1.0.0");
            droolsConfiguration.kieContainer = KieAPITestUtils.setupKieContainer(releaseId, "pom/pom-1.0.0.xml", "rules/gameplay_retract.drl");
            droolsConfiguration.kieBase = droolsConfiguration.kieContainer.getKieBase();
            return droolsConfiguration;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDroolsConfiguration buildRulesWithRetract() {
        try {
            TestDroolsConfiguration droolsConfiguration = new TestDroolsConfiguration();
            ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "1.0.0");
            droolsConfiguration.kieContainer = KieAPITestUtils.setupKieContainer(releaseId, "pom/pom-1.0.0.xml", "rules/complex-retract-rule.drl");
            droolsConfiguration.kieBase = droolsConfiguration.kieContainer.getKieBase();
            return droolsConfiguration;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDroolsConfiguration buildV1() {
        try {

            ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "1.0.0");
            KieModule kieModule = KieAPITestUtils.createKieModule(releaseId, "pom/pom-1.0.0.xml", "rules/simple-rule.drl");
            ClassPathResource resource = new ClassPathResource("pom/pom-1.0.0.xml");
            File file = new File(resource.getURL().toURI());

            MavenRepository.getMavenRepository().installArtifact(releaseId, (InternalKieModule) kieModule, file);

            KieServices ks = KieServices.Factory.get();

            TestDroolsConfiguration droolsConfiguration = new TestDroolsConfiguration();
            droolsConfiguration.upgradable = true;
            droolsConfiguration.kieContainer = ks.newKieContainer(releaseId);
            droolsConfiguration.kieBase = droolsConfiguration.kieContainer.getKieBase();
            return droolsConfiguration;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void upgradeToV2() throws IOException, URISyntaxException {
        if (!upgradable) {
            throw new IllegalStateException();
        }

        ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "2.0.0");
        KieModule kieModule = KieAPITestUtils.createKieModule(releaseId, "pom/pom-2.0.0.xml", "rules/simple-rule_modified.drl");
        ClassPathResource resource = new ClassPathResource("pom/pom-2.0.0.xml");
        File file = new File(resource.getURL().toURI());

        MavenRepository.getMavenRepository().installArtifact(releaseId, (InternalKieModule) kieModule, file);

        ReleaseId oldReleaseId = kieContainer.getReleaseId();

        Results results = kieContainer.updateToVersion(releaseId);
        KieAPITestUtils.hasErrors(results);

        kieBase = kieContainer.getKieBase();
    }

    public void registerChannel(String name, Channel channel, Channel replay) {
        channels.put(name, channel);
        replayChannels.put(name, replay);
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    @Override
    public KieSession newKieSession() {
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
