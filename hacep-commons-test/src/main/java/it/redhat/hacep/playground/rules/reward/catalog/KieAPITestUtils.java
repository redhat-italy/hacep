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

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

public class KieAPITestUtils {

    public static KieSession buildKieSession(KieBase kieBase) {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        KieSession session = kieBase.newKieSession(sessionConf, null);
        return session;
    }


    public static KieBase setupKieBase(String... resources) throws Exception {
        ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "1.0.0");
        KieContainer kieContainer = setupKieContainer(releaseId, "pom/pom-1.0.0.xml", resources);

        KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);

        return kieContainer.getKieBase();
    }

    public static KieContainer setupKieContainer(ReleaseId releaseId, String pom, String... resources) {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kfs = kieServices.newKieFileSystem();

        Resource pomResource = new ClassPathResource(pom);
        kfs.write("pom.xml", pomResource);

        Resource kmodule = new ClassPathResource("kmodule/kmodule.xml");
        kfs.write("src/main/resources/META-INF/kmodule.xml", kmodule);

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        KieBuilder kb = kieServices.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb.getResults());

        KieContainer kc = kieServices.newKieContainer(releaseId);

        return kc;
    }

    public static KieModule createKieModule(ReleaseId releaseId, String pom, String... resources) {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kfs = kieServices.newKieFileSystem();

        Resource pomResource = new ClassPathResource(pom);
        kfs.write("pom.xml", pomResource);

        Resource kmodule = new ClassPathResource("kmodule/kmodule.xml");
        kfs.write("src/main/resources/META-INF/kmodule.xml", kmodule);

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        KieBuilder kb = kieServices.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb.getResults());

        return kb.getKieModule();
    }

    public static void hasErrors(Results results) {
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + results.toString());
        }
    }
}
