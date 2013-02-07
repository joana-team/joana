/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import java.io.File;
import java.io.IOException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.util.Pair;

/**
 * Provides a little command-line interface for comparing two SDGs. The user
 * provides the names of two files containing SDGs and statistics are printed,
 * how the method-wise numbers of nodes and edges differ.
 * 
 * @author Martin Mohr
 */
public final class CompareSDGs {

	private CompareSDGs() {}
	
	public static final void main(String[] args) throws IOException {
		if (args.length == 2) {
			File fSDG1 = new File(args[0]);
			File fSDG2 = new File(args[1]);
			if (fSDG1.exists() && fSDG2.exists()) {
				SDG sdg1 = SDG.readFrom(fSDG1.getPath());
				SDG sdg2 = SDG.readFrom(fSDG2.getPath());
				compareSDGsAndPrintStatistics(new NamedSDG(sdg1, fSDG1.getName()), new NamedSDG(sdg2, fSDG1.getName()));
			} else {
				System.out.println(String.format("Either '%s' or '%s' does not exist.", args[0], args[1]));
			}
		} else {
			System.out.println("Provide two file names!");
		}
	}
	

	private static final void compareSDGsAndPrintStatistics(NamedSDG sdg1, NamedSDG sdg2) {
		GraphStats gs1 = GraphStats.computeFrom(sdg1.getSDG());
		GraphStats gs2 = GraphStats.computeFrom(sdg2.getSDG());
		GraphStats gsDiff = GraphStats.difference(gs1, gs2);
		System.out.println(String.format("Graph stat differences between %s and %s:", sdg1.getName(), sdg2.getName()));
		for (Pair<String, MethodStats> e : gsDiff) {
			System.out.println(e.getFirst() + ": " + e.getSecond().getNumberOfNodes() + " nodes, "
					+ e.getSecond().getNumberOfEdges() + " edges");
		}
	}

}

class NamedSDG {
	private final Pair<SDG, String> data;
	
	public NamedSDG(SDG sdg, String name) {
		this.data = Pair.pair(sdg, name);
	}
	
	public SDG getSDG() {
		return data.getFirst();
	}
	
	public String getName() {
		return data.getSecond();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NamedSDG)) {
			return false;
		}
		NamedSDG other = (NamedSDG) obj;
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else if (!data.equals(other.data)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SDG with name " + getName();
	}
}
