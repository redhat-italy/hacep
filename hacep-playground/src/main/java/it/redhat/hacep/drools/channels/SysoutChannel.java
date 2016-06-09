package it.redhat.hacep.drools.channels;

import org.kie.api.runtime.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysoutChannel implements Channel {
    
    public static final String CHANNEL_ID = "outcomes";
    private static final Logger logger = LoggerFactory.getLogger("it.redhat.hacep.logger");

    @Override
    public void send(Object object) {
        logger.info("=============================>" + object);
    }

}
