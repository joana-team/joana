/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.lattice.impl.StaticLatticeBitset;


/**
 * Utility class implementing some common operations on lattices. Also provides
 * functionality to load lattices from Strings and InputStreams and to compile
 * a lattice to a immutable but efficient bitset representation.
 *
 */
public final class LatticeUtil {
	private static final String TOKEN_LESS = "<=";
	private static final String TOKEN_COMMENT = "#";

	private LatticeUtil() {}

	/**
	 * Compiles a given lattice to an immutable but efficient bitset
	 * representation.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the lattice.
	 * @param lattice
	 *            the lattice.
	 * @return an immutable but efficient bitset representation of
	 *         <code>lattice</code>.
	 */
	public static <ElementType> IStaticLattice<ElementType> compileBitsetLattice(IEditableLattice<ElementType> lattice) {
		return new StaticLatticeBitset<ElementType>(lattice.getElements(), lattice);
	}

	/**
	 * Loads a lattice containing string elements from a encoding contained in a
	 * string.
	 *
	 * @param string
	 *            the string containing the lattice definition.
	 * @return the <code>IEditableLattice</code> loaded from the string
	 *         definition.
	 * @throws WrongLatticeDefinitionException
	 *             if a syntax error was found in the lattice encoding.
	 */
	public static IEditableLattice<String> loadLattice(String string) throws WrongLatticeDefinitionException {
		StringTokenizer tokenizer = new StringTokenizer(string, "\n");
		IEditableLattice<String> lattice = new EditableLatticeSimple<String>();
		while (tokenizer.hasMoreTokens())
			parseLatticeDefinitionLine(tokenizer.nextToken().trim(), lattice);
		return lattice;
	}

	/**
	 * Loads a lattice containing string elements from a
	 * <code>InputStream</code>
	 *
	 * @param stream
	 *            the <code>InputStream</code> to load the lattice from.
	 * @return the <code>IEditableLattice</code> loaded fom the string
	 *         definition.
	 * @throws WrongLatticeDefinitionException
	 *             if a syntax error was found in the lattice encoding.
	 * @throws IOException
	 *             if an I/O error occured during lattice loading.
	 */
	public static IEditableLattice<String> loadLattice(InputStream stream) throws WrongLatticeDefinitionException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		IEditableLattice<String> lattice = new EditableLatticeSimple<String>();
		while ((line = reader.readLine()) != null) {
			parseLatticeDefinitionLine(line.trim(), lattice);
		}
		return lattice;
	}

	private static void parseLatticeDefinitionLine(String line, IEditableLattice<String> lattice) throws WrongLatticeDefinitionException {
		if (line.startsWith(TOKEN_COMMENT))
			return;
		if (line.length() <= 0)
			return;
		if (line.indexOf(TOKEN_LESS) >= 0) {
			int leindex = line.indexOf(TOKEN_LESS);
			String lower = line.substring(0, leindex).trim();
			String higher = line.substring(leindex + 2).trim();
			if (!lattice.getElements().contains(lower))
				lattice.addElement(lower);
			if (!lattice.getElements().contains(higher))
				lattice.addElement(higher);
			lattice.setImmediatelyGreater(lower, higher);
		} else {
			//throw new WrongLatticeDefinitionException("Invalid line format");
			//Einzelne Elemente zulassen.
			String elem = line.trim();
			if (!lattice.getElements().contains(elem))
				lattice.addElement(elem);
		}


	}

	/**
	 * Loads a lattice containing string elements from a
	 * <code>InputStream</code> and compiles it to a efficient but immutable
	 * bitset representation.
	 *
	 * @param stream
	 *            the <code>InputStream</code> to load the lattice from.
	 * @return the <code>IStaticLattice</code> loaded fom the string
	 *         definition.
	 * @throws WrongLatticeDefinitionException
	 *             if a syntax error was found in the lattice encoding.
	 * @throws IOException
	 *             if an I/O error occured during lattice loading.
	 */
	public static IStaticLattice<String> compileBitsetLattice(InputStream stream) throws WrongLatticeDefinitionException, IOException {
		return compileBitsetLattice(loadLattice(stream));
	}

	/**
     * @see IStaticLattice#collectAllGreaterElements(Object)
     *
	 * @deprecated use {@link IStaticLattice#collectAllGreaterElements(Object)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllGreaterElements(ElementType s, IStaticLattice<ElementType> lat) {
    	return lat.collectAllGreaterElements(s);
	}

	/**
	 * @see ILatticeOperations#findUnreachableFromBottom(Collection)
	 * 
	 * @deprecated use {@link ILatticeOperations#findUnreachableFromBottom(Collection)} instead
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findUnreachableFromBottom(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) throws InvalidLatticeException {
    	return ops.findUnreachableFromBottom(inElements);
	}

	public static <ElementType> void markReachableUp(ArrayList<ElementType> elements, ElementType current, boolean seen[], ILatticeOperations<ElementType> ops) {
		int currentIndex = elements.indexOf(current);
		if (seen[currentIndex])
			return;
		seen[currentIndex] = true;
		for (ElementType parent : ops.getImmediatelyGreater(current))
			markReachableUp(elements, parent, seen, ops);
	}

	/**
	 * @see ILatticeOperations#findUnreachableFromTop(Collection)
	 * 
	 * @deprecated use {@link ILatticeOperations#findUnreachableFromTop(Collection)} instead
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findUnreachableFromTop(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) throws InvalidLatticeException {
		return ops.findUnreachableFromTop(inElements);
	}

	public static <ElementType> void markReachableDown(ArrayList<ElementType> elements, ElementType current, boolean seen[], ILatticeOperations<ElementType> ops) {
		int currentIndex = elements.indexOf(current);
		if (seen[currentIndex])
			return;
		seen[currentIndex] = true;
		for (ElementType child : ops.getImmediatelyLower(current))
			markReachableDown(elements, child, seen, ops);
	}

	/**
	 * @see ILatticeOperations#leastUpperBounds(Object, Object)
	 * 
	 * @deprecated use {@link ILatticeOperations#leastUpperBounds(Object, Object)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> leastUpperBounds(ElementType s, ElementType t, ILatticeOperations<ElementType> ops) {
		assert ops != null;

		return ops.leastUpperBounds(s, t);
	}

	public static <ElementType> Collection<ElementType> min(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> ret = new ArrayList<ElementType>();
		Elements: for (ElementType e : elements) {
			for (ElementType g : ops.getImmediatelyLower(e)) {
				if (elements.contains(g))
					continue Elements;
			}
			ret.add(e);
		}
		return ret;
	}

	/**
	 * @see ILatticeOperations#greatestLowerBounds(Object, Object)
	 * 
	 * @deprecated use {@link ILatticeOperations#greatestLowerBounds(Object, Object)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> greatestLowerBounds(ElementType s, ElementType t, ILatticeOperations<ElementType> ops) {
		assert ops != null;

		return ops.greatestLowerBounds(s, t);
	}

	public static <ElementType> Collection<ElementType> max(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> ret = new ArrayList<ElementType>();
		Elements: for (ElementType e : elements) {
			for (ElementType g : ops.getImmediatelyGreater(e)) {
				if (elements.contains(g))
					continue Elements;
			}
			ret.add(e);
		}
		return ret;
	}

	/**
	 * @see ILatticeOperations#findTopElements(Collection)
	 * 
	 * @deprecated use {@link ILatticeOperations#findTopElements(Collection)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findTopElements(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) {
		return ops.findTopElements(inElements);
	}

	/**
	 * @see ILatticeOperations#findBottomElements(Collection)
	 * 
	 * @deprecated use {@link ILatticeOperations#findBottomElements(Collection)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findBottomElements(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) {
		return ops.findBottomElements(inElements);
	}

	/**
	 * @see IStaticLattice#collectNoninterferingElements(Object)
	 *
	 * @deprecated use {@link IStaticLattice#collectNoninterferingElements(Object)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectNoninterferingElements(ElementType s, IStaticLattice<ElementType> lat) {
    	return lat.collectNoninterferingElements(s);
	}

	/**
	 * @see IStaticLattice#collectAllLowerElements(Object)
	 *
	 * @deprecated use {@link IStaticLattice#collectAllLowerElements(Object)} instead
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllLowerElements(ElementType s, IStaticLattice<ElementType> lat) {
    	return lat.collectAllLowerElements(s);
	}

	/**
	 * @see IStaticLattice#isLeq(Object, Object)
	 *
	 * @deprecated use {@link IStaticLattice#isLeq(Object, Object)} instead
	 */
	@Deprecated
	public static <ElementType> boolean isLeq(IStaticLattice<ElementType> l, ElementType l1, ElementType l2) {
		return l.isLeq(l1, l2);
	}
	
	
	
	public static void naiveTopBottomCompletion(IEditableLattice<String> lattice) {
		Collection<String> tops = lattice.findTopElements(lattice.getElements());
		Collection<String> bots = lattice.findBottomElements(lattice.getElements());
		
		if (tops.size() > 1 ) {
			if (lattice.getElements().contains("top")) {
				throw new IllegalArgumentException("Lattice contains element named \"top\" that isn't the top element");
			}
			lattice.addElement("top");
			for (String t : tops) {
				lattice.setImmediatelyGreater(t, "top");
			}
		}
		
		if (bots.size() > 1 ) {
			if (lattice.getElements().contains("bottom")) {
				throw new IllegalArgumentException("Lattice contains element named \"bottom\" that isn't the bottom element");
			}
			lattice.addElement("bottom");
			for (String t : bots) {
				lattice.setImmediatelyLower(t, "bottom");
			}
		}
	}
	

	/**
	 * An Implementation of the Algorithm presented in
	 * "Stepwise construction of the Dedekind-MacNeille completion"
	 * Bernhard Ganter, Sergei O. Kuznetsov
	 * http://dx.doi.org/10.1007/BFb0054922
	 * @param preorder the pre-order to be completed
	 * @return a lattice completing preorder, with newly-named elements "newElement-n" for some numbers n
	 */
	public static IEditableLattice<String> dedekindMcNeilleCompletion(IEditableLattice<String> preorder) {
		class Cut {
			public Cut(Set<String> a, Set<String> b) {
				this.a = a;
				this.b = b;
			}
			final Set<String> a;
			final Set<String> b;
			
			@Override
			public String toString() {
				return "(" + a + ", " + b + ")";
			}
		}
		
		class Util {
			private Set<String> insert(String x, Collection<String> s) {
				final TreeSet<String> result = new TreeSet<>(s);
				result.add(x);
				return result;
			}
			private Set<String> intersection(Set<String> a, Collection<String> b) {
				final TreeSet<String> result = new TreeSet<>(a);
				result.retainAll(b);
				return result;
			}

		}
		final Util u = new Util();
		
		@SuppressWarnings("unchecked")
		final Set<String> empty = Collections.EMPTY_SET;
		
		final TreeSet<String> remaining = new TreeSet<>(preorder.getElements());
		final EditableLatticeSimple<String> alreadyEncountered = new EditableLatticeSimple<>();
		
		
		List<Cut> l = new LinkedList<>();
		l.add(new Cut(empty,empty));
		
		while (!remaining.isEmpty()) {
			final String x = remaining.pollLast();

			alreadyEncountered.addElement(x);
			for (String greater : preorder.getImmediatelyGreater(x)) {
				if (alreadyEncountered.getElements().contains(greater)) alreadyEncountered.setImmediatelyGreater(x, greater);
			}
			for (String lower : preorder.getImmediatelyLower(x)) {
				if (alreadyEncountered.getElements().contains(lower)) alreadyEncountered.setImmediatelyLower(x, lower);
			}
			final Collection<String> s = alreadyEncountered.collectAllLowerElements(x);
			assert (s.contains(x));
			s.remove(x);
			
			final Collection<String> t = alreadyEncountered.collectAllGreaterElements(x);
			assert (t.contains(x));
			t.remove(x);
			alreadyEncountered.removeElement(x);
			
			final List<Cut> lnew = new LinkedList<>();
			
			lnew.add(new Cut(u.insert(x,s), u.insert(x,t)));
			for (Cut cut : l) {
				final Set<String> c = cut.a;
				final Set<String> d = cut.b;
				// TODO: optimize
				if ( s.containsAll(c) && !t.containsAll(d)) lnew.add(new Cut(c, u.insert(x,d)));
				if (!s.containsAll(c) &&  t.containsAll(d)) lnew.add(new Cut(u.insert(x,c), d));
				if (!s.containsAll(c) && !t.containsAll(d)) {
					lnew.add(cut);
					
					Set<String> down = new TreeSet<>(alreadyEncountered.getElements());
					Set<String> dt = u.intersection(d,t);
					for(String y : dt) {
						down.retainAll(alreadyEncountered.collectAllLowerElements(y));
					}
					Set<String> up = new TreeSet<>(alreadyEncountered.getElements());
					Set<String> cs = u.intersection(c,s);
					for(String y : cs) {
						up.retainAll(alreadyEncountered.collectAllGreaterElements(y));
					}

					if (c.equals(down)) lnew.add(new Cut(u.insert(x,c), dt));
					if (d.equals(up))   lnew.add(new Cut(cs, u.insert(x,d)));
				}
			}
			l = lnew;
			
			alreadyEncountered.addElement(x);
			for (String greater : preorder.getImmediatelyGreater(x)) {
				if (alreadyEncountered.getElements().contains(greater)) alreadyEncountered.setImmediatelyGreater(x, greater);
			}
			for (String lower : preorder.getImmediatelyLower(x)) {
				if (alreadyEncountered.getElements().contains(lower)) alreadyEncountered.setImmediatelyLower(x, lower);
			}
		}

		IEditableLattice<Cut> cutCompletion = new EditableLatticeSimple<>();
		for (Cut cut : l) cutCompletion.addElement(cut);
		for (Cut x : l) {
			List<Cut> greaterThanX = new LinkedList<>();
			List<Cut> smallerThanX = new LinkedList<>();
			for (Cut y : l) {
				if (!x.equals(y)) {
					if (y.a.containsAll(x.a))
						greaterThanX.add(y);
					if (x.a.containsAll(y.a))
						smallerThanX.add(y);
				}
			}
			for (Cut y : greaterThanX) {
				boolean yisMinimal = true;

				// TODO: optimize
				for (Cut z : greaterThanX) {
					if ((y.a.containsAll(z.a)) && z!=y)
						yisMinimal = false;
				}
				if (yisMinimal) cutCompletion.setImmediatelyGreater(x, y);
			}

			for (Cut y : smallerThanX) {
				boolean yisMaximal = true;

				// TODO: optimize
				for (Cut z : smallerThanX) {
					if ((z.a.containsAll(y.a)) && z!=y) yisMaximal = false;
				}
				if (yisMaximal) cutCompletion.setImmediatelyLower(x, y);
			}
			
		}

		IEditableLattice<String> completion = new EditableLatticeSimple<String>();
		Map<Cut,String> reverse = new HashMap<>(l.size());
		int nextNew = 0;
		for (Cut cut : l) {
			boolean isNewElement = true;
			for (String x : preorder.getElements()) {
				Collection<String> xembedding = preorder.collectAllLowerElements(x);
				if (cut.a.equals(xembedding)) {
					assert isNewElement;
					reverse.put(cut, x);
					completion.addElement(x);
					isNewElement = false;
				}
			}
			if (isNewElement) {
				while (preorder.getElements().contains("newElement-" + nextNew)) nextNew++;
				final String x = "newElement-" + nextNew++;
				reverse.put(cut, x);
				completion.addElement(x);
			}
		}

		for (Cut x : l) {
			for (Cut y : cutCompletion.getImmediatelyGreater(x)) {
				completion.setImmediatelyGreater(reverse.get(x), reverse.get(y));
			}
			for (Cut y : cutCompletion.getImmediatelyLower(x)) {
				completion.setImmediatelyLower(reverse.get(x), reverse.get(y));
			}
		}
		return completion;
	}

}
