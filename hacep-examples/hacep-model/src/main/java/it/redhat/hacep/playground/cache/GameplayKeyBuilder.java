package it.redhat.hacep.playground.cache;

import it.redhat.hacep.model.KeyBuilder;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.playground.rules.model.Gameplay;

public class GameplayKeyBuilder implements KeyBuilder<Gameplay, String> {

    @Override
    public Key<String> extractFromFact(Gameplay fact) {
        return new GameplayKey(String.valueOf(fact.getId()), String.valueOf(fact.getPlayerId()));
    }
}
