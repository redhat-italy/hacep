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

import java.util.Date;
import java.util.Objects;

public class GameplayBet extends Gameplay {

    private static final long serialVersionUID = 1779732802814978929L;

    private long amount;

    public GameplayBet(long id, Long playerId, Date timestamp, long amount) {
        super(id, playerId, timestamp);
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameplayBet)) return false;
        if (!super.equals(o)) return false;
        GameplayBet that = (GameplayBet) o;
        return amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount);
    }

    @Override
    public String toString() {
        return "GamePlayBet{" +
                "amount=" + amount +
                "} " + super.toString();
    }
}
