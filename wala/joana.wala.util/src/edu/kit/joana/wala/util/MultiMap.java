/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.cscott.jutil.CollectionFactory;
import net.cscott.jutil.Factories;

/**
 * <p>
 * Title: Dependance Analysis for Java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002 Christian Hammer
 * </p>
 * <p>
 * Organization: University of Passau, Software Systems Chair
 * </p>
 *
 * @author Christian Hammer
 * @version 1.0
 */

public class MultiMap<K, V, C extends Collection<V>> extends HashMap<K, C> {

	private static final long serialVersionUID = 4953048202063525184L;

	public static <K, V> boolean addTo(Map<K, Set<V>> map, K key, V o) {
		Set<V> s = map.get(key);
		if (s == null) {
			s = new HashSet<V>();
			map.put(key, s);
		}
		return s.add(o);
	}

	public static <K, V> boolean addAllTo(Map<K, Set<V>> map, K key,
			Set<? extends V> o) {
		Set<V> s = map.get(key);
		if (s == null) {
			s = new HashSet<V>();
			map.put(key, s);
		}
		return s.addAll(o);
	}

	public static <K, V> boolean removeAllFrom(Map<K, Set<V>> map, K key,
			Set<? extends V> o) {
		Set<V> s = map.get(key);
		if (s == null)
			return false;
		return s.removeAll(o);
	}

	CollectionFactory<V> cf;
	int initCapacity;

	public MultiMap() {
		this(1);
	}

	public MultiMap(int intitialCapacities) {
		this(Factories.<V>hashSetFactory(), intitialCapacities);
	}

	public MultiMap(CollectionFactory<V> f, int intitialCapacities) {
		cf = f;
		initCapacity = intitialCapacities;
	}

	public MultiMap(Map<? extends K, ? extends C> f) {
		super(f);
		cf = Factories.<V>hashSetFactory();
		initCapacity = 1;
	}

	public boolean add(K key, V value) {
		C s = get(key);
		return s.add(value);
	}

	public boolean addAll(K key, C values) {
		C s = get(key);
		return s.addAll(values);
	}

	public boolean removeAll(K key, C values) {
		C s = super.get(key);
		if (s == null)
			return false; // not change
		return s.removeAll(values);
	}

	/*
	 * private C get(K key) { C s = super.get(key); if (s == null) { s = (C)
	 * cf.makeCollection(initCapacity); // new HashSet(); put(key, s); } return
	 * s; }
	 *
	 * public C get(Object key) { C s = super.get(key); if (s == null) { s = (C)
	 * cf.makeCollection(initCapacity); // new HashSet(); s = (C)
	 * Collections.unmodifiableCollection(s); } return s; }
	 */

	@SuppressWarnings("unchecked")
	public C get(Object key) {
		C s = super.get(key);
		if (s == null) {
			K k;
			try {
				k = (K) key;
			} catch (ClassCastException e) {
				return s;
			}
			s = (C) cf.makeCollection(initCapacity);
			put(k, s);
		}
		return s;
	}
}
