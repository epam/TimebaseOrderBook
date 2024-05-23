package com.epam.deltix.orderbook.core.impl.collections.rbt;


import com.epam.deltix.orderbook.core.impl.ObjectPool;

import java.util.*;

/**
 * A Red-Black tree-based implementation.
 * The map is sorted according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * <p>This implementation provides guaranteed log(n) time cost for the
 * {@code containsKey}, {@code get}, {@code put} and {@code remove}
 * operations.  Algorithms are adaptations of those in Cormen, Leiserson, and
 * Rivest's <em>Introduction to Algorithms</em>.
 */
public class RBTree<K, V> {
    private static final boolean RED = false;
    private static final boolean BLACK = true;
    /**
     * The comparator used to maintain order in this tree map, or
     * null if it uses the natural ordering of its keys.
     */
    private final Comparator<? super K> comparator;
    private final ObjectPool<Entry<K, V>> pool;
    private final EntryIterator entryIterator = new EntryIterator(null);
    private Entry<K, V> root;
    /**
     * The number of entries in the tree
     */
    private int size = 0;
    /**
     * The number of structural modifications to the tree.
     */
    private int modCount = 0;

    // Query Operations
    /**
     * Index for tracking array traversal in buildFromSorted()
     */
    private int currentIndex = 0;

    /**
     * Constructs a new, empty tree map, ordered according to the given
     * comparator.  All keys inserted into the map must be <em>mutually
     * comparable</em> by the given comparator: {@code comparator.compare(k1,
     * k2)} must not throw a {@code ClassCastException} for any keys
     * {@code k1} and {@code k2} in the map.  If the user attempts to put
     * a key into the map that violates this constraint, the {@code put(Object
     * key, Object value)} call will throw a
     * {@code ClassCastException}.
     *
     * @param initialSize initial size for object pool
     * @param comparator  the comparator that will be used to order this map.
     *                    If {@code null}, the {@linkplain Comparable natural
     *                    ordering} of the keys will be used.
     */
    public RBTree(final int initialSize, final Comparator<? super K> comparator) {
        this.comparator = comparator;
        pool = new ObjectPool<>(initialSize, Entry::new);
    }

    /**
     * Test two values for equality.  Differs from o1.equals(o2) only in
     * that it copes with {@code null} o1 properly.
     * @param o1 the first object to be compared for equality
     * @param o2 the second object to be compared for equality
     * @return {@code true} if the objects are equal or both {@code null}; {@code false} otherwise
     */
    static boolean valEquals(final Object o1, final Object o2) {
        return (Objects.equals(o1, o2));
    }

    /**
     * Returns the key corresponding to the specified Entry.
     *
     * @param <K> The type parameter representing the key's type in the entry.
     * @param e The {@link Entry} from which the key is to be retrieved. Must not be {@code null}.
     * @return The key corresponding to the specified Entry {@code e}.
     * @throws NoSuchElementException if the provided entry {@code e} is {@code null}, indicating that there is no entry to retrieve a key from.
     */
    static <K> K key(final Entry<K, ?> e) {
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e.key;
    }

    /**
     * Returns the successor of the specified Entry, or null if no such.
     *
     * @param <K> the type parameter representing the key's type in the entry
     * @param <V> the type parameter representing the value's type in the entry
     * @param t the entry whose successor is to be found; may be null
     * @return the successor of the specified entry if it exists; otherwise, {@code null}
     */
    static <K, V> Entry<K, V> successor(final Entry<K, V> t) {
        if (t == null) {
            return null;
        } else if (t.right != null) {
            Entry<K, V> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            Entry<K, V> p = t.parent;
            Entry<K, V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Determines the color attribute of a given tree node within Red-Black Tree operations.
     * <p>
     * This utility method is a part of the balancing operations employed by Red-Black Trees during insertion and deletion
     * to maintain their balanced tree properties. It simplifies the process by safely handling {@code null} references,
     * avoiding the complexity and messiness of null checks within the core algorithms. Directly dealing with {@code null}
     * values is a deviation from the approach described in the CLR (Cormen, Leiserson, Rivest, and Stein) textbook, which
     * utilizes dummy nil nodes. This adaptation enhances readability and efficiency by removing the need for dummy nodes
     * and explicit null checks.
     * </p>
     *
     * <p>
     * By returning a predefined color (black) for {@code null} nodes, this method ensures that the tree's properties
     * are correctly maintained during algorithmic operations without introducing the additional complexity of handling
     * dummy nodes. This approach is particularly beneficial for operations that rely on the color properties of a node's
     * parent or children, which may be {@code null} at the edges of the tree.
     * </p>
     *
     * @param <K> the type parameter representing the key's type in the entry
     * @param <V> the type parameter representing the value's type in the entry
     * @param p The node whose color is to be determined. Can be {@code null}, in which case it is considered black
     *          to maintain Red-Black Tree properties.
     * @return The color of the node {@code p}, with the color black ({@code false}) returned for {@code null} nodes to
     *         simplify handling at the tree's boundaries.
     */
    private static <K, V> boolean colorOf(final Entry<K, V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <K, V> Entry<K, V> parentOf(final Entry<K, V> p) {
        return (p == null ? null : p.parent);
    }

    private static <K, V> void setColor(final Entry<K, V> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    private static <K, V> Entry<K, V> leftOf(final Entry<K, V> p) {
        return (p == null) ? null : p.left;
    }

    private static <K, V> Entry<K, V> rightOf(final Entry<K, V> p) {
        return (p == null) ? null : p.right;
    }

    /**
     * Finds the level down to which to assign all nodes BLACK.  This is the
     * last `full' level of the complete binary tree produced by buildTree.
     * The remaining nodes are colored RED. 'This makes a `nice' set of
     * color assignments wrt future insertions.' This level number is
     * computed by finding the number of splits needed to reach the zeroeth
     * node.
     *
     * @param size the (non-negative) number of keys in the tree to be built
     * @return an integer representing the level in the tree down to which all nodes are to be colored black,
     *         ensuring an optimal structure for future growth
     */
    private static int computeRedLevel(final int size) {
        return 31 - Integer.numberOfLeadingZeros(size + 1);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    // Little utilities

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key} compares
     * equal to {@code k} according to the map's ordering, then this
     * method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <em>necessarily</em>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@code containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned
     * @return value for a given key
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    public V get(final Object key) {
        final Entry<K, V> p = getEntry(key);
        return (p == null ? null : p.value);
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * Retrieves the first (lowest) key currently in this map.
     *
     * This method returns the lowest key stored in the map, based on the map's
     * current sorting criteria. It is particularly useful in scenarios where
     * an operation needs to start processing or inspection from the very beginning
     * of the ordered map. The method assumes that the map is not empty and has at
     * least one key-value mapping stored.
     * @throws NoSuchElementException {@inheritDoc}
     * @return the first (lowest) key currently stored in this map
     */
    public K firstKey() {
        return key(getFirstEntry());
    }

    // Red-black mechanics

    /**
     * @throws NoSuchElementException {@inheritDoc}
     * @return the last key currently stored in this map
     */
    public K lastKey() {
        return key(getLastEntry());
    }

    /**
     * Returns this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key.
     *
     * @param key The key whose associated entry is to be returned.
     * @return this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    final Entry<K, V> getEntry(final Object key) {
        // Offload comparator-based version for the sake of performance
        if (comparator != null) {
            return getEntryUsingComparator(key);
        }
        if (key == null) {
            throw new NullPointerException();
        }
        @SuppressWarnings("unchecked") final Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = k.compareTo(p.key);
            if (cmp < 0) {
                p = p.left;
            } else if (cmp > 0) {
                p = p.right;
            } else {
                return p;
            }
        }
        return null;
    }

    /**
     * Version of getEntry using comparator. Split off from getEntry
     * for performance. (This is not worth doing for most methods
     * that are less dependent on comparator performance, but is
     * worthwhile here.)
     * @param key The key whose associated entry in the tree is to be returned. The type of the key must
     *            be compatible with the {@code comparator} used by this tree.
     * @return The {@code Entry} associated with the given key if it exists or {@code null} if the tree
     *         does not contain an entry for the key.
     * @throws ClassCastException if the specified key's type prevents it from being compared by the
     *                            tree's comparator.
     */
    final Entry<K, V> getEntryUsingComparator(final Object key) {
        @SuppressWarnings("unchecked") final K k = (K) key;
        final Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K, V> p = root;
            while (p != null) {
                final int cmp = cpr.compare(k, p.key);
                if (cmp < 0) {
                    p = p.left;
                } else if (cmp > 0) {
                    p = p.right;
                } else {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    public V put(final K key, final V value) {
        Entry<K, V> t = root;
        if (t == null) {
            compare(key, key); // type (and possibly null) check

            root = pool.borrow();
            root.set(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K, V> parent;
        // split comparator and comparable paths
        final Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        } else {
            if (key == null) {
                throw new NullPointerException();
            }
            @SuppressWarnings("unchecked") final Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        }
        final Entry<K, V> e = pool.borrow();
        e.set(key, value, parent);
        if (cmp < 0) {
            parent.left = e;
        } else {
            parent.right = e;
        }
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * Removes the mapping for this key from this RBTree if present.
     *
     * @param key key for which mapping should be removed
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    public V remove(final Object key) {
        final Entry<K, V> p = getEntry(key);
        if (p == null) {
            return null;
        }

        final V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            pool.release(e);
        }
        modCount++;
        size = 0;
        root = null;
    }

    /**
     * Returns an iterator over the map's entries.
     *
     * This method provides an {@link Iterator} that allows iterating through all the entries ({@code Map.Entry<K, V>}) in the map,
     * starting with the first entry. The iteration is in the order determined by the specific implementation of the map,
     * which could be insertion order, natural ordering of keys, or any other order defined by the map.
     *
     * Note: The returned iterator supports the {@code remove()} operation if the underlying map supports it. However,
     * modifying the map while iterating (except through the iterator's own {@code remove} method) may result in
     * {@link ConcurrentModificationException}.
     *
     * <p>Usage example:</p>
     * <pre>{@code
     * for (Map.Entry<K, V> entry : mapInstance.iterator()) {
     *     // Process each entry
     * }
     * }</pre>
     *
     * <p>This method initializes or resets the iterator's state before returning it, ensuring that each call to
     * {@code iterator()} starts the iteration from the first entry of the map.</p>
     *
     * @return an {@link Iterator} over the map's entries, starting from the first entry.
     */
    public Iterator<Map.Entry<K, V>> iterator() {
        entryIterator.reset(getFirstEntry());
        return entryIterator;
    }

    /**
     * Compares two keys using either their natural ordering or a specified {@link Comparator}.
     *
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Assuming a comparator set for Strings
     * int result = compare("apple", "banana"); // Using the comparator
     *
     * // Assuming no comparator is set, and keys are Comparable
     * int result = compare(10, 20); // Using natural ordering
     * }</pre>
     *
     * @param k1 the first object to be compared.
     * @param k2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to,
     *         or greater than the second.
     * @throws ClassCastException if the keys are not {@link Comparable} or are not mutually comparable
     *                            when {@code comparator} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    final int compare(final Object k1, final Object k2) {
        return comparator == null ?
                ((Comparable<? super K>) k1).compareTo((K) k2) :
                comparator.compare((K) k1, (K) k2);
    }

    /**
     * Returns the first Entry in the RBTree (according to the RBTree's
     * key-sort function).  Returns null if the RBTree is empty.
     *
     * @return the first entry in the Red-Black Tree if it exists, or {@code null} if the tree is empty
     */
    final Entry<K, V> getFirstEntry() {
        Entry<K, V> p = root;
        if (p != null) {
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    /**
     * Returns the last Entry in the RBTree (according to the RBTree's
     * key-sort function).  Returns null if the RBTree is empty.
     *
     * @return The last {@link Entry} in the RBTree, representing the maximum key entry according to the tree's
     *         sorting criteria. Returns {@code null} if the RBTree is empty, indicating the absence of any entries.
     */
    final Entry<K, V> getLastEntry() {
        Entry<K, V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    /**
     * From CLR
     * @param p the node around which the left rotation is to be performed. Must not be null and
     *          must have a non-null right child.
     */
    private void rotateLeft(final Entry<K, V> p) {
        if (p != null) {
            final Entry<K, V> r = p.right;
            p.right = r.left;
            if (r.left != null) {
                r.left.parent = p;
            }
            r.parent = p.parent;
            if (p.parent == null) {
                root = r;
            } else if (p.parent.left == p) {
                p.parent.left = r;
            } else {
                p.parent.right = r;
            }
            r.left = p;
            p.parent = r;
        }
    }

    /**
     * From CLR
     @param p the node around which the right rotation is to be performed. Must not be null and
      *          must have a non-null right child.
     */
    private void rotateRight(final Entry<K, V> p) {
        if (p != null) {
            final Entry<K, V> l = p.left;
            p.left = l.right;
            if (l.right != null) {
                l.right.parent = p;
            }
            l.parent = p.parent;
            if (p.parent == null) {
                root = l;
            } else if (p.parent.right == p) {
                p.parent.right = l;
            } else {
                p.parent.left = l;
            }
            l.right = p;
            p.parent = l;
        }
    }

    /**
     * Restores the Red-Black Tree properties after a node has been inserted.
     * This method applies a series of rotations and recolorings to maintain the Red-Black Tree properties following
     * the insertion of a new node. The new node initially colored red might cause conflicts with existing Red-Black
     * Tree properties, specifically the rule that two red nodes cannot be consecutive. This method addresses these
     * discrepancies through a series of case analyses and adjustments.
     * The process involves traversing up the tree from the inserted node, looking at the color properties of
     * the node's relatives (parent, grandparent, and uncle). Depending on these properties, the tree undergoes
     * specific rotations (left or right) and recoloring to rebalance itself and preserve the Red-Black properties.
     * Conditions leading to different operations include:
     * 1. If the inserted node's uncle is red, recoloring of the parent, uncle, and grandparent nodes occurs.
     * 2. If the inserted node's uncle is black, and the node is situated in a specific pattern (left-right or right-left),
     *    rotations are performed, and specific nodes are recolored to restore the tree to its proper state.
     * As a final step, the root node is always colored black to ensure the Black-Depth property of Red-Black Trees is
     * satisfied.
     *
     * @param x the newly inserted node that may have caused a violation of the Red-Black Tree properties
     */
    private void fixAfterInsertion(Entry<K, V> x) {
        x.color = RED;

        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                final Entry<K, V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                final Entry<K, V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Delete node p, and then rebalance the tree.
     *
     * @param p the node to be deleted from the tree
     */
    private void deleteEntry(Entry<K, V> p) {
        modCount++;
        size--;

        // If strictly internal, copy successor's element to p and then make p
        // point to successor.
        if (p.left != null && p.right != null) {
            final Entry<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
        final Entry<K, V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                p.parent.left = replacement;
            } else {
                p.parent.right = replacement;
            }

            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;

            // Fix replacement
            if (p.color == BLACK) {
                fixAfterDeletion(replacement);
            }
            pool.release(p);
        } else if (p.parent == null) { // return if we are the only node.
            pool.release(p);
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if (p.color == BLACK) {
                fixAfterDeletion(p);
            }

            if (p.parent != null) {
                if (p == p.parent.left) {
                    p.parent.left = null;
                } else if (p == p.parent.right) {
                    p.parent.right = null;
                }
                p.parent = null;
            }
            pool.release(p);
        }
    }

    /**
     * Restores the Red-Black Tree properties after a node has been deleted.
     * This method is invoked after a node is deleted to ensure the tree remains balanced and maintains the Red-Black Tree invariants.
     * Deletion from a Red-Black Tree can disrupt the tree's balance by removing a black node, potentially violating the properties
     * that define a Red-Black Tree. This method addresses such disruptions through a series of corrective rotations and color changes.
     * The core idea revolves around fixing potential double black issues that arise when a black node is removed or moved. We treat
     * the node {@code x} as carrying an extra black, which could be due to it being a new child of a deleted black node or it being
     * a black node that was moved up. The while loop continues until {@code x} is the root (effectively removing the extra black
     * from the tree) or until {@code x} is made red-black (balancing the extra black). The main operations include:
     * - Rotation and recoloring when the sibling of {@code x} is red, transforming the situation into one of the subsequent cases.
     * - Recoloring when both of the sibling's children are black, moving the extra black up the tree.
     * - Rotation and recoloring to remove the extra black at {@code x} when the sibling and its closer child are black, but its
     *   further child is red.
     * - Rotation and recoloring to properly redistribute the colors and ensure the parent node carries the extra black, preparing
     *   for the algorithm to correct higher-up violations.
     * These operations are mirrored for both left and right sides of the parent, ensuring symmetry in handling.
     * Once the loop exits, if {@code x} was originally marked with an extra black, it is made simply black, restoring the tree's
     * properties. This method is critical for maintaining the red-black properties of the tree after deletions, without
     * significantly impacting its balanced structure.
     *
     * @param x the node to start fixing the Red-Black Tree properties from, typically the child node of the deleted node or
     *          the node that moved into the deleted node's position.
     */
    private void fixAfterDeletion(Entry<K, V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<K, V> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib)) == BLACK &&
                        colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Entry<K, V> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK &&
                        colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
    }

    /**
     * Linear time tree building algorithm from sorted data.
     * It is assumed that the comparator of the TreeMap is already set prior
     * to calling this method.
     *
     * @param values new entries are created from entries
     *               in this array.
     */
    public void buildFromSorted(final ArrayList<V> values) {
        size = values.size();
        currentIndex = 0;
        root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), values);
    }

    /**
     * Recursive "helper method" that does the real work of the
     * previous method.  Identically named parameters have
     * identical definitions.  Additional parameters are documented below.
     * It is assumed that the comparator and size fields of the TreeMap are
     * already set prior to calling this method.  (It ignores both fields.)
     *
     * @param level The current depth in the tree during the recursive build. For the initial call,
     *              this should be 0.
     * @param lo The index of the first element (inclusive) in the current segment being considered
     *           for this subtree. For the initial call, this would typically be 0.
     * @param hi The index of the last element (inclusive) in the current segment for this subtree.
     *           Initially, this should be set to the size of the sorted array minus one.
     * @param redLevel A predetermined depth in the tree at which nodes are colored red to ensure
     *                 the tree satisfies the balancing properties of a Red-Black Tree. This level
     *                 is calculated based on the size of the tree and typically obtained through
     *                 a call to {@code computeRedLevel}.
     * @param values An {@link ArrayList<V>} containing the sorted values to be added to the tree.
     *               This array provides the values in sequence which are then structured into
     *               the tree format according to the balancing rules of the tree being constructed.
     *
     * @return An {@code Entry<K, V>} object representing the root of the constructed subtree for the
     *         current recursive invocation. When called initially, this result represents the root of
     *         the entire tree.
     *
     */
    @SuppressWarnings("unchecked")
    private Entry<K, V> buildFromSorted(final int level,
                                        final int lo,
                                        final int hi,
                                        final int redLevel,
                                        final ArrayList<V> values) {
        /*
         * Strategy: The root is the middlemost element. To get to it, we
         * have to first recursively construct the entire left subtree,
         * so as to grab all of its elements. We can then proceed with right
         * subtree.
         *
         * The lo and hi arguments are the minimum and maximum
         * indices to pull out of the iterator or stream for current subtree.
         * They are not actually indexed, we just proceed sequentially,
         * ensuring that items are extracted in corresponding order.
         */

        if (hi < lo) {
            return null;
        }

        final int mid = (lo + hi) >>> 1;

        Entry<K, V> left = null;
        if (lo < mid) {
            left = buildFromSorted(level + 1, lo, mid - 1, redLevel,
                    values);
        }

        final K key = (K) values.get(currentIndex);
        final V value = values.get(currentIndex);


        final Entry<K, V> middle = pool.borrow();
        middle.set(key, value, null);

        // color nodes in non-full bottommost level red
        if (level == redLevel) {
            middle.color = RED;
        }

        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }

        currentIndex++;

        if (mid < hi) {
            final Entry<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, values);
            middle.right = right;
            right.parent = middle;
        }

        return middle;
    }

    /**
     * Node in the Tree.  Doubles as a means to pass key-value pairs back to
     * user (see Map.Entry).
     */

    static final class Entry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Entry<K, V> left;
        Entry<K, V> right;
        Entry<K, V> parent;
        boolean color;

        /**
         * Returns the key.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @param value the new value to be associated with this entry's key
         * @return the old value that was previously associated with the key, or {@code null} if the key
         *         did not have an associated value prior to this update. Note that a {@code null} return can also indicate that
         *         the key was explicitly mapped to {@code null}.
         */
        public V setValue(final V value) {
            final V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

            return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
        }

        public int hashCode() {
            final int keyHash = (key == null ? 0 : key.hashCode());
            final int valueHash = (value == null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }

        public void set(final K key, final V value, final Entry<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
            this.left = null;
            this.right = null;
            this.color = BLACK;
        }
    }

    /**
     * RBTree Iterator
     */
    final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        Entry<K, V> next;
        Entry<K, V> lastReturned;
        int expectedModCount;

        EntryIterator(final Entry<K, V> first) {
            reset(first);
        }

        private void reset(final Entry<K, V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }

        public boolean hasNext() {
            return next != null;
        }

        public Entry<K, V> next() {
            final Entry<K, V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = successor(e);
            lastReturned = e;
            return e;
        }
    }
}
