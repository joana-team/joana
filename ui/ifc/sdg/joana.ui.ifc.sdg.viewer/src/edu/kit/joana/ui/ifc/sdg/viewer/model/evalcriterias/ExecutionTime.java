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

public class ExecutionTime implements EvaluationCriteria {

	private long time;

	@Override
	public void executeBefore(Run r) {
		time = System.currentTimeMillis();
	}

	@Override
	public void executeAfter() {
		time = System.currentTimeMillis() - time;
	}

	@Override
	public String getResult() {
		return "" + time;
	}

	@Override
	public String getName() {
		return "Execution Time";
	}

}
