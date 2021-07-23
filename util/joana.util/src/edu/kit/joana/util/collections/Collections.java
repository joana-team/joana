/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import com.google.common.collect.Iterables;

import java.util.*;

/**
 * TODO: @author Add your name here.
 */
public class Collections {

	
	public static <T, S extends T> Collection<S> concatDisjunctCollections(final Iterable<? extends Collection<S>> inputs) {

		int size = 0;
		for (Collection<? extends T> input : inputs) {
			size += input.size();
		}
		
		final int ssize = size; 
		
		return new AbstractCollection<S>() {
			
			final int size = ssize;
			
			@Override
			public int size() {
				return size;
			}
			
			@Override
			public int hashCode() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean equals(Object obj) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public Iterator<S> iterator() {
				return Iterables.<S>concat(inputs).iterator();
			}
			
			@Override
			public boolean contains(Object o) {
				for (Collection<S> input : inputs) {
					if (input.contains(o)) return true;
				}
				return false;
			}
		};
		
		
	}

	public static <T, S extends Set<T>> Set<T> merge(S s1, S s2) {
		HashSet<T> s = new HashSet<>();
		s.addAll(s1);
		s.addAll(s2);
		return s;
	}
}
