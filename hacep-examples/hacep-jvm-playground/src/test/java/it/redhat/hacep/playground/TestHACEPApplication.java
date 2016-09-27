package it.redhat.hacep.playground;

import it.redhat.hacep.configuration.HACEP;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;

public class TestHACEPApplication {

    @Test
    public void testStart() {
        HACEP hacep = null;
        try {
            Weld weld = new Weld();
            WeldContainer container = weld.initialize();

            hacep = container.instance().select(HACEP.class).get();
            hacep.start();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (hacep != null) {
                hacep.stop();
            }
        }
    }
}
