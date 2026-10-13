package dev.sentinel.lab;

/**
 * Week 1, Day 2: build your own HashMap.
 *
 * Rules:
 *  - Backing store is an array of linked-list buckets (separate chaining).
 *  - No java.util.Map / HashMap inside this class.
 *  - Resize (double the array and re-hash every entry) when size > capacity * 0.75.
 *  - Allow null values; reject null keys with NullPointerException.
 *
 * Work through MyHashMapTest one test at a time: make one go green, then the next.
 */
public class MyHashMap<K, V> {

    private static final int INITIAL_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    @SuppressWarnings("unchecked")
    private Node<K, V>[] buckets = (Node<K, V>[]) new Node[INITIAL_CAPACITY];
    private int size;

    /** Insert or overwrite. Returns the previous value, or null if the key was new. */
    public V put(K key, V value) {
        // TODO: 1) find bucket index  2) walk the chain  3) overwrite or prepend  4) resize if needed
        throw new UnsupportedOperationException("TODO");
    }

    /** Returns the value for the key, or null if absent. */
    public V get(K key) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    public boolean containsKey(K key) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** Removes the key. Returns the removed value, or null if the key was absent. */
    public V remove(K key) {
        // TODO: careful with removing the head of a chain vs. a middle node
        throw new UnsupportedOperationException("TODO");
    }

    public int size() {
        return size;
    }

    /** Which bucket does this key belong in? Think about negative hash codes. */
    private int indexFor(K key, int capacity) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    private void resize() {
        // TODO: new array of double size; every entry must be re-hashed, not copied by index
        throw new UnsupportedOperationException("TODO");
    }
}
