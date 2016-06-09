package it.redhat.hacep.rules.reward.catalog;

import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditChannel implements Channel {
    
    public static final String CHANNEL_ID = "audit";
    private static final Logger logger = LoggerFactory.getLogger(AuditChannel.class);

    @Override
    public void send(Object object) {
        logger.info(object.toString());
    }

}
