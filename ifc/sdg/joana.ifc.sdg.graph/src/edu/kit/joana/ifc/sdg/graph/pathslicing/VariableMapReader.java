/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.pathslicing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class VariableMapReader {
	private Map<Integer, Set<Integer>> def; // maps SDGNode -> set of modified local variables
	private Map<Integer, Set<Integer>> use; // maps SDGNode -> set of used local variables
	private Map<Integer, Set<Integer>> mod; // maps SDGNode -> set of modified references
	private Map<Integer, Set<Integer>> ref; // maps SDGNode -> set of used references
	private Map<Integer, Set<Integer>> pto; // maps references -> set of referenced objects

	public VariableMapReader(String sdgFile) throws IOException {
		ObjectInputStream defStream = new ObjectInputStream(new FileInputStream(new File(sdgFile+"_def")));
		ObjectInputStream useStream = new ObjectInputStream(new FileInputStream(new File(sdgFile+"_use")));
		ObjectInputStream modStream = new ObjectInputStream(new FileInputStream(new File(sdgFile+"_mod")));
		ObjectInputStream refStream = new ObjectInputStream(new FileInputStream(new File(sdgFile+"_ref")));
		ObjectInputStream ptoStream = new ObjectInputStream(new FileInputStream(new File(sdgFile+"_pto")));

		try {
			def = (Map<Integer, Set<Integer>>) defStream.readObject();
			use = (Map<Integer, Set<Integer>>) useStream.readObject();
			mod = (Map<Integer, Set<Integer>>) modStream.readObject();
			ref = (Map<Integer, Set<Integer>>) refStream.readObject();
			pto = (Map<Integer, Set<Integer>>) ptoStream.readObject();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		defStream.close();
		useStream.close();
		modStream.close();
		refStream.close();
		ptoStream.close();
	}

	public Set<Integer> getDef(int i) {
		Set<Integer> ret = def.get(i);
		if (ret == null) {
			ret = new HashSet<Integer>();
		}
		return ret;
	}

	public Set<Integer> getUse(int i) {
		Set<Integer> ret = use.get(i);
		if (ret == null) {
			ret = new HashSet<Integer>();
		}
		return ret;
	}

	public Set<Integer> getMod(int i) {
		Set<Integer> ret = mod.get(i);
		if (ret == null) {
			ret = new HashSet<Integer>();
		}
		return ret;
	}

	public Set<Integer> getRef(int i) {
		Set<Integer> ret = ref.get(i);
		if (ret == null) {
			ret = new HashSet<Integer>();
		}
		return ret;
	}

	public Set<Integer> getPto(int i) {
		Set<Integer> ret = pto.get(i);
		if (ret == null) {
			ret = new HashSet<Integer>();
		}
		return ret;
	}
}
