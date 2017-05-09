/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("jgroups.bind_addr", "localhost");

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
