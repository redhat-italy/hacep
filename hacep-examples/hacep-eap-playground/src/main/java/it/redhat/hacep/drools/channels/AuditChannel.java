package it.redhat.hacep.drools.channels;

import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditChannel implements Channel {

    public static final String CHANNEL_ID = "audit";
    private static final Logger audit = LoggerFactory.getLogger("audit.redhat.hacep");

    @Override
    public void send(Object object) {
        audit.info("" + object);
    }

}
