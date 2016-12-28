package it.redhat.hacep.playground;

import it.redhat.hacep.HACEP;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHACEPApplication {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestHACEPApplication.class);

    @Test
    public void testStart() {
        LOGGER.info("Start JVM HACEP Application");
        HACEP hacep = null;
        try {
            System.setProperty("grid.owners", "1");

            Weld weld = new Weld();
            WeldContainer container = weld.initialize();

            hacep = container.instance().select(HACEP.class).get();
            LOGGER.info("HACEP Application instance created");

            hacep.start();
            LOGGER.info("HACEP Application started");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (hacep != null) {
                LOGGER.info("HACEP Application stopping");
                hacep.stop();
            }
        }
    }
}
