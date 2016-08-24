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

package it.redhat.hacep.playground.rules.model.outcome;

public class GamePlayingStats {

    private String gameName;
    private Number amountPlayed;
    private Number numberOfPlays;
    private Number numberOfPlayers;

    public GamePlayingStats(String gameName, Number amountPlayed, Number numberOfPlays, Number numberOfPlayers) {
        this.gameName = gameName;
        this.amountPlayed = amountPlayed;
        this.numberOfPlays = numberOfPlays;
        this.numberOfPlayers = numberOfPlayers;
    }

    public String getGameName() {
        return gameName;
    }

    public Number getAmountPlayed() {
        return amountPlayed;
    }

    public Number getNumberOfPlays()  { return numberOfPlays; }

    public Number getNumberOfPlayers() { return numberOfPlayers; }

    @Override
    public String toString() {
        return "GameStats: { Game: " + gameName
                + ", Amount Played: " + amountPlayed
                + ", Times Played: " + numberOfPlays
                + ", Unique Players: " + numberOfPlayers;
    }

}
