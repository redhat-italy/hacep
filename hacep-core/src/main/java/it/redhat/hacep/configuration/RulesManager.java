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

package it.redhat.hacep.configuration;

import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import org.drools.core.util.StringUtils;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.marshalling.KieMarshallers;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class RulesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesManager.class);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final RulesConfiguration rulesConfiguration;

    private KieContainer kieContainer;
    private KieServices kieServices;

    public RulesManager(RulesConfiguration rulesConfiguration) {
        this.rulesConfiguration = rulesConfiguration;
    }

    public KieBase getKieBase() {
        checkStatus();
        if (!StringUtils.isEmpty(rulesConfiguration.getKieBaseName())) {
            return kieContainer.getKieBase(rulesConfiguration.getKieBaseName());
        }
        return kieContainer.getKieBase();
    }

    public KieSession newKieSession() {
        checkStatus();
        if (!StringUtils.isEmpty(rulesConfiguration.getKieSessionName())) {
            return kieContainer.newKieSession(rulesConfiguration.getKieSessionName());
        }
        return kieContainer.newKieSession();
    }

    public boolean updateToVersion(String version) {
        if (StringUtils.isEmpty(version)) {
            throw new IllegalArgumentException("Update to version cannot accept an empty version");
        }
        if (started.get()) {
            ReleaseId releaseId = kieServices.newReleaseId(
                    rulesConfiguration.getGroupId(),
                    rulesConfiguration.getArtifactId(),
                    version);
            Results results = kieContainer.updateToVersion(releaseId);
            for (Message result : results.getMessages()) {
                switch (result.getLevel()) {
                    case ERROR:
                        LOGGER.error(result.toString());
                        break;
                    case WARNING:
                        LOGGER.warn(result.toString());
                        break;
                    case INFO:
                        LOGGER.info(result.toString());
                        break;
                    default:
                        LOGGER.warn(result.toString());
                }
            }
            if(results.hasMessages(Message.Level.ERROR)) {
                LOGGER.error("Update to version {} aborted due to errors", version);
                return false;
            }
            LOGGER.info("Update to version {} completed", version);
            return true;
        }
        LOGGER.warn("Cannot complete update to version {} in a stopped rulemanager", version);
        return false;
    }

    public void start(String groupId, String artifactId, String version) {
        if (started.compareAndSet(false, true)) {
            kieServices = KieServices.Factory.get();
            if (!(StringUtils.isEmpty(groupId) || StringUtils.isEmpty(artifactId) || StringUtils.isEmpty(version)) &&
                    (!rulesConfiguration.getGroupId().equals(groupId) || !rulesConfiguration.getArtifactId().equals(artifactId))) {
                throw new IllegalStateException(String.format("Cannot start a Rule Manager with different Group Id and Artifact. " +
                                "Rule configuration releaseId [%s:%s:%s] cached value [%s:%s:%s]",
                        rulesConfiguration.getGroupId(), rulesConfiguration.getArtifactId(), rulesConfiguration.getVersion(),
                        groupId, artifactId, version));
            }

            ReleaseId releaseId = kieServices.newReleaseId(
                    rulesConfiguration.getGroupId(),
                    rulesConfiguration.getArtifactId(),
                    StringUtils.isEmpty(version) ? rulesConfiguration.getVersion() : version);
            kieContainer = kieServices.newKieContainer(releaseId);
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            kieContainer.dispose();
        }
    }

    private void checkStatus() {
        if (!started.get()) {
            throw new IllegalStateException("Rule manager must be started first!");
        }
    }

    public byte[] serialize(KieSession kieSession) {
        Marshaller marshaller = createSerializableMarshaller();
        return KieSessionByteArraySerializer.writeObject(marshaller, kieSession);
    }

    public KieSession deserializeOrCreate(byte[] buffer) {
        if (buffer == null) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Buffer empty, creating new KieSession");
            return newKieSession();
        }

        Marshaller marshaller = createSerializableMarshaller();
        return KieSessionByteArraySerializer.readSession(marshaller, buffer);
    }

    public void registerChannels(KieSession session) {
        rulesConfiguration.getChannels().forEach(session::registerChannel);
    }

    public void registerReplayChannels(KieSession session) {
        rulesConfiguration.getReplayChannels().forEach(session::registerChannel);
    }

    public int getMaxBufferSize() {
        try {
            return Integer.valueOf(System.getProperty("grid.buffer", "1000"));
        } catch (IllegalArgumentException e) {
            return 1000;
        }
    }

    private Marshaller createSerializableMarshaller() {
        KieServices ks = KieServices.Factory.get();
        KieMarshallers marshallers = ks.getMarshallers();
        ObjectMarshallingStrategy strategy = marshallers.newSerializeMarshallingStrategy();
        return marshallers.newMarshaller(getKieBase(), new ObjectMarshallingStrategy[]{strategy});
    }
}
