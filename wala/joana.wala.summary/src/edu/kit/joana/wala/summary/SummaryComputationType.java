/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

public enum SummaryComputationType {
	JOANA_CLASSIC(new SummaryComputer()),
	SIMON_PARALLEL_SCC(new SummaryComputer2());
	
	private final ISummaryComputer summaryComputer;
	
	private SummaryComputationType(ISummaryComputer summaryComputer) {
		this.summaryComputer = summaryComputer;
	}
	
	public ISummaryComputer getSummaryComputer() {
		return summaryComputer;
	}
}
