package it.redhat.hacep.cluster;

import it.redhat.hacep.cache.listeners.UpdateVersionListener;
import it.redhat.hacep.configuration.Router;
import it.redhat.hacep.configuration.RulesManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = false, sync = true, observation = Listener.Observation.POST)
public class UpdateVersionListnerError {

    private final Logger LOGGER = LoggerFactory.getLogger(UpdateVersionListener.class);

    private final Router router;
    private final RulesManager rulesManager;

    public UpdateVersionListnerError(Router router, RulesManager rulesManager) {
        this.router = router;
        this.rulesManager = rulesManager;
    }

    @CacheEntryModified
    public void eventReceived(CacheEntryModifiedEvent event) {
        Object key = event.getKey();
        Object value = event.getValue();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Received MODIFIED key on INFOS key=[{}] value=[{}]", key, value);
        if (RulesManager.RULES_ARTIFACT_ID.equals(key) || RulesManager.RULES_GROUP_ID.equals(key)) {
            throw new IllegalStateException("Cannot change rules artifact or group id.");
        }
        if (RulesManager.RULES_VERSION.equals(key)) {
            throw new RuntimeException( "Upgrade rules version failed!!!" );
        }
    }

}
