/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala.objecttree;

import java.util.Set;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IKey2Origin {

	public static abstract class InstanceKeyOrigin {

		public static enum Type {ALLOCATION, PEI, CLASS};

		public abstract ProgramCounter getCounter();
		public abstract CGNode getNode();
		public abstract NewSiteReference getNsRef();
		public abstract TypeReference getTRef();
		public abstract Type getType();

	}

	public static abstract class PointerKeyOrigin {

		public static enum Type {STATIC_FIELD, INSTANCE_FIELD, ARRAY_FIELD, SSA_VAR}

		public abstract ParameterField getField();
		public abstract InstanceKey getIKey();
		public abstract Type getType();
		public abstract CGNode getNode();
		public abstract int getSSA();

	}

	public Set<InstanceKeyOrigin> getOrigin(InstanceKey iKey);

	public Set<PointerKeyOrigin> getOrigin(PointerKey pKey);

	public InstanceKeyOrigin findOrigin(CGNode node, NewSiteReference nsRef);
	public InstanceKeyOrigin findOrigin(CGNode node, ProgramCounter counter, TypeReference tRef);
	public InstanceKeyOrigin findOrigin(TypeReference tRef);

	public PointerKeyOrigin findOrigin(InstanceKey iKey, ParameterField field);
	public PointerKeyOrigin findOrigin(ParameterField field);
	public PointerKeyOrigin findOrigin(CGNode node, int ssaVar);


}
