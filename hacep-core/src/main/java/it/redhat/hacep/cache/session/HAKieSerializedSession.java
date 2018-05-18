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
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
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
        this.version = rulesManager.getReleaseId().getVersion();
        LOGGER.debug(String.format("Create serialized empty session with version [%s]", this.version));
    }

    public HAKieSerializedSession(RulesManager rulesManager, Executor executor, String version, byte[] session) {
        super(rulesManager, executor);
        this.rulesManager = rulesManager;
        this.executor = executor;
        this.version = version;
        this.session = session;
        LOGGER.debug(String.format("Create serialized session with version [%s]", this.version));
    }

    public void add(Fact f) {
        if (isUpgradeNeeded()) {
            rebuildSessionAndUpgrade();
        }
        buffer.offer(f);
        size++;
        if (this.needToSave()) {
            this.createSnapshot();
        }
    }

    private boolean isUpgradeNeeded() {
        return version != null && !version.equals(rulesManager.getReleaseId().getVersion());
    }

    public void createSnapshot() {
        if (saving.compareAndSet(false, true)) {
            latch = new CountDownLatch(1);
            executor.execute(() -> {
                KieSession localSession = null;
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Start consuming buffer: Size [%s] - Buffer [%s]", getSessionSize(), size));
                    }
                    if (isUpgradeNeeded()) {
                        rebuildSessionAndUpgrade();
                    }
                    localSession = buildSession();
                    session = rulesManager.serialize(localSession);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Buffer empty: Size [%s] - Buffer [%s]", getSessionSize(), size));
                    }
                } catch (Exception e) {
                    LOGGER.error("Unexpected exception", e);
                } finally {
                    saving.set(false);
                    latch.countDown();
                    KieSessionUtils.dispose(localSession);
                }
            });
        }
    }

    public HAKieSession rebuild() {
        this.waitForSnapshotToComplete();
        if (isUpgradeNeeded()) {
            rebuildSessionAndUpgrade();
        }
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

    private void rebuildSessionAndUpgrade() {
        KieContainer kieContainer = null;
        KieSession kieSession = null;
        try {
            kieContainer = rulesManager.newKieContainer(this.version);
            kieSession = rulesManager.deserializeOrCreate(kieContainer, this.session);
            replayFacts(kieSession);
            kieContainer.updateToVersion(rulesManager.getReleaseId());
            this.session = rulesManager.serialize(kieContainer, kieSession);
            this.version = rulesManager.getReleaseId().getVersion();
        } finally {
            KieSessionUtils.dispose(kieSession);
            KieSessionUtils.dispose(kieContainer);
        }
    }

    private KieSession buildSession() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Rebuild session from serialized byte array. Buffer size [%s]", getSessionSize()));
        }
        KieSession localSession = rulesManager.deserializeOrCreate(this.session);
        return replayFacts(localSession);
    }

    private KieSession replayFacts(KieSession session) {
        if (!buffer.isEmpty()) {
            rulesManager.registerReplayChannels(session);
            while (!buffer.isEmpty()) {
                Fact fact = buffer.remove();
                KieSessionUtils.advanceClock(session, fact);
                session.insert(fact);
            }
            size = 0;
            session.fireAllRules();
        }
        rulesManager.registerChannels(session);
        return session;
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

    public int getSessionSize() {
        return this.session != null ? this.session.length : 0;
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
            output.writeInt(object.getSessionSize());
            if (object.session != null) {
                output.write(object.session);
                output.writeUTF(object.version);
            }

            Fact[] fa = new Fact[object.buffer.size()];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(baos);

            oo.writeObject( object.buffer.toArray(fa) );
            byte[] ba = baos.toByteArray();
            output.writeInt( ba.length );
            output.write( ba );
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

            int lenBa = input.readInt();
            byte[] ba = new byte[lenBa];
            input.read(ba);

            ByteArrayInputStream bais = new ByteArrayInputStream( ba );
            ObjectInputStream oi = new ObjectInputStream( bais );

            object.buffer = new ConcurrentLinkedQueue<>( Arrays.asList((Fact[]) oi.readObject()));
            return object;
        }
    }
}
