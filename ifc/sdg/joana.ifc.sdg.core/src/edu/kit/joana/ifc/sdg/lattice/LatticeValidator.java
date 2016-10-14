/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implements functionality to test a graph for required lattice properties.
 *
 */
public class LatticeValidator {

	private LatticeValidator() { }

	/**
	 * Finds a cycle in a graph. This function requires the graph to have a
	 * unique top element.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param inElements
	 *            the elements of the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the basic
	 *            lattice operations.
	 * @return the elements contained in the first found cycle, or
	 *         <code>null</code> if no cycle was found.
	 */
	public static <ElementType> Collection<ElementType> findCycle(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) {
		Collection<ElementType> tops = ops.findTopElements(inElements);
		if (tops.size() != 1)
			throw new InvalidLatticeException("Cycle detection requires a graph with a unique top element");

		ElementType top = tops.iterator().next();
		boolean onCurrentPath[] = new boolean[inElements.size()];

		return findCycleWalk(new ArrayList<ElementType>(inElements), top, ops, onCurrentPath);
	}

	private static <ElementType> ArrayList<ElementType> findCycleWalk(ArrayList<ElementType> inElements, ElementType current, ILatticeOperations<ElementType> ops, boolean onCurrentPath[]) {
		int currentIndex = inElements.indexOf(current);

		if (onCurrentPath[currentIndex]) {
			ArrayList<ElementType> ret = new ArrayList<ElementType>();
			ret.add(current);
			return ret;
		}

		onCurrentPath[currentIndex] = true;
		for (ElementType child : ops.getImmediatelyLower(current)) {
			ArrayList<ElementType> ret = findCycleWalk(inElements, child, ops, onCurrentPath);
			if (ret != null) {
				if (ret.size() == 1 || !ret.get(0).equals(ret.get(ret.size() - 1)))
					ret.add(current);
				return ret;
			}
		}
		onCurrentPath[currentIndex] = false;
		return null;
	}

	/**
	 * This function performs a lattice validation as proposed by G.M. Ziegler
	 * in the paper "A new local criterion for the lattice property". A
	 * bottom-up sweep of the graph is performed, and criterion 1 from the paper
	 * is used to validate the lattice integrity. This function requires a
	 * unique bottom element and a graph where all nodes can be reached via a
	 * bottom-up sweep.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param inElements
	 *            the elements contained in the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the basic
	 *            lattice operations.
	 * @return two elements for which not least upper bound could be found if
	 *         the graph does not represent a valid lattice, or
	 *         <code>null</code> if the validation succeeded.
	 * @throws InvalidLatticeException
	 *             if no unique bottom element could be found.
	 */
	public static <ElementType> Collection<ElementType> validateBottomUp(Collection<ElementType> inElements, ILatticeOperations<ElementType> ops) throws InvalidLatticeException {
		Collection<ElementType> bottoms = ops.findBottomElements(inElements);
		if (bottoms.size() != 1)
			throw new InvalidLatticeException("Lattice does not have a unique bottom element");

		ElementType current = bottoms.iterator().next();
		return checkUpwards(new ArrayList<ElementType>(ops.getImmediatelyGreater(current)), ops);
	}

	private static <ElementType> Collection<ElementType> checkUpwards(List<ElementType> siblings, ILatticeOperations<ElementType> ops) {
		if (siblings.isEmpty())
			return null;

		for (int i = 0; i < siblings.size(); i++) {
			for (int j = i; j < siblings.size(); j++) {
				Collection<ElementType> lubs = ops.leastUpperBounds(siblings.get(i), siblings.get(j));
				if (lubs.size() != 1) {
					ArrayList<ElementType> ret = new ArrayList<ElementType>();
					ret.add(siblings.get(i));
					ret.add(siblings.get(j));
					return ret;
				}
			}
			Collection<ElementType> ret = checkUpwards(new ArrayList<ElementType>(ops.getImmediatelyGreater(siblings.get(i))), ops);
			if (ret != null)
				return ret;
		}

		return null;
	}

	/**
	 * Performs a incremental full validation of the lattice integrity for a
	 * given graph. This is done by successively performing the following tests:
	 * find a unique top element, find a unique bottom element, ensure the graph
	 * contains no cycles, ensure all elements can be reached from the bottom
	 * element, and finally use Ziegler's criterion as implemented by
	 * <code>validateBottomUp()</code>.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param elements
	 *            the elements contained in the graph.
	 * @param ops
	 *            a <code>ILatticeOperations</code> object providing the basic
	 *            lattice operations.
	 * @return a <code>LatticeProblemDescription</code> object describing a
	 *         problem encountered while validating the lattice, or
	 *         <code>null</code> if the provided graph successfully passed
	 *         lattice validation.
	 */
	public static <ElementType> LatticeProblemDescription<ElementType> validateIncremental(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
		if(elements.size() == 0)
			return new LatticeProblemDescription<ElementType>("Graph is empty");

		Collection<ElementType> tops = ops.findTopElements(elements);
		if (tops.size() == 0)
			return new LatticeProblemDescription<ElementType>("Graph does not have a top element");
		if (tops.size() > 1)
			return new LatticeProblemDescription<ElementType>("Graph has multiple top elements", tops);

		Collection<ElementType> bottoms = ops.findBottomElements(elements);
		if (bottoms.size() == 0)
			return new LatticeProblemDescription<ElementType>("Graph does not have a bottom element");
		if (bottoms.size() > 1)
			return new LatticeProblemDescription<ElementType>("Graph has multiple bottom elements", bottoms);

		Collection<ElementType> cycle = findCycle(elements, ops);
		if (cycle != null)
			return new LatticeProblemDescription<ElementType>("Graph has a cycle", cycle);

		Collection<ElementType> unreachable = ops.findUnreachableFromBottom(elements);
		if (unreachable != null)
			return new LatticeProblemDescription<ElementType>("Some elements can not be reached from bottom", unreachable);

		Collection<ElementType> problems = validateBottomUp(elements, ops);
		if (problems != null)
			return new LatticeProblemDescription<ElementType>("Nodes don't have a unique least upper bound", problems);

		return null;
	}

	/**
	 * Performs a incremental full validation of the lattice integrity for a
	 * given graph. This is done by successively performing the following tests:
	 * find a unique top element, find a unique bottom element, ensure the graph
	 * contains no cycles, ensure all elements can be reached from the bottom
	 * element, and finally use Ziegler's criterion as implemented by
	 * <code>validateBottomUp()</code>.
	 *
	 * @param <ElementType>
	 *            the type of the elements contained in the graph.
	 * @param lattice
	 *            the lattice to validate.
	 * @return a <code>LatticeProblemDescription</code> object describing a
	 *         problem encountered while validating the lattice, or
	 *         <code>null</code> if the provided graph successfully passed
	 *         lattice validation.
	 */
	public static <ElementType> LatticeProblemDescription<ElementType> validateIncremental(ILatticeOperations<ElementType> lattice) {
		return validateIncremental(lattice.getElements(), lattice);
	}
}
