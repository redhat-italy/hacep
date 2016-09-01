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

package it.redhat.hacep.playground.rules.model.util;

import it.redhat.hacep.playground.rules.model.Gameplay;

import java.util.Date;

public class GameplayBuilder {

    private long playerId;

    private long id;

    private String gameName;

    private Date timestamp;

    public GameplayBuilder id(long id) {
        this.id = id;
        return this;
    }

    public GameplayBuilder playerId(long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayBuilder gameName(String gameName) {
        this.gameName = gameName;
        return this;
    }

    public GameplayBuilder timestamp(long time) {
        this.timestamp = new Date(time);
        return this;
    }

    public Gameplay build() {
        return new Gameplay(id, playerId, gameName, timestamp);
    }

}
