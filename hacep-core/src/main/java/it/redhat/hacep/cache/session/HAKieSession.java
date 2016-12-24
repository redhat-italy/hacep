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

package it.redhat.hacep.cache.session;

import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.support.KieSessionUtils;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.infinispan.commons.util.Util.asSet;

public class HAKieSession implements DeltaAware {

    private final static Logger LOGGER = LoggerFactory.getLogger(HAKieSession.class);

    private final RulesManager droolsConfiguration;
    private final Executor executor;

    private Fact lastFact;
    private KieSession session;

    public HAKieSession(RulesManager droolsConfiguration, Executor executor) {
        this.droolsConfiguration = droolsConfiguration;
        this.executor = executor;
    }

    public HAKieSession(RulesManager droolsConfiguration, Executor executor, KieSession session) {
        this(droolsConfiguration, executor);
        this.session = session;
    }

    public final HAKieSerializedSession wrapWithSerializedSession() {
        if (session != null) {
            return new HAKieSerializedSession(droolsConfiguration, executor, this.toByteArray());
        }
        return new HAKieSerializedSession(droolsConfiguration, executor);
    }

    public void insert(Fact fact) {
        if (session == null) {
            session = droolsConfiguration.newKieSession();
            droolsConfiguration.registerChannels(session);
        }
        lastFact = fact;
        KieSessionUtils.advanceClock(session, fact);
        session.insert(fact);
        session.fireAllRules();
    }

    private byte[] toByteArray() {
        return droolsConfiguration.serialize(session);
    }

    @Override
    public Delta delta() {
        if (lastFact != null) {
            return new HAKieSessionDeltaFact(lastFact);
        }
        return new HAKieSessionDeltaEmpty();
    }

    @Override
    public void commit() {
        lastFact = null;
    }

    @Override
    protected void finalize() throws Throwable {
        this.dispose();
        super.finalize();
    }

    public void dispose() {
        KieSessionUtils.dispose(session);
        session = null;
    }

    public boolean isSerialized() {
        return false;
    }

    public HAKieSession rebuild() {
        throw new IllegalStateException("Cannot rebuild an HAKieSession");
    }

    public static class HASessionExternalizer implements AdvancedExternalizer<HAKieSession> {

        private final HAKieSessionBuilder builder;

        public HASessionExternalizer(HAKieSessionBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Set<Class<? extends HAKieSession>> getTypeClasses() {
            return asSet(HAKieSession.class);
        }

        @Override
        public Integer getId() {
            return JDGExternalizerIDs.HASessionID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HAKieSession object) throws IOException {
            if (object.session != null) {
                byte[] buffer = object.toByteArray();
                output.writeInt(buffer.length);
                output.write(buffer);
            } else {
                output.writeInt(0);
            }
        }
        @Override
        public HAKieSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            int len = input.readInt();
            if (len > 0) {
                byte[] buffer = new byte[len];
                input.read(buffer);
                return builder.buildSerialized(buffer);
            } else {
                return builder.buildSerialized();
            }
        }

    }
}
