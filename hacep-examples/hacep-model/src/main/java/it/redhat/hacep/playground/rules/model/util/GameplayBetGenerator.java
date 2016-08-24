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

import it.redhat.hacep.model.Key;
import it.redhat.hacep.playground.cache.GameplayKey;
import it.redhat.hacep.playground.rules.model.GameplayBet;

public class GameplayBetGenerator extends Generator<GameplayBet> {

    private long id = new Long(0);

    private long playerId;

    private Key eventKey;

    private String gameName;

    private long amount;


    public GameplayBetGenerator playerId(long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayBetGenerator id(long id) {
        this.id = id;
        return this;
    }

    public GameplayBetGenerator eventKey(Key eventKey) {
        this.eventKey = eventKey;
        return this;
    }

    public GameplayBetGenerator gameName(String gameName) {
        this.gameName = gameName;
        return this;
    }

    public GameplayBetGenerator amount(long amount) {
        this.amount = amount;
        return this;
    }

    @Override
    protected GameplayBet build(long ts) {
        if (eventKey == null) {
            eventKey= new GameplayKey(String.valueOf(id), String.valueOf(playerId));
        }
        return (GameplayBet) new GameplayBetBuilder()
                .id(id)
                .playerId(playerId)
                .gameName(gameName)
                .amount(amount)
                .timestamp(ts)
                .build()
                .forKey(eventKey);
    }
}
