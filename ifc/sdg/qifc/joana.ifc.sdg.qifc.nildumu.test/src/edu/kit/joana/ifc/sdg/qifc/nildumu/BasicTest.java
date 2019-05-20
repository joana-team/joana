/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import edu.kit.joana.ifc.sdg.qifc.nildumu.prog.Basic;

class BasicTest {
	public static void main(String[] args) {
		Program program = new Program(new Builder().entry(Basic.class).buildOrDie());
		Context context = program.analyze();
		context.printLeakages();
	}
}
