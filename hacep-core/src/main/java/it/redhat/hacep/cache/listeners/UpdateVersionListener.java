package it.redhat.hacep.cache.listeners;

import it.redhat.hacep.configuration.Router;
import it.redhat.hacep.configuration.RulesManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = false, sync = true, observation = Listener.Observation.POST)
public class UpdateVersionListener {

    private final Logger LOGGER = LoggerFactory.getLogger(UpdateVersionListener.class);

    private final Router router;
    private final RulesManager rulesManager;

    public UpdateVersionListener(Router router, RulesManager rulesManager) {
        this.router = router;
        this.rulesManager = rulesManager;
    }

    @CacheEntryModified
    public void eventReceived(CacheEntryModifiedEvent event) {
        Object key = event.getKey();
        Object value = event.getValue();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Received MODIEFIED key on INFOS key=[{}] value=[{}]", key, value);
        if (RulesManager.RULES_ARTIFACT_ID.equals(key) || RulesManager.RULES_GROUP_ID.equals(key)) {
            throw new IllegalStateException("Cannot change rules artifact or group id.");
        }
        if (RulesManager.RULES_VERSION.equals(key)) {
            updateVersion((String) value);
        }
    }

    private void updateVersion(String value) {
        try {
            router.suspend();
            rulesManager.updateToVersion(value);
        } finally {
            router.resume();
        }
    }

}
