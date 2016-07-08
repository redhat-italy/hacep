package it.redhat.hacep.event.model;

import it.redhat.hacep.model.Fact;

import java.time.Instant;

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

public class LoginEvent implements Fact {

    private Instant instant;
    private String usr;
    private String pwd;

    public LoginEvent(Instant instant, String usr, String pwd) {
        this.instant = instant;
        this.usr = usr;
        this.pwd = pwd;
    }

    public String getUsr() {
        return usr;
    }

    public String getPwd() {
        return pwd;
    }

    @Override
    public Instant getInstant() {
        return null;
    }

}
