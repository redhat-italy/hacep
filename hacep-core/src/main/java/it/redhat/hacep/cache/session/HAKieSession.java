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

import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import org.drools.core.time.SessionPseudoClock;
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
import java.util.concurrent.TimeUnit;

import static org.infinispan.commons.util.Util.asSet;

public class HAKieSession implements DeltaAware {

    private final static Logger LOGGER = LoggerFactory.getLogger(HAKieSession.class);

    private final DroolsConfiguration droolsConfiguration;
    private final KieSessionByteArraySerializer serializer;
    private final Executor executor;

    private Fact lastFact;
    private KieSession session;

    public HAKieSession(DroolsConfiguration droolsConfiguration, KieSessionByteArraySerializer serializer, Executor executor) {
        this.droolsConfiguration = droolsConfiguration;
        this.serializer = serializer;
        this.executor = executor;
    }

    public HAKieSession(DroolsConfiguration droolsConfiguration, KieSessionByteArraySerializer serializer, Executor executor, KieSession session) {
        this(droolsConfiguration, serializer, executor);
        this.session = session;
    }

    public final HAKieSerializedSession wrapWithSerializedSession() {
        if (session != null) {
            return new HAKieSerializedSession(droolsConfiguration, serializer, executor, serializer.writeObject(session));
        }
        return new HAKieSerializedSession(droolsConfiguration, serializer, executor);
    }

    public void insert(Fact fact) {
        if (session == null) {
            session = droolsConfiguration.getKieSession();
            droolsConfiguration.getChannels().forEach(session::registerChannel);
        }
        lastFact = fact;
        advanceClock(session, fact);
        session.insert(fact);
        session.fireAllRules();
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
        if (session != null) {
            dispose(session);
            session = null;
        }
        super.finalize();
    }

    static void advanceClock(KieSession kieSession, Fact fact) {
        SessionPseudoClock clock = kieSession.getSessionClock();
        long gts = fact.getInstant().toEpochMilli();
        long current = clock.getCurrentTime();
        if (gts < current) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format("Moving clock backwards. New Clock is [%s], current was [%s]", gts, current));
            }
        }
        clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
    }

    static void dispose(KieSession localSession) {
        if (localSession != null) {
            try {
                localSession.dispose();
            } catch (Exception e) {
                LOGGER.error("Unexpected exception", e);
            }
        }
    }

    public static class HASessionExternalizer implements AdvancedExternalizer<HAKieSession> {

        private final DroolsConfiguration droolsConfiguration;
        private final KieSessionByteArraySerializer serializer;
        private final Executor executor;

        public HASessionExternalizer(DroolsConfiguration droolsConfiguration, KieSessionByteArraySerializer serializer,
                                     Executor executor) {
            this.droolsConfiguration = droolsConfiguration;
            this.serializer = serializer;
            this.executor = executor;
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
                byte[] buffer = serializer.writeObject(object.session);
                System.out.println("Advanced externalizer write buffer [" + buffer.length + "]");
                output.writeInt(buffer.length);
                output.write(buffer);
            } else {
                output.writeInt(0);
            }
        }

        @Override
        public HAKieSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            int len = input.readInt();
            System.out.println("Advanced externalizer read buffer [" + len + "]");
            if (len > 0) {
                byte[] buffer = new byte[len];
                int read = input.read(buffer);
                System.out.println("Bytes Read [" + read + " of " + len + "]");
                return new HAKieSerializedSession(droolsConfiguration, serializer, executor, buffer);
            } else {
                return new HAKieSerializedSession(droolsConfiguration, serializer, executor);
            }
        }
    }
}
