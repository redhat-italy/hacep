package it.redhat.hacep.playground.rules.model.util;

import it.redhat.hacep.playground.rules.model.Gameplay;

import java.util.Date;

public class GameplayBuilder {

    private long playerId;

    private long id;

    private Date timestamp;

    public GameplayBuilder id(long id) {
        this.id = id;
        return this;
    }

    public GameplayBuilder playerId(long playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplayBuilder timestamp(long time) {
        this.timestamp = new Date(time);
        return this;
    }

    public Gameplay build() {
        return new Gameplay(id, playerId, timestamp);
    }

}
