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

import it.redhat.hacep.cache.session.utils.SessionUtils;
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static it.redhat.hacep.cache.session.JDGExternalizerIDs.HASerializerSessionID;
import static org.infinispan.commons.util.Util.asSet;

public class HASerializedSession implements DeltaAware {

    private final static Logger logger = LoggerFactory.getLogger(HASerializedSession.class);

    private final KieSessionByteArraySerializer serializer;
    private final Executor executor;
    private final DroolsConfiguration droolsConfiguration;

    private CountDownLatch latch = new CountDownLatch(0);

    private byte[] session;
    private transient long size = 0;
    private Queue<Fact> buffer = new ConcurrentLinkedQueue<>();

    public HASerializedSession(DroolsConfiguration droolsConfiguration, KieSessionByteArraySerializer serializer, Executor executor) {
        this.serializer = serializer;
        this.droolsConfiguration = droolsConfiguration;
        this.executor = executor;
    }


    public void add(Fact f) {
        buffer.offer(f);
        size++;
        if (this.needToSave()) {
            this.createSafePoint();
        }
    }

    public HASession rebuild() {
        this.waitSafePoint();
        return new HASession(droolsConfiguration, buildSession());
    }

    private void waitSafePoint() {
        if (isSaving()) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean needToSave() {
        return (!isSaving() && (size > droolsConfiguration.getMaxBufferSize()));
    }

    private void createSafePoint() {
        if (isSaving()) {
            throw new IllegalStateException("slim session already active");
        }
        latch = new CountDownLatch(1);
        executor.execute(() -> {
            KieSession localSession = null;
            try {
                printSessionSize("Start consuming buffer");
                localSession = buildSession();
                session = serializer.writeObject(localSession);
                printSessionSize("Buffer empty");
            } catch (Exception e) {
                logger.error("Unexpected exception", e);
            } finally {
                SessionUtils.dispose(localSession);
            }
            latch.countDown();
        });
    }

    private void printSessionSize(String message) {
        if (logger.isDebugEnabled()) {
            int sessionSize = this.session != null ? this.session.length : 0;
            logger.debug(message + " - Size [" + sessionSize + "] - Buffer [" + size + "]");
        }
    }

    private KieSession buildSession() {
        KieSession localSession;
        if (session != null) {
            localSession = serializer.readSession(this.session);
        } else {
            localSession = droolsConfiguration.getKieSession();
        }
        droolsConfiguration.getReplayChannels().forEach(localSession::registerChannel);
        while (!buffer.isEmpty()) {
            Fact fact = buffer.remove();
            SessionUtils.advanceClock(localSession, fact);
            localSession.insert(fact);
        }
        size = 0;
        localSession.fireAllRules();
        droolsConfiguration.getChannels().forEach(localSession::registerChannel);
        return localSession;
    }

    @Override
    public Delta delta() {
        throw new IllegalStateException("Delta not expected on HASerializedSession");
    }

    @Override
    public void commit() {
        throw new IllegalStateException("Delta not expected on HASerializedSession");
    }

    private boolean isSaving() {
        return latch.getCount() > 0;
    }

    public static class HASerializedSessionExternalizer implements AdvancedExternalizer<HASerializedSession> {

        private final DroolsConfiguration droolsConfiguration;
        private final KieSessionByteArraySerializer serializer;
        private final Executor executor;

        public HASerializedSessionExternalizer(DroolsConfiguration droolsConfiguration, KieSessionByteArraySerializer serializer, Executor executor) {
            this.serializer = serializer;
            this.droolsConfiguration = droolsConfiguration;
            this.executor = executor;
        }

        @Override
        public Set<Class<? extends HASerializedSession>> getTypeClasses() {
            return asSet(HASerializedSession.class);
        }

        @Override
        public Integer getId() {
            return HASerializerSessionID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HASerializedSession object) throws IOException {
            output.writeInt(object.session!=null?object.session.length:0);
            if(object.session!=null) {
                output.write(object.session);
            }
            output.writeObject(object.buffer);
        }

        @Override
        public HASerializedSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            HASerializedSession haSerializedSession = new HASerializedSession(droolsConfiguration, serializer, executor);
            int len = input.readInt();
            if(len>0) {
                haSerializedSession.session = new byte[len];
                input.read(haSerializedSession.session);
            }
            haSerializedSession.buffer = (Queue<Fact>) input.readObject();
            return haSerializedSession;
        }
    }
}
