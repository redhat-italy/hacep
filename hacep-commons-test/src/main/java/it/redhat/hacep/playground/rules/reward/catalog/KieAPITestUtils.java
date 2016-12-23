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

package it.redhat.hacep.playground.rules.reward.catalog;

import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.io.impl.ReaderResource;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class KieAPITestUtils {

    private final static List<ReleaseId> releases = new ArrayList<>();
    private final static List<KieContainer> kieContainers = new ArrayList<>();

    private final static KieServices ks = KieServices.Factory.get();

    private final static String kmoduleTemplate = "<kmodule xmlns=\"http://jboss.org/kie/6.0.0/kmodule\">\n" +
            "    <kbase name=\"kbase-test\" default=\"true\" equalsBehavior=\"equality\" eventProcessingMode=\"stream\">\n" +
            "        <ksession name=\"ksession-test\" default=\"true\" type=\"stateful\" clockType=\"pseudo\"/>\n" +
            "    </kbase>\n" +
            "</kmodule>";

    private final static String pomTemplate = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>%s</groupId>\n" +
            "  <artifactId>%s</artifactId>\n" +
            "  <version>%s</version>\n" +
            "</project>";

    public static KieSession buildKieSession(KieBase kieBase) {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        KieSession session = kieBase.newKieSession(sessionConf, null);
        return session;
    }

    public static KieBase setupKieBase(String... resources) throws Exception {
        ReleaseId releaseId = ks.newReleaseId("it.redhat.jdg", "rules", "1.0.0");
        KieContainer kieContainer = setupKieContainerFromTemplates(releaseId, resources);

        KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);

        return kieContainer.getKieBase();
    }

    public static KieContainer setupKieContainerFromTemplates(ReleaseId releaseId, String... resources) {
        String pomFile = String.format(pomTemplate, releaseId.getGroupId(), releaseId.getArtifactId(), releaseId.getVersion());
        ReaderResource pomResource = new ReaderResource(new StringReader(pomFile));
        ReaderResource kmoduleResource = new ReaderResource(new StringReader(kmoduleTemplate));
        return setupKieContainer(releaseId, pomResource, kmoduleResource, resources);
    }

    public static void buildReleaseFromTemplates(ReleaseId releaseId, String... resources) {
        String pomFile = String.format(pomTemplate, releaseId.getGroupId(), releaseId.getArtifactId(), releaseId.getVersion());
        ReaderResource pomResource = new ReaderResource(new StringReader(pomFile));
        ReaderResource kmoduleResource = new ReaderResource(new StringReader(kmoduleTemplate));
        buildRelease(releaseId, pomResource, kmoduleResource, resources);
    }

    private static KieContainer setupKieContainer(ReleaseId releaseId, Resource pomResource, Resource kmodule, String... resources) {
        KieServices kieServices = KieServices.Factory.get();

        buildRelease(releaseId, pomResource, kmodule, resources);

        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        kieContainers.add(kieContainer);

        return kieContainer;
    }

    private static void buildRelease(ReleaseId releaseId, Resource pomResource, Resource kmodule, String... resources) {
        if (releases.contains(releaseId)) {
            ks.getRepository().removeKieModule(releaseId);
        }
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kfs = kieServices.newKieFileSystem();

        kfs.write("pom.xml", pomResource);

        kfs.write("src/main/resources/META-INF/kmodule.xml", kmodule);

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        hasErrors(kieServices.newKieBuilder(kfs).buildAll().getResults());

        releases.add(releaseId);
    }

    public static void cleanUp() {
        kieContainers.forEach(KieContainer::dispose);
        kieContainers.clear();
        releases.forEach(ks.getRepository()::removeKieModule);
        releases.clear();
    }

    public static void hasErrors(Results results) {
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + results.toString());
        }
    }
}
