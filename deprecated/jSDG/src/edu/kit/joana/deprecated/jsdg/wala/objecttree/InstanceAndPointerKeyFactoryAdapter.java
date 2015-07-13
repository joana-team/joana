/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala.objecttree;


import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class InstanceAndPointerKeyFactoryAdapter extends AbstractKey2Origin
		implements InstanceKeyFactory, PointerKeyFactory {

	private final PointerKeyFactory pkFact;
	private final InstanceKeyFactory ikFact;

	public InstanceAndPointerKeyFactoryAdapter(InstanceKeyFactory ikFact,
			PointerKeyFactory pkFact) {
		this.pkFact = pkFact;
		this.ikFact = ikFact;
	}

	public InstanceKey getInstanceKeyForAllocation(CGNode node,
			NewSiteReference allocation) {
		InstanceKey iKey = ikFact.getInstanceKeyForAllocation(node, allocation);

		InstanceKeyOrigin origin = findOrigin(node, allocation);
		addOrigin(iKey, origin);

		/* debug stuff
		TypeReference dclt = allocation.getDeclaredType();
		if (dclt.isClassType() && node.getMethod().isClinit()) {
			TypeReference inner =  dclt.getInnermostElementType();
			Atom innerName = inner.getName().getClassName();
			Atom cname = Atom.findOrCreateAsciiAtom("A");

			if (innerName == cname) {
				Analyzer.IK = iKey;
			}
		}
		*/
		return iKey;
	}

	public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
		InstanceKey iKey = ikFact.getInstanceKeyForMetadataObject(obj, objType);

		InstanceKeyOrigin origin = findOrigin(objType);
		addOrigin(iKey, origin);

		return iKey;
	}

	public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
		return ikFact.getInstanceKeyForConstant(type, S);
	}

	public InstanceKey getInstanceKeyForMultiNewArray(CGNode node,
			NewSiteReference allocation, int dim) {
		return ikFact.getInstanceKeyForMultiNewArray(node, allocation, dim);
	}

	public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr,
			TypeReference type) {
		return ikFact.getInstanceKeyForPEI(node, instr, type);
	}

	public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node,
			int valueNumber, TypeFilter filter) {
		return pkFact.getFilteredPointerKeyForLocal(node, valueNumber, filter);
	}

	public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
		PointerKey pKey = pkFact.getPointerKeyForArrayContents(I);

		TypeReference tRef = I.getConcreteType().getReference();
		ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);
		PointerKeyOrigin origin = findOrigin(field);
		addOrigin(pKey, origin);

		return pKey;
	}

	public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
		return pkFact.getPointerKeyForExceptionalReturnValue(node);
	}

	public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField ifield) {
		PointerKey pKey = pkFact.getPointerKeyForInstanceField(I, ifield);

		ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
		PointerKeyOrigin origin = findOrigin(I, field);
		addOrigin(pKey, origin);

		return pKey;
	}

	public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
		PointerKey pKey = pkFact.getPointerKeyForLocal(node, valueNumber);

		PointerKeyOrigin origin = findOrigin(node, valueNumber);
		addOrigin(pKey, origin);

		return pKey;
	}

	public PointerKey getPointerKeyForReturnValue(CGNode node) {
		return pkFact.getPointerKeyForReturnValue(node);
	}

	public PointerKey getPointerKeyForStaticField(IField ifield) {
		PointerKey pKey = pkFact.getPointerKeyForStaticField(ifield);

		ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
		PointerKeyOrigin origin = findOrigin(field);
		addOrigin(pKey, origin);

		return pKey;
	}

}
