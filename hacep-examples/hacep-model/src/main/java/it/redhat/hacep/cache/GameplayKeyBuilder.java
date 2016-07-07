package it.redhat.hacep.cache;

import it.redhat.hacep.model.KeyBuilder;
import it.redhat.hacep.model.Key;
import it.redhat.hacep.rules.model.Gameplay;

public class GameplayKeyBuilder implements KeyBuilder<Gameplay, String> {

    @Override
    public Key<String> extractFromFact(Gameplay fact) {
        return new GameplayKey(String.valueOf(fact.getId()), String.valueOf(fact.getPlayerId()));
    }
}
