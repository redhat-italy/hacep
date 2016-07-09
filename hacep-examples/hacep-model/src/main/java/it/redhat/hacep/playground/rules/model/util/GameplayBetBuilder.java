package it.redhat.hacep.playground.rules.model.util;

import it.redhat.hacep.playground.rules.model.GameplayBet;

import java.util.Date;

public class GameplayBetBuilder {

    private Long playerId;

    private Long id = new Long(0);

    private Long amount;

    private Date timestamp;

    public GameplayBetBuilder playerId(Long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayBetBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public GameplayBetBuilder amount(Long amount) {
        this.amount = amount;
        return this;
    }

    public GameplayBetBuilder timestamp(long time) {
        this.timestamp = new Date(time);
        return this;
    }

    public GameplayBet build() {
        return new GameplayBet(id, playerId, timestamp, amount);
    }

}
