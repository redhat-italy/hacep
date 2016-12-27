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
import java.util.concurrent.atomic.AtomicBoolean;

import static it.redhat.hacep.cache.session.JDGExternalizerIDs.HASerializerSessionID;
import static org.infinispan.commons.util.Util.asSet;

public class HAKieSerializedSession extends HAKieSession {

    private final static Logger LOGGER = LoggerFactory.getLogger(HAKieSerializedSession.class);

    private final Executor executor;
    private final RulesManager rulesManager;

    private AtomicBoolean saving = new AtomicBoolean(false);
    private volatile CountDownLatch latch = new CountDownLatch(0);

    private String version;
    private byte[] session = null;
    private transient long size = 0;
    private Queue<Fact> buffer = new ConcurrentLinkedQueue<>();

    public HAKieSerializedSession(RulesManager rulesManager, Executor executor) {
        super(rulesManager, executor);
        this.rulesManager = rulesManager;
        this.executor = executor;
    }

    public HAKieSerializedSession(RulesManager rulesManager, Executor executor, String version, byte[] session) {
        this(rulesManager, executor);
        this.version = version;
        this.session = session;
    }

    public void add(Fact f) {
        if (isUpgradeNeeded()) {

        }
        buffer.offer(f);
        size++;
        if (this.needToSave()) {
            this.createSnapshot();
        }
    }

    private boolean isUpgradeNeeded() {
        return session!=null && version != null && !version.equals(rulesManager.getReleaseId().getVersion());
    }

    public void createSnapshot() {
        if (saving.compareAndSet(false, true)) {
            latch = new CountDownLatch(1);
            executor.execute(() -> {
                KieSession localSession = null;
                try {
                    printSessionSize("Start consuming buffer");
                    localSession = buildSession();
                    session = rulesManager.serialize(localSession);
                    printSessionSize("Buffer empty");
                } catch (Exception e) {
                    LOGGER.error("Unexpected exception", e);
                } finally {
                    KieSessionUtils.dispose(localSession);
                    saving.set(false);
                    latch.countDown();
                }
            });
        }
    }

    public HAKieSession rebuild() {
        this.waitForSnapshotToComplete();
        KieSession session = buildSession();
        return new HAKieSession(rulesManager, executor, session);
    }

    public void waitForSnapshotToComplete() {
        if (saving.get()) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean needToSave() {
        return (size > rulesManager.getMaxBufferSize());
    }

    private void printSessionSize(String message) {
        if (LOGGER.isDebugEnabled()) {
            int sessionSize = this.session != null ? this.session.length : 0;
            LOGGER.debug(message + " - Size [" + sessionSize + "] - Buffer [" + size + "]");
        }
    }

    private KieSession buildSession() {
        if (LOGGER.isDebugEnabled()) {
            int sessionSize = this.session != null ? this.session.length : 0;
            LOGGER.debug("Rebuild session from serialized byte array. Buffer size [" + sessionSize + "]");
        }
        KieSession localSession = rulesManager.deserializeOrCreate(this.session);
        if (!buffer.isEmpty()) {
            rulesManager.registerReplayChannels(localSession);
            while (!buffer.isEmpty()) {
                Fact fact = buffer.remove();
                KieSessionUtils.advanceClock(localSession, fact);
                localSession.insert(fact);
            }
            size = 0;
            localSession.fireAllRules();
        }
        rulesManager.registerChannels(localSession);
        return localSession;
    }

    @Override
    public void insert(Fact fact) {
        throw new IllegalStateException("Insert a new fact is not expected on HAKieSerializedSession");
    }

    @Override
    public Delta delta() {
        throw new IllegalStateException("Delta not expected on HAKieSerializedSession");
    }

    @Override
    public void commit() {
        throw new IllegalStateException("Delta not expected on HAKieSerializedSession");
    }

    @Override
    public boolean isSerialized() {
        return true;
    }

    public byte[] getSerializedSession() {
        return this.session;
    }

    public static class HASerializedSessionExternalizer implements AdvancedExternalizer<HAKieSerializedSession> {

        private final HAKieSessionBuilder builder;

        public HASerializedSessionExternalizer(HAKieSessionBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Set<Class<? extends HAKieSerializedSession>> getTypeClasses() {
            return asSet(HAKieSerializedSession.class);
        }

        @Override
        public Integer getId() {
            return HASerializerSessionID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HAKieSerializedSession object) throws IOException {
            output.writeInt(object.session != null ? object.session.length : 0);
            if (object.session != null) {
                output.write(object.session);
                output.writeUTF(object.version);
            }
            output.writeObject(object.buffer);
        }

        @Override
        public HAKieSerializedSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            HAKieSerializedSession object = builder.buildSerialized();
            int len = input.readInt();
            if (len > 0) {
                object.session = new byte[len];
                input.read(object.session);
                object.version = input.readUTF();
            }
            object.buffer = (Queue<Fact>) input.readObject();
            return object;
        }
    }
}
