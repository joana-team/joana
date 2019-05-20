/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * An extensible wrapper around an ordinary map. Usually used to implement mappings that appear in
 * the thesis
 *
 * @param <K> type of the keys
 * @param <V> type of the value
 */
public class DefaultMap<K, V> implements Map<K, V> {

    private final Map<K, V> map;
    private final Extension<K, V> extension;
    private final boolean forbidValueUpdates;
    private final boolean forbidDeletions;
    
    public DefaultMap(Supplier<V> defaultValueProducer, ForbiddenAction... forbiddenActions) {
        this((map, k) -> defaultValueProducer.get());
    }

    public DefaultMap(BiFunction<Map<K, V>, K, V> defaultValueProducer, ForbiddenAction... forbiddenActions) {
        this(new HashMap<>(), new Extension<K, V>() {
            @Override
            public V defaultValue(Map<K, V> map, K key) {
                return defaultValueProducer.apply(map, key);
            }
        }, forbiddenActions);
    }

    public DefaultMap(Map<K, V> map, ForbiddenAction... forbiddenActions) {
        this(map, new Extension<K, V>() {}, forbiddenActions);
    }

    public DefaultMap(Map<K, V> map, Extension<K, V> extension, ForbiddenAction... forbiddenActions) {
        this.map = map;
        this.extension = extension;
        List<ForbiddenAction> fl = Arrays.asList(forbiddenActions);
        this.forbidValueUpdates = fl.contains(ForbiddenAction.FORBID_VALUE_UPDATES);
        this.forbidDeletions = fl.contains(ForbiddenAction.FORBID_VALUE_UPDATES);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (!map.containsKey(key)) {
            map.put((K)key, extension.defaultValue(map, (K) key));
        }
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (map.containsKey(key) && !map.get(key).equals(value)) {
            if (forbidValueUpdates) {
                throw new UnsupportedOperationException(
                        String.format(
                                "Changing the value mapped to key %s from %s to %s is not supported in %s",
                                key, map.get(key), value, this));
            }
            extension.handleValueUpdate(this, key, value);
        }
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if (forbidDeletions) {
            throw new UnsupportedOperationException(
                    String.format("Deletion of key %s from map %s not supported", key, this));
        }
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultMap
                && map.equals(((DefaultMap) o).map)
                && extension.equals(((DefaultMap) o).extension);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public static enum ForbiddenAction {
        FORBID_VALUE_UPDATES,
        FORBID_DELETIONS
    }

    public static interface Extension<K, V> {
        default void handleValueUpdate(DefaultMap<K, V> map, K key, V value) {}

        default V defaultValue(Map<K, V> map, K key) {
            throw new NoSuchElementException(key.toString());
        }
    }
}
