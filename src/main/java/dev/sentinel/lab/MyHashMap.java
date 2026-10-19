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
@SuppressWarnings("unchecked")
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
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                V old = current.value;
                current.value = value;
                return old;
            }
            current = current.next;
        }
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        if (size > buckets.length * LOAD_FACTOR) {
            resize();
        }
        return null;
    }

    /** Returns the value for the key, or null if absent. */
    public V get(K key) {
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }


    /** Removes the key. Returns the removed value, or null if the key was absent. */
    public V remove(K key) {
        int index = indexFor(key, buckets.length);
        Node<K, V> previous = null;
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;

    }

    public int size() {
        return size;
    }

    int capacity() {
        return buckets.length;
    }

    /** Which bucket does this key belong in? Think about negative hash codes. */
    private int indexFor(K key, int capacity) {
        return Math.floorMod(key.hashCode(), capacity);
    }

    private void resize() {
        Node<K, V>[] oldBuckets = buckets;
        int newCapacity = 2*capacity();
        Node<K, V>[] newBuckets = (Node<K, V>[]) new Node[newCapacity];

        for (int i = 0; i < oldBuckets.length; i++) {
            Node<K, V> current = oldBuckets[i];
            while (current != null) {
                Node<K, V> next = current.next;
                int index = indexFor(current.key, newCapacity);
                current.next = newBuckets[index];
                newBuckets[index] = current;
                current = next;
            }
        }
        buckets = newBuckets;
    }
}
