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

package it.redhat.hacep.cache.session.utils;

import it.redhat.hacep.model.Fact;
import org.drools.core.time.SessionPseudoClock;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SessionUtils {

    private final static Logger logger = LoggerFactory.getLogger(SessionUtils.class);

    public static void advanceClock(KieSession kieSession, Fact fact) {
        SessionPseudoClock clock = kieSession.getSessionClock();
        long gts = fact.getInstant().toEpochMilli();
        long current = clock.getCurrentTime();
        clock.advanceTime(gts - current, TimeUnit.MILLISECONDS);
    }

    public static void dispose(KieSession localSession) {
        if (localSession != null) {
            try {
                localSession.dispose();
            } catch (Exception e) {
                logger.error("Unexpected exception", e);
            }
        }
    }
}
