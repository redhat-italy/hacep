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

package it.redhat.hacep.support;

import it.redhat.hacep.model.Fact;
import org.drools.core.time.SessionPseudoClock;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class KieSessionUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(KieSessionUtils.class);

    public static void advanceClock(KieSession kieSession, Fact fact) {
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

    public static void dispose(KieSession kieSession) {
        if (kieSession != null) {
            try {
                kieSession.dispose();
            } catch (Exception e) {
                LOGGER.error("Unexpected exception", e);
            }
        }
    }

    public static void dispose(KieContainer kieContainer) {
        if (kieContainer != null) {
            try {
                kieContainer.dispose();
            } catch (Exception e) {
                LOGGER.error("Unexpected exception", e);
            }
        }
    }

    public static void logResults(Results results) {
        for (Message result : results.getMessages()) {
            switch (result.getLevel()) {
                case ERROR:
                    LOGGER.error(result.toString());
                    break;
                case WARNING:
                    LOGGER.warn(result.toString());
                    break;
                case INFO:
                    LOGGER.info(result.toString());
                    break;
                default:
                    LOGGER.warn(result.toString());
            }
        }
    }
}
