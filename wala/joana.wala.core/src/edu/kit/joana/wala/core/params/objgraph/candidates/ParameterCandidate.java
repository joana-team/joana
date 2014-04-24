/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.params.objgraph.TVL;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface ParameterCandidate {

	TVL.V isArray();
	TVL.V isStatic();
	TVL.V isRoot();
	TVL.V isPrimitive();
	boolean isMerged();
	boolean isUnique();
	// other.fieldpts and this.basePts share common elements
	boolean isReachableFrom(ParameterCandidate other);
	boolean isBaseAliased(OrdinalSet<InstanceKey> pts);
	boolean isFieldAliased(OrdinalSet<InstanceKey> other);
	boolean isReferenceToField(OrdinalSet<InstanceKey> pts, ParameterField otherField);
	boolean isReferenceToAnyField(OrdinalSet<ParameterField> otherField);
	OrdinalSet<UniqueParameterCandidate> getUniques();
	boolean isMustAliased(ParameterCandidate pc);
	boolean isMayAliased(ParameterCandidate pc);
	TypeReference getType();
	int getBytecodeIndex();
	String getBytecodeName();
	OrdinalSet<ParameterField> getFields();
	OrdinalSet<InstanceKey> getBasePointsTo();
	OrdinalSet<InstanceKey> getFieldPointsTo();

}
