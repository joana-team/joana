/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import edu.kit.joana.wala.eval.RunSummaryComputation.Task;
import edu.kit.joana.wala.eval.RunSummaryComputation.Variant;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class RunSingleFileSummaryComputation {

	public static void main(String argv[]) {
		final Task t = new Task();
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsInst_Graph_Std.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsInst_Graph_Std.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jc-Purse.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsInst_Graph_StdNoOpt.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsInst_Graph_StdNoOpt.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jc-Purse.pdg";

//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jc-Purse.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-0-cfa\\jc-Purse.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsObj_Graph_Std.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jre14-jif-battleship.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jre14-jif-battleship.pdg";
		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_RS3CloudStorage_PtsType_Graph.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_Battleship_PtsType_Graph.pdg";
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_Battleship_PtsInst_Graph.pdg";
		
//		t.filename = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeSparseMatmult_PtsType_Graph.pdg";
		t.logname = "RunSingleFileSummaryComputation.log";
		final int numRuns = 1;
		final int timeOut = -1;
		
		RunSummaryComputation.work(t, Variant.OLD, numRuns, timeOut);
	}
	
}
