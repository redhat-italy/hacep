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
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.marshalling.KieMarshallers;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractBaseDroolsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseDroolsConfiguration.class);

    public abstract KieSession newKieSession();

    public abstract KieBase getKieBase();

    public abstract Map<String, Channel> getChannels();

    public abstract Map<String, Channel> getReplayChannels();

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
        this.getChannels().forEach(session::registerChannel);
    }

    public void registerReplayChannels(KieSession session) {
        this.getReplayChannels().forEach(session::registerChannel);
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
