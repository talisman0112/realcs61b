package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    @Override
    public void clear() {
        size=0;
        buckets=createTable(bucketCount);
    }

    @Override
    public boolean containsKey(K key) {

        int index=Math.abs(key.hashCode() % bucketCount);
        Collection<Node> bucket=buckets[index];
        for(Node node:bucket){
            if(Objects.equals(node.key,key)){
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(K key) {
        int index=Math.abs(key.hashCode() % bucketCount);
        Collection<Node> bucket=buckets[index];
        for(Node node:bucket){
            if(Objects.equals(node.key,key)){
                return node.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        int index =Math.abs(key.hashCode() % bucketCount);
        Collection<Node> bucket = buckets[index];
        for (Node node : bucket) {
            if (Objects.equals(node.key, key)) {
                node.value = value;
                return;
            }
        }
        bucket.add(createNode(key, value));
        size++;
        resizeIfNecessary();
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Collection<Node> bucket : buckets) {
            for (Node node : bucket) {
                keys.add(node.key);
            }
        }
        return keys;
    }

    @Override
    public V remove(K key) {
        int index =Math.abs(key.hashCode() % bucketCount);
        Collection<Node> bucket = buckets[index];
        Iterator<Node> it = bucket.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (Objects.equals(node.key, key)) {
                it.remove();
                size--;
                return node.value;
            }
        }
        return null;
    }

    @Override
    public V remove(K key, V value) {
        int index =Math.abs(key.hashCode() % bucketCount);
        Collection<Node> bucket = buckets[index];
        Iterator<Node> it = bucket.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (Objects.equals(node.key, key)&&Objects.equals(node.value,value)) {
                it.remove();
                size--;
                return node.value;
            }
        }
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }
    private static final int DEFAULT_INITIAL_SIZE = 16;

    private static final double DEFAULT_MAX_LOAD_FACTOR = 0.75;
    /** 存储所有桶的数组，每个元素是一个 Collection<Node> */

    /** 当前映射中键值对的数量 */
    private int size;

    /** 最大负载因子，超过此值时进行扩容 */
    private double maxLoadFactor;

    /** 当前桶的数量（buckets.length） */
    private int bucketCount;

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!

    /** Constructors */
    public MyHashMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_MAX_LOAD_FACTOR);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, DEFAULT_MAX_LOAD_FACTOR);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        if (initialSize < 1) {
            initialSize = 1;
        }
        if (maxLoad <= 0) {
            maxLoad = DEFAULT_MAX_LOAD_FACTOR;
        }
        this.maxLoadFactor = maxLoad;
        this.bucketCount = initialSize;
        this.buckets = createTable(initialSize);
        this.size = 0;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    @SuppressWarnings("unchecked")
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table=new Collection[tableSize];
        for(int i=0;i<tableSize;i++){
            table[i]=createBucket();
        }
        return table;
    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!
}
    private void resizeIfNecessary() {
        if (size > bucketCount * maxLoadFactor) {
            resize(bucketCount * 2);
        }
    }

    /** 扩容到新的容量 newCapacity */
    private void resize(int newCapacity) {
        Collection<Node>[] oldBuckets = buckets;
        bucketCount = newCapacity;
        buckets = createTable(newCapacity);
        for (Collection<Node> bucket : oldBuckets) {
            for (Node node : bucket) {
                int newIndex = Math.abs(Math.abs(node.key.hashCode() % bucketCount));
                buckets[newIndex].add(node);
            }
}
}
}