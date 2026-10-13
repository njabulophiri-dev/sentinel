package dev.sentinel.lab;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MyHashMapTest {

    /** Different keys, same hashCode: forces collisions into one bucket. */
    record BadKey(String name) {
        @Override public int hashCode() { return 42; }
    }

    @Test
    void putThenGet() {
        var map = new MyHashMap<String, Integer>();
        map.put("a", 1);
        assertEquals(1, map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    void missingKeyReturnsNull() {
        var map = new MyHashMap<String, Integer>();
        assertNull(map.get("nope"));
        assertFalse(map.containsKey("nope"));
    }

    @Test
    void overwriteReturnsOldValueAndKeepsSize() {
        var map = new MyHashMap<String, Integer>();
        assertNull(map.put("a", 1));
        assertEquals(1, map.put("a", 2));
        assertEquals(2, map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    void collidingKeysAreBothStored() {
        var map = new MyHashMap<BadKey, String>();
        map.put(new BadKey("x"), "X");
        map.put(new BadKey("y"), "Y");
        assertEquals("X", map.get(new BadKey("x")));
        assertEquals("Y", map.get(new BadKey("y")));
        assertEquals(2, map.size());
    }

    @Test
    void removeHeadMiddleAndTailOfChain() {
        var map = new MyHashMap<BadKey, Integer>();
        for (int i = 0; i < 5; i++) map.put(new BadKey("k" + i), i);
        assertEquals(2, map.remove(new BadKey("k2")));   // middle
        assertEquals(4, map.remove(new BadKey("k4")));   // head (last inserted if you prepend)
        assertEquals(0, map.remove(new BadKey("k0")));   // tail
        assertNull(map.remove(new BadKey("k0")));        // already gone
        assertEquals(2, map.size());
        assertEquals(1, map.get(new BadKey("k1")));
        assertEquals(3, map.get(new BadKey("k3")));
    }

    @Test
    void survivesResizingWithManyEntries() {
        var map = new MyHashMap<Integer, Integer>();
        for (int i = 0; i < 10_000; i++) map.put(i, i * 2);
        assertEquals(10_000, map.size());
        for (int i = 0; i < 10_000; i++) assertEquals(i * 2, map.get(i));
    }

    @Test
    void handlesNegativeHashCodes() {
        var map = new MyHashMap<String, Integer>();
        // "polygenelubricants".hashCode() == Integer.MIN_VALUE, a classic trap for Math.abs
        map.put("polygenelubricants", 7);
        assertEquals(7, map.get("polygenelubricants"));
    }

    @Test
    void nullKeyIsRejected() {
        var map = new MyHashMap<String, Integer>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    void nullValueIsAllowed() {
        var map = new MyHashMap<String, String>();
        map.put("a", null);
        assertTrue(map.containsKey("a"));
        assertNull(map.get("a"));
    }
}
