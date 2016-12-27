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
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class KieSessionSaver {

    private static final Logger LOGGER = LoggerFactory.getLogger(KieSessionSaver.class);

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    private final HAKieSessionBuilder haKieSessionBuilder;

    private final Cache<String, Object> sessionCache;

    public KieSessionSaver(HAKieSessionBuilder haKieSessionBuilder, Cache<String, Object> sessionCache) {
        this.haKieSessionBuilder = haKieSessionBuilder;
        this.sessionCache = sessionCache;
    }

    public void insert(Key key, Fact fact) {
        String sessionKey = key.getGroup();

        if (LOGGER.isDebugEnabled()) LOGGER.debug("Getting session for fact: " + fact + ", key: " + sessionKey);
        synchronized (getLock(sessionKey)) {
            HAKieSession haKieSession;
            Object value = sessionCache.get(sessionKey);
            if (value == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Session doesn't exist, must create a new session");
                }
                haKieSession = haKieSessionBuilder.build();
                sessionCache.put(sessionKey, haKieSession);
            } else {
                haKieSession = (HAKieSession) value;
                if (haKieSession.isSerialized()) {
                    haKieSession = haKieSession.rebuild();
                }
            }

            if (LOGGER.isDebugEnabled()) LOGGER.debug("Insert fact: " + fact);
            haKieSession.insert(fact);

            if (LOGGER.isDebugEnabled()) LOGGER.debug("Put back HAKieSession in the grid for key: " + sessionKey);

            sessionCache.put(sessionKey, haKieSession);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Done saving HAKieSession for key: " + sessionKey);

        }
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
