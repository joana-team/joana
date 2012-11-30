/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model.evalcriterias;

import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationCriteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;

public class NodeCount implements EvaluationCriteria {

	private int nodes;
	private Run r;

	@Override
	public void executeBefore(Run r) {
		this.r = r;
	}

	@Override
	public void executeAfter() {
		nodes = r.getResult().keySet().size();
	}

	@Override
	public String getResult() {
		return "" + nodes;
	}

	@Override
	public String getName() {
		return "Number of Nodes";
	}

}
