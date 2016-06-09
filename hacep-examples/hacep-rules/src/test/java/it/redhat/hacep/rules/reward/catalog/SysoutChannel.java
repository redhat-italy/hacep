package it.redhat.hacep.rules.reward.catalog;

import org.kie.api.runtime.Channel;

public class SysoutChannel implements Channel {
    
    public static final String CHANNEL_ID = "outcomes"; 

    @Override
    public void send(Object object) {
        System.out.println(object.toString());
        
    }

}
