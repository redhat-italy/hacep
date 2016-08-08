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
import it.redhat.hacep.configuration.annotations.HACEPExecutorService;
import it.redhat.hacep.configuration.annotations.HACEPKieSessionSerializer;
import it.redhat.hacep.configuration.annotations.HACEPSessionCache;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.model.SessionKey;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class KieSessionSaver {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionSaver.class);
    private static final Logger audit = LoggerFactory.getLogger("audit.redhat.hacep");

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    @Inject
    @HACEPSessionCache
    private Cache<Key, Object> sessionCache;

    @Inject
    @HACEPKieSessionSerializer
    private KieSessionByteArraySerializer serializer;

    @Inject
    @HACEPExecutorService
    private ExecutorService executorService;

    @Inject
    private DroolsConfiguration droolsConfiguration;

    public KieSessionSaver insert(Key key, Fact fact) {
        SessionKey sessionKey = new SessionKey(key.getGroup());
        audit.info(key + " | " + fact + " | COD_21 | starting to insert fact");
        synchronized (getLock(sessionKey.toString())) {
            HAKieSession haKieSession;
            Object value = sessionCache.get(sessionKey);
            if (isANewSession(value)) {
                haKieSession = new HAKieSession(droolsConfiguration, serializer, executorService);
                sessionCache.put(sessionKey, haKieSession);
            } else if (isASerializedSession(value)) {
                haKieSession = ((HAKieSerializedSession) value).rebuild();
            } else {
                // is a local KieSession
                haKieSession = (HAKieSession) value;
            }
            haKieSession.insert(fact);
            audit.info(key + " | " + fact + " | COD_23 | rules invoked");
            sessionCache.put(sessionKey, haKieSession);
            audit.info(key + " | " + fact + " | COD_24 | fact inserted");
        }
        return this;
    }

    private boolean isANewSession(Object value) {
        return (value == null);
    }

    private boolean isASerializedSession(Object value) {
        return HAKieSerializedSession.class.isAssignableFrom(value.getClass());
    }

    //@todo must be evaluated. In production code something like [1] or use infinispan locking (verifying that everything happens locally)
    // [1] https://github.com/ModeShape/modeshape/blob/master/modeshape-jcr/src/main/java/org/modeshape/jcr/value/binary/NamedLocks.java
    private Object getLock(String name) {
        Object lock = locks.get(name);
        if (lock == null) {
            Object newLock = new Object();
            lock = locks.putIfAbsent(name, newLock);
            if (lock == null) {
                lock = newLock;
            }
        }
        return lock;
    }
}
