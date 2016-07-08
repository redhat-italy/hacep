package it.redhat.hacep;

import it.redhat.hacep.configuration.HACEPApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Startup
@Singleton
public class Application {

    @Inject
    private HACEPApplication hacepApplication;

    @PostConstruct
    public void start() {
        hacepApplication.start();
    }

    @PreDestroy
    public void stop() {
        hacepApplication.stop();
    }
}
