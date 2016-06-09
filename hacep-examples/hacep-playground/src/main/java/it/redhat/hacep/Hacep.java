/**
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

package it.redhat.hacep;

import it.redhat.hacep.configuration.PlaygroundConfiguration;
import it.redhat.hacep.console.NullUI;
import it.redhat.hacep.console.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Hacep {
    private final static Logger logger = LoggerFactory.getLogger(Hacep.class);
    private PlaygroundConfiguration configuration;
    private UI console = new NullUI();

    public PlaygroundConfiguration getConfiguration() {
        return this.configuration;
    }

    public void start() {
        printBanner();
        try {
            console.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public Hacep connectConsole(UI console) {
        this.console = console;
        return this;
    }

    public Hacep configure(PlaygroundConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    private void printBanner() {
        System.out.println("---------------------------------------");
        System.out.println("                HACEP CLI");
        System.out.println("---------------------------------------");
        System.out.println();
    }

}
