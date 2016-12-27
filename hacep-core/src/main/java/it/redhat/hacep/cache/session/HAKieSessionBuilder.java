package it.redhat.hacep.cache.session;

import it.redhat.hacep.configuration.RulesManager;
import org.kie.api.runtime.KieSession;

import java.util.concurrent.ExecutorService;

public class HAKieSessionBuilder {

    private final RulesManager rulesManager;
    private final ExecutorService executorService;

    public HAKieSessionBuilder(RulesManager rulesManager, ExecutorService executorService) {
        this.rulesManager = rulesManager;
        this.executorService = executorService;
    }

    public HAKieSession build() {
        return new HAKieSession(rulesManager, executorService);
    }

    public HAKieSession build(KieSession session) {
        return new HAKieSession(rulesManager, executorService, session);
    }

    public HAKieSerializedSession buildSerialized() {
        return new HAKieSerializedSession(rulesManager, executorService);
    }

    public HAKieSerializedSession buildSerialized(String version, byte[] buffer) {
        return new HAKieSerializedSession(rulesManager, executorService, version, buffer);
    }

    public String getVersion() {
        return rulesManager.getReleaseId().getVersion();
    }
}
