package dev.sentinel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RequestHasherTest {

    @Test
    void sameInputGivesTheSameHash() {
        assertEquals(RequestHasher.hash("a", "b", "100.00"), RequestHasher.hash("a", "b", "100.00"));
    }

    @Test
    void aDifferentAmountGivesADifferentHash() {
        assertNotEquals(RequestHasher.hash("a", "b", "100.00"), RequestHasher.hash("a", "b", "100.01"));
    }

    @Test
    void fieldBoundariesMatter() {
        assertNotEquals(RequestHasher.hash("ab", "c"), RequestHasher.hash("a", "bc"));
    }

    @Test
    void hashIsSixtyFourHexCharacters() {
        assertEquals(64, RequestHasher.hash("x").length());
    }
}
