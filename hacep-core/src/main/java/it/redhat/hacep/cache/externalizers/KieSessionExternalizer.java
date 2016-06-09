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

package it.redhat.hacep.cache.externalizers;

import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.reteoo.common.ReteWorkingMemory;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Util;
import org.kie.api.runtime.KieSession;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class KieSessionExternalizer implements AdvancedExternalizer<KieSession> {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionExternalizer.class);
    public static final int EXTERNALIZER_ID = 3000;

    private final KieSessionByteArraySerializer serializer;

    public KieSessionExternalizer(KieSessionByteArraySerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Set<Class<? extends KieSession>> getTypeClasses() {
        return Util.asSet(KieSession.class, StatefulKnowledgeSession.class, StatefulKnowledgeSessionImpl.class,
                ReteWorkingMemory.class, CommandBasedStatefulKnowledgeSession.class);
    }

    @Override
    public Integer getId() {
        return EXTERNALIZER_ID;
    }

    @Override
    public void writeObject(ObjectOutput output, KieSession object) throws IOException {
        logger.debug("Write KieSession to byte array");
        byte[] buffer = serializer.writeObject(object);
        output.writeInt(buffer.length);
        output.write(buffer, 0, buffer.length);
    }

    @Override
    public KieSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        logger.debug("Read KieSession from serialized session");
        int len = input.readInt();
        byte[] buffer = new byte[len];
        int read = input.read(buffer, 0, len);
        if (read < len) {
            logger.error(String.format("Read less than required len[%s] read[%s]", len, read));
        }
        return serializer.readSession(buffer);
    }

}
