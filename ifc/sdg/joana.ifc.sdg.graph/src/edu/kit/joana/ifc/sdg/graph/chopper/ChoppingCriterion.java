/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A ChoppingCriterion encapsulates the source- and target criterion of a chop.
 *
 * @author giffhorn
 * @see Chopper
 */
public class ChoppingCriterion {
	/** the source of the chop */
	private final Collection<SDGNode> sourceSet;

	/** the target of the chop */
	private final Collection<SDGNode> targetSet;

	public ChoppingCriterion() {
		sourceSet = new HashSet<SDGNode>();
		targetSet = new HashSet<SDGNode>();
	}

	/**
	 * Instantiates a ChoppingCriterion with the given nodes.
	 * Further nodes can be added later.
	 */
	public ChoppingCriterion(SDGNode source, SDGNode target) {
		sourceSet = new HashSet<SDGNode>();
		targetSet = new HashSet<SDGNode>();
		sourceSet.add(source);
		targetSet.add(target);
	}

	/**
	 * Instantiates a ChoppingCriterion with the given sets of nodes.
	 * Further nodes can be added later.
	 */
	public ChoppingCriterion(Collection<SDGNode> sourceSet, Collection<SDGNode> targetSet) {
		this.sourceSet = sourceSet;
		this.targetSet = targetSet;
	}

	/**
	 * Adds another node to the source criterion.
	 * @param source  Should not be null.
	 */
	public void addSource(SDGNode source) {
		sourceSet.add(source);
	}

	/**
	 * Adds another node to the target criterion.
	 * @param source  Should not be null.
	 */
	public void addTarget(SDGNode target) {
		targetSet.add(target);
	}

	/**
	 * @return the source criterion.
	 */
	public Collection<SDGNode> getSourceSet() {
		return sourceSet;
	}

	/**
	 * @return the target criterion.
	 */
	public Collection<SDGNode> getTargetSet() {
		return targetSet;
	}

	/**
	 * @return `true', if the criterion is same-level.
	 * @throws InvalidCriterionException, if one of the criterion sets is null.
	 */
	public boolean isSameLevelCriterion()
	throws InvalidCriterionException {
		return Chopper.testSameLevelSetCriteria(sourceSet, targetSet);
	}

	/**
	 * @return a textual representation.
	 */
    public String toString() {
        return "("+sourceSet+", "+targetSet+")";
    }
}
