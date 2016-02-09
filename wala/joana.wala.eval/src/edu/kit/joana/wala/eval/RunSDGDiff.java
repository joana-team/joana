/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.IOException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Compares two SDG files.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class RunSDGDiff {

	public static class SDGData {
		final String file;
		int numNodes;
		int numEdges;
		int numParams;
		final SortedSet<String> methods = new TreeSet<>();
		
		public SDGData(final String file) {
			this.file = file;
		}

		public String toStringExtended() {
			final StringBuffer sb = new StringBuffer();
			sb.append("file: '" + file + "'\n");
			sb.append(numNodes + " nodes, " + numParams + " params (" + ((numParams * 100) / numNodes) + "%), "
					+ numEdges + " edges for " + methods.size() + " methods.\n");
			
			for (final String m : methods) {
				sb.append(m + "\n");
			}
			
			return sb.toString();
		}
		
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("file: '" + file + "'\n");
			sb.append(numNodes + " nodes, " + numParams + " params (" + ((numParams * 100) / numNodes) + "%), "
					+ numEdges + " edges for " + methods.size() + " methods.\n");
			
			return sb.toString();
		}
		
		public SortedSet<String> setMinus(final SDGData other) {
			final SortedSet<String> minus = new TreeSet<>();
			minus.addAll(methods);
			minus.removeAll(other.methods);
			return minus;
		}
	}
	
	public static void main(String[] args) throws IOException {
//		final String sdgFile1 = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jre14-jg-Series.pdg";
//		final String sdgFile2 = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeSeries_PtsType_Graph.pdg";
//		final String sdgFile1 = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jc-Purse.pdg";
//		final String sdgFile2 = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsType_Graph_Std.pdg";
		final String sdgFile1 = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jre14-jif-battleship.pdg";
//		final String sdgFile1 = "C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-0-1-cfa\\jre14-jif-battleship.pdg";
		final String sdgFile2 = "C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_Battleship_PtsInst_Graph.pdg";
		
		final SDGData sd1 = readIn(sdgFile1);
		System.out.println(sd1);
		final SDGData sd2 = readIn(sdgFile2);
		System.out.println(sd2);
		final SortedSet<String> sd1minus2 = sd1.setMinus(sd2);
		System.out.println("methods not in " + sd2.file);
		print(sd1minus2);
		
		System.out.println();
		
		final SortedSet<String> sd2minus1 = sd2.setMinus(sd1);
		System.out.println("methods not in " + sd1.file);
		print(sd2minus1);
	}

	private static SDGData readIn(final String file) throws IOException {
		final SDG sdg = SDG.readFromAndUseLessHeap(file);

		final SDGData data = new SDGData(file);
		data.numNodes = sdg.vertexSet().size();
		data.numEdges = sdg.edgeSet().size();
		
		for (final SDGNode n : sdg.vertexSet()) {
			switch (n.kind) {
			case ACTUAL_IN:
			case ACTUAL_OUT:
			case FORMAL_IN:
			case FORMAL_OUT:
				data.numParams++;
				break;
			case ENTRY:
				if (!n.getLabel().equals("*Start*")) {
					// ignore artificial start node
					data.methods.add(n.getLabel().replace("$", "."));
				}
				break;
			default: //nop
			}
		}
		
		return data;
	}
	
	private static void print(final Set<String> set) {
		for (final String s : set) {
			System.out.println(s);
		}
	}
	
}
