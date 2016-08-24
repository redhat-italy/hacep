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

package it.redhat.hacep.playground.rules.model;

import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.playground.cache.GameNameKey;
import it.redhat.hacep.playground.cache.GameplayKey;

import java.time.Instant;
import java.util.*;

public class Gameplay implements Fact {

    private static final long serialVersionUID = 7517352753296362943L;

    protected long id;

    protected Long playerId;

    protected String gameName;

    protected Date timestamp;

    public Gameplay(long id, Long playerId, String gameName, Date timestamp) {
        this.id = id;
        this.playerId = playerId;
        this.gameName = gameName;
        this.timestamp = timestamp;
    }

    @Override
    public Instant getInstant() {
        return timestamp.toInstant();
    }

    @Override
    public Set<Key> extractKeys() {
        Set<Key> keys = new HashSet<>();
        keys.add(new GameplayKey(String.valueOf(id), String.valueOf(playerId)));
        keys.add(new GameNameKey(String.valueOf(id), gameName));
        return keys;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getGameName() { return gameName; }

    public void setGameName(String gameName) { this.gameName = gameName; }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Gameplay)) return false;
        Gameplay gameplay = (Gameplay) o;
        return id == gameplay.id &&
                Objects.equals(playerId, gameplay.playerId) &&
                Objects.equals(gameName, gameplay.gameName) &&
                Objects.equals(timestamp, gameplay.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerId, gameName, timestamp);
    }

    @Override
    public String toString() {
        return "GamePlay{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", game=" + gameName +
                ", timestamp=" + timestamp +
                '}';
    }
}
