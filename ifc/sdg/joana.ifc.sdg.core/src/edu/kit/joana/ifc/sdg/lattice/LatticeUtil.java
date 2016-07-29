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
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.lattice.impl.StaticLatticeBitset;


/**
 * Utility class implementing some common operations on lattices. Also provides
 * functionality to load lattices from Strings and InputStreams and to compile
 * a lattice to a immutable but efficient bitset representation.
 *
 */
public class LatticeUtil {
	private static final String TOKEN_LESS = "<=";
	private static final String TOKEN_COMMENT = "#";

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
	 * Transitive greater elements of a given lattice element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the lattice.
	 * @param s
	 *            the element for which to collect all greater elements.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all transitive greater elements of <code>s</code>
	 * 
	 * @deprecated use {@link ILatticeOperations#collectAllGreaterElements(Object)} instead, which may have an optimized implementation
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllGreaterElements(ElementType s, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> greaterElements = new HashSet<ElementType>();
		greaterElements.add(s);
		boolean changed = false;
		do {
			changed = false;
			Collection<ElementType> toAdd = new ArrayList<ElementType>();
			for (ElementType e : greaterElements)
				for (ElementType p : ops.getImmediatelyGreater(e))
					if (!greaterElements.contains(p)) {
						toAdd.add(p);
						changed = true;
					}
			greaterElements.addAll(toAdd);
		} while (changed);
		return greaterElements;
	}

	/**
	 * Transitive greater elements of a given lattice element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the lattice.
	 * @param s
	 *            the element for which to collect all greater elements.
	 * @param lat lattice which provides the partial order
	 *
	 * @return all transitive greater elements of <code>s</code>
	 * 
	 * @deprecated use {@link IStaticLattice#collectAllGreaterElements(Object)} instead, which may have an optimized implementation
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllGreaterElements(ElementType s, IStaticLattice<ElementType> lat) {
		Collection<ElementType> greaterElements = new HashSet<ElementType>();

		for (ElementType e : lat.getElements()) {

			if (lat.greatestLowerBound(s, e).equals(s)) {
				// inf(s,e) = s ==> s <= e
				greaterElements.add(e);
			}
		}

		return greaterElements;
	}

	/**
	 * Does a bottom-up sweep of the graph and returns all elements not
	 * reachable from the unique bottom. This function requires the graph to
	 * have a unique bottom element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param inElements
	 *            the elements of the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all elements that could not be reached during the bottom-up
	 *         sweep, or <code>null</code> if all elements could be reached.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique bottom element
	 * 
	 * @deprecated use {@link ILatticeOperations#findUnreachableFromBottom(Collection)} instead, which may have an optimized implementation
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findUnreachableFromBottom(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) throws InvalidLatticeException {
		ArrayList<ElementType> elements = new ArrayList<ElementType>(inElements);
		boolean seen[] = new boolean[elements.size()];
		Collection<ElementType> bottoms = findBottomElements(elements, ops);
		if (bottoms.size() != 1)
			throw new InvalidLatticeException("Graph does not have a unique bottom");
		markReachableUp(elements, bottoms.iterator().next(), seen, ops);
		ArrayList<ElementType> ret = new ArrayList<ElementType>();
		for (int i = 0; i < seen.length; i++)
			if (!seen[i])
				ret.add(elements.get(i));
		if (ret.isEmpty())
			return null;
		return ret;
	}

	private static <ElementType> void markReachableUp(ArrayList<ElementType> elements, ElementType current, boolean seen[], ILatticeOperations<ElementType> ops) {
		int currentIndex = elements.indexOf(current);
		if (seen[currentIndex])
			return;
		seen[currentIndex] = true;
		for (ElementType parent : ops.getImmediatelyGreater(current))
			markReachableUp(elements, parent, seen, ops);
	}

	/**
	 * Does a top-down sweep of the graph and returns all elements not reachable
	 * from the unique top. This function requires the graph to have a unique
	 * top element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param inElements
	 *            the elements of the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all elements that could not be reached during the top-down sweep,
	 *         or <code>null</code> if all elements could be reached.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique top element
	 * 
	 * @deprecated use {@link ILatticeOperations#findUnreachableFromTop(Collection)} instead, which may have an optimized implementation
	 * 
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findUnreachableFromTop(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) throws InvalidLatticeException {
		ArrayList<ElementType> elements = new ArrayList<ElementType>(inElements);
		boolean seen[] = new boolean[elements.size()];
		Collection<ElementType> tops = findTopElements(elements, ops);
		if (tops.size() != 1)
			throw new InvalidLatticeException("Graph does not have a unique top");
		markReachableDown(elements, tops.iterator().next(), seen, ops);
		ArrayList<ElementType> ret = new ArrayList<ElementType>();
		for (int i = 0; i < seen.length; i++)
			if (!seen[i])
				ret.add(elements.get(i));
		if(ret.isEmpty())
			return null;
		return ret;
	}

	private static <ElementType> void markReachableDown(ArrayList<ElementType> elements, ElementType current, boolean seen[], ILatticeOperations<ElementType> ops) {
		int currentIndex = elements.indexOf(current);
		if (seen[currentIndex])
			return;
		seen[currentIndex] = true;
		for (ElementType child : ops.getImmediatelyLower(current))
			markReachableDown(elements, child, seen, ops);
	}

	/**
	 * Transitive lower elements of a given lattice element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the lattice.
	 * @param s
	 *            the element for which to collect all lower elements.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all transitive lower elements of <code>s</code>
	 * 
	 * @deprecated use {@link ILatticeOperations#collectAllLowerElements(Object))} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllLowerElements(ElementType s, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> lowerElements = new HashSet<ElementType>();
		lowerElements.add(s);
		boolean changed = false;
		do {
			changed = false;
			Collection<ElementType> toAdd = new ArrayList<ElementType>();
			for (ElementType e : lowerElements)
				for (ElementType p : ops.getImmediatelyLower(e))
					if (!lowerElements.contains(p)) {
						toAdd.add(p);
						changed = true;
					}
			lowerElements.addAll(toAdd);
		} while (changed);
		return lowerElements;
	}

	/**
	 * Finds all least uppper bounds (lub) for two given elements.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param s
	 *            the first parameter for the lub operation.
	 * @param t
	 *            the second parameter for the lub operation.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all least upper bounds of the elements <code>s</code> and
	 *         <code>t</code>
	 * 
	 * @deprecated use {@link ILatticeOperations#leastUpperBounds(Object, Object)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> leastUpperBounds(ElementType s, ElementType t, ILatticeOperations<ElementType> ops) {
		assert s != null;
		assert t != null;
		assert ops != null;

		// GBs = {x in elements | x >= s}
		Collection<ElementType> gbs = collectAllGreaterElements(s, ops);

		// GBt = {x in elements | x >= t}
		Collection<ElementType> gbt = collectAllGreaterElements(t, ops);

		// CGB = GBs intersect GBt
		List<ElementType> cgb = new ArrayList<ElementType>();
		for (ElementType a : gbs)
			if (gbt.contains(a))
				cgb.add(a);

		// return min(CLB)
		return min(cgb, ops);
	}

	private static <ElementType> Collection<ElementType> min(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
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
	 * Finds all greatest lower bounds (glb) for two given elements.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param s
	 *            the first parameter for the glb operation.
	 * @param t
	 *            the second parameter for the glb operation.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all greatest lower bounds of the elements <code>s</code> and
	 *         <code>t</code>
	 * 
	 * @deprecated use {@link ILatticeOperations#greatestLowerBounds(Object, Object)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> greatestLowerBounds(ElementType s, ElementType t, ILatticeOperations<ElementType> ops) {
		assert s != null;
		assert t != null;
		assert ops != null;

		// LBs = {x in elements | x <= s}
		Collection<ElementType> lbs = collectAllLowerElements(s, ops);

		// LBt = {x in elements | x <= t}
		Collection<ElementType> lbt = collectAllLowerElements(t, ops);

		// CLB = LBs intersect LBt
		List<ElementType> clb = new ArrayList<ElementType>();
		for (ElementType a : lbs)
			if (lbt.contains(a))
				clb.add(a);

		// return max(CLB)
		return max(clb, ops);
	}

	private static <ElementType> Collection<ElementType> max(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
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
	 * Finds all elements that do not have a predecessor.
	 *
	 * @param <ElementType>
	 *            the type of the elements in the graph.
	 * @param inElements
	 *            the elements of the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all elements in <code>inElements</code> that do not have a
	 *         predecessor.
	 * 
	 * @deprecated use {@link ILatticeOperations#findTopElements(Collection)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findTopElements(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> tops = new ArrayList<ElementType>();

		for (ElementType e : inElements) {
			if (ops.getImmediatelyGreater(e).size() == 0)
				tops.add(e);
		}
		return tops;
	}

	/**
	 * Finds all elements that do not have a successor.
	 *
	 * @param <ElementType>
	 *            the type of the elements in the graph.
	 * @param inElements
	 *            the elements of the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the
	 *            basic lattice operations.
	 * @return all elements in <code>inElements</code> that do not have a
	 *         successor.
	 * 
	 * @deprecated use {@link ILatticeOperations#findBottomElements(Collection)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> findBottomElements(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> bottoms = new ArrayList<ElementType>();

		for (ElementType e : inElements) {
			if (ops.getImmediatelyLower(e).size() == 0)
				bottoms.add(e);
		}
		return bottoms;
	}

	/**
	 * @deprecated use {@link IStaticLattice#isLeq(Object, Object)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectNoninterferingElements(ElementType s, IStaticLattice<ElementType> lat) {
		Collection<ElementType> noninterferingElements = new HashSet<ElementType>();

		for (ElementType e : lat.getElements()) {
			if (!lat.leastUpperBound(s, e).equals(s)) {
				noninterferingElements.add(e);
			}
		}

		return noninterferingElements;
	}

	/**
	 * @deprecated use {@link IStaticLattice#isLeq(Object, Object)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> Collection<ElementType> collectAllLowerElements(ElementType s, IStaticLattice<ElementType> lat) {
		Collection<ElementType> lowerElements = new HashSet<ElementType>();

		for (ElementType e : lat.getElements()) {
			if (lat.leastUpperBound(s, e).equals(s)) {
				lowerElements.add(e);
			}
		}

		return lowerElements;
	}

	/**
	 * Given a lattice l and two elements l1 and l2 or l's carrier set, returns whether l1
	 * is lower than or equal to l2. Note that l1 and l2 really have to be elements of l's carrier
	 * set, otherwise the correct functionality of this method cannot be guaranteed.
	 * @param l the lattice in which the "is-lower-than-or-equal-to" relation is to be determined
	 * @param l1 an element of the given lattice
	 * @param l2 another element of the given lattice
	 * @return {@code true} if the first given element is lower than or equal to the second given
	 * element.
	 * 
	 * @deprecated use {@link IStaticLattice#isLeq(Object, Object)} instead, which may have an optimized implementation
	 */
	@Deprecated
	public static <ElementType> boolean isLeq(IStaticLattice<ElementType> l, ElementType l1, ElementType l2) {
		return l.leastUpperBound(l1, l2).equals(l2);
	}
}
