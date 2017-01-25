/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Node for the start of a catch block.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class CatchNode extends NormalNode {

	private int basicBlock;
	private int val;

	private Set<TypeReference> catches;

	CatchNode(int id, int basicBlock, int val) {
		super(id);
		this.basicBlock = basicBlock;
		this.catches = HashSetFactory.make();
	}

	public int getBasicBlockNr() {
		return this.basicBlock;
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitCatch(this);
	}

	public String getLabel() {
		String str = "v" + val + " = catch ";
		for (TypeReference tref : catches) {
			str += Util.typeName(tref.getName()) + " ";
		}

		return str;
	}

	public void setCatches(Iterator<? extends TypeReference> ctypes) {
		while (ctypes != null && ctypes.hasNext()) {
			TypeReference tref = ctypes.next();
			catches.add(tref);
		}
	}

	public final boolean isCatchesSet() {
		return !catches.isEmpty();
	}
}
