package bstmap;

import javax.security.auth.kerberos.KerberosKey;
import java.util.*;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V>{
    private int size=0;
    private Node root;
private class Node{
    K key;
    V val;
    Node left;
    Node right;
    Node(K key, V value) {
        this.key = key;
        this.val = value;
        this.left = null;
        this.right = null;
    }
}
    public void printInOrder(){
    
    }
public BSTMap(){
    size=0;
    root=null;
}
private Node getnode(Node item, K key){
    if(item==null){
        return null;
    }
    else {
        if(key.compareTo(item.key)<0){
            return getnode(item.left,key);
        } else if (key.compareTo(item.key)>0) {
            return getnode(item.right,key);
        }
        else return item;
    }
}
    @Override
    public void clear() {
        size=0;
        root=null;
    }

    @Override
    public boolean containsKey(K key) {
        Node node=getnode(root,key);
        return node != null;
    }

    @Override
    public V get(K key) {
        Node node=getnode(root,key);
        return node==null?null:node.val;
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        if(key==null){
            throw new IllegalArgumentException("KEY CANT BE NULL");
        }
        if (root == null) {
            root = new Node(key, value);
            size = 1;
        } else {
            root = putHelper(root, key, value);
        }
    }
    private Node putHelper(Node node, K key, V value) {
        if (node == null) {
            size++;
            return new Node(key, value);
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = putHelper(node.left, key, value);
        } else if (cmp > 0) {
            node.right = putHelper(node.right, key, value);
        } else {
            node.val = value;
        }
        return node;
    }


    @Override
    public Set<K> keySet() {
        Set<K> keys = new TreeSet<>();
        addKeysInOrder(root, keys);
        return keys;
    }
    private void addKeysInOrder(Node node, Set<K> keys) {
        if (node != null) {
            addKeysInOrder(node.left, keys);
            keys.add(node.key);
            addKeysInOrder(node.right, keys);
        }
    }

    @Override
    public V remove(K key) {
        Node node=getnode(root,key);
        if(root==null){
            return null;
        }
        if(node==null){
            return null;
        }
        V removeval=node.val;
        root=removehelp(root,key);
        size--;
        return removeval;
    }
   private Node removehelp(Node node,K key){
        if(node==null){
            return null;
        }
        int temp=key.compareTo(node.key);
        if(temp<0){
            node.left=removehelp(node.left,key);
        }
        else if (temp>0){
           node.right=removehelp(node.right,key);
        }
        else {
            if (node.left == null && node.right == null) {
                return null;
            }
            if(node.left==null&&node.right!=null){
                return node.right;
            }
            else if (node.left!=null&&node.right==null){
                return node.left;
            }
            else if (node.left!=null&&node.right!=null) {
                Node taregt=findmin(node.right);
                node.val=taregt.val;
                node.key=taregt.key;
                node.right = removehelp(node.right,taregt.key);
            }
        };
        return node;
    }
private Node findmin(Node node){
    if(node.left==null) {
        return node;
    }
    else return findmin(node.left);
}
    @Override
    public V remove(K key, V value) {
        Node node = getnode(root, key);
        if (node != null && Objects.equals(node.val, value)) {
            return remove(key);
        }
        return null;
    }


    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
