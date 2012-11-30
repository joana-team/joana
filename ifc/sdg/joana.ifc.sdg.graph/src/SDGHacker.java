/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;



public class SDGHacker {
	public static class ProcData {
		String source;
		int entry;
		int entryLine;
		int exitLine;
		int[][] calls;

		ProcData(String s, int en, int enl, int exl, int[][] c) {
			source = s;
			entry = en;
			entryLine = enl;
			exitLine = exl;
			calls = c;
		}
	}


	public static void main(String[] args) throws Exception {
		/* Config */
//		String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.ConcPasswordFile.pdg";
//		String newName = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.ConcPasswordFileJoana.pdg";
//		ProcData[] procedureData = {
//				new ProcData("tests/ConcPasswordFile.java", 172, 28, 29, new int[][]{{176, 29}}),
//				new ProcData("tests/ConcPasswordFile.java", 89, 8, 19, new int[][]{{93, 12}, {97, 12}}),
//				new ProcData("tests/ConcPasswordFile.java", 4, 22, 25, new int[][]{{7, 23}, {8, 24}, {11, 25}})
//		};
		String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.ProbPasswordFile.pdg";
		String newName = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.ProbPasswordFileJoana.pdg";
		ProcData[] procedureData = {
				new ProcData("tests/ProbPasswordFile.java", 114, 8, 19, new int[][]{{118, 12}, {122, 12}}),
				new ProcData("tests/ProbPasswordFile.java", 4, 22, 27, new int[][]{{7, 23}, {10, 26}, {14, 27}, {15, 27}}),
				new ProcData("tests/ProbPasswordFile.java", 197, 30, 41, new int[][]{{211, 41}, {202, 34}, {204, 34}})
		};

		/* generic part */
		SDG g = SDG.readFrom(name);

		for (ProcData pd : procedureData) {
			SDGNode entry = g.getNode(pd.entry);
			Collection<SDGNode> formal = g.getParametersFor(entry);

			for (SDGNode n : formal) {
				n.setSource(pd.source);

				if (n.getKind() == SDGNode.Kind.FORMAL_IN || n.getKind() == SDGNode.Kind.ENTRY) {
					n.setLine(pd.entryLine, pd.entryLine);

				} else if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
					n.setLine(pd.exitLine, pd.exitLine);
				}
			}

			for (int c = 0; c < pd.calls.length; c++) {
				int callID = pd.calls[c][0];
				System.out.println("call: "+callID+", line: "+pd.calls[c][1]);
				SDGNode call = g.getNode(callID);
				Collection<SDGNode> actual = g.getParametersFor(call);

				for (SDGNode n : actual) {
					n.setSource(pd.source);
					n.setLine(pd.calls[c][1], pd.calls[c][1]);
				}
			}
		}

		/* store SDG */
		PrintWriter pw = new PrintWriter(new FileWriter(new File(newName)));
		SDGSerializer.toPDGFormat(g, pw);
	}
}
