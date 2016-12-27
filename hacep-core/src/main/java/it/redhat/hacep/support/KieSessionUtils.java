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
