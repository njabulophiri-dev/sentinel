package dev.sentinel.engine;

import java.util.List;

/** The engine's verdict: a decision, a 0-100 risk score, and every rule that contributed. */
public record Assessment(Decision decision, int score, List<RuleHit> hits) {

    public Assessment {
        hits = List.copyOf(hits);
    }
}
