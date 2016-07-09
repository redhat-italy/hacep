package it.redhat.hacep.playground.rules.model.util;

import it.redhat.hacep.playground.rules.model.GameplayBet;

public class GameplayBetGenerator extends Generator<GameplayBet> {

    private long id = new Long(0);

    private long playerId;

    private long amount;


    public GameplayBetGenerator playerId(long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayBetGenerator id(long id) {
        this.id = id;
        return this;
    }

    public GameplayBetGenerator amount(long amount) {
        this.amount = amount;
        return this;
    }

    @Override
    protected GameplayBet build(long ts) {
        return new GameplayBetBuilder().id(id).playerId(playerId).amount(amount).timestamp(ts).build();
    }
}
