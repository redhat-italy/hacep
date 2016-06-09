package it.redhat.hacep.rules.model.util;

import it.redhat.hacep.rules.model.Gameplay;

public class GameplayGenerator extends Generator<Gameplay> {

    private long playerId;

    private long id = 0L;

    public GameplayGenerator playerId(long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayGenerator id(long id) {
        this.id = id;
        return this;
    }

    @Override
    protected Gameplay build(long ts) {
        return new GameplayBuilder().playerId(playerId).id(id).timestamp(ts).build();
    }
}
