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

import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

import java.util.ArrayList;
import java.util.List;

public class KieBaseContainerTest {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String SIMPLE_DATA_FORMAT_GLOBAL_NAME = "sdf";
    private List<String> rules = new ArrayList<>();


    public KieSession newKieSession(KieBase kieBase) throws Exception {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        KieSession session = kieBase.newKieSession(sessionConf, null);
        return session;
    }

    public KieBase setupKieBase(String... resources) throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieBaseConfiguration config = ks.newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        KieFileSystem kfs = ks.newKieFileSystem();
        KieRepository kr = ks.getRepository();

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb);

        KieContainer kc = ks.newKieContainer(kr.getDefaultReleaseId());

        return kc.newKieBase(config);
    }

    private static void hasErrors(KieBuilder kbuilder) throws Exception {
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + kbuilder.getResults().toString());
        }
    }

}
