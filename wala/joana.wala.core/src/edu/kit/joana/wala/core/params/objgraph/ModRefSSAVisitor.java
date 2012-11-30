/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefSSAVisitor extends SSAInstruction.Visitor {

	public interface CandidateConsumer {
		public void addModCandidate(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts);
		public void addRefCandidate(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts);
	}

	public interface PointsTo {
		public OrdinalSet<InstanceKey> getPointsTo(int ssaVar);
	}

	private final CandidateConsumer consumer;
	private final ParameterFieldFactory params;
	private final PointsTo pts;
	private final IClassHierarchy cha;
	private final boolean doStaticFields;

	public ModRefSSAVisitor(final CandidateConsumer consumer, final ParameterFieldFactory params,
			final PointsTo pts, final IClassHierarchy cha, final boolean doStaticFields) {
		this.consumer = consumer;
		this.params = params;
		this.pts = pts;
		this.cha = cha;
		this.doStaticFields = doStaticFields;
	}

	@Override
	public void visitArrayLoad(final SSAArrayLoadInstruction instr) {
		final TypeReference elemType = instr.getElementType();
		final ParameterField field = params.getArrayField(elemType);
		final int baseVar = instr.getArrayRef();
		final OrdinalSet<InstanceKey> basePts = pts.getPointsTo(baseVar);
		final int fieldVar = instr.getDef();
		final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
		consumer.addRefCandidate(basePts, field, fieldPts);
	}

	@Override
	public void visitArrayStore(final SSAArrayStoreInstruction instr) {
		final TypeReference elemType = instr.getElementType();
		final ParameterField field = params.getArrayField(elemType);
		final int baseVar = instr.getArrayRef();
		final OrdinalSet<InstanceKey> basePts = pts.getPointsTo(baseVar);
		final int fieldVar = instr.getValue();
		final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
		consumer.addModCandidate(basePts, field, fieldPts);
	}

	@Override
	public void visitGet(final SSAGetInstruction instr) {
		if (!doStaticFields && instr.isStatic()) {
			return;
		}

		final FieldReference f = instr.getDeclaredField();
		final IField ifield = cha.resolveField(f);

		if (ifield == null) {
//			System.err.println("Could not resolve " + f);
			return;
		}

		final ParameterField field = params.getObjectField(ifield);

		if (instr.isStatic()) {
			final int fieldVar = instr.getDef();
			final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
			consumer.addRefCandidate(null, field, fieldPts);
		} else {
			final int baseVar = instr.getRef();
			final OrdinalSet<InstanceKey> basePts = pts.getPointsTo(baseVar);
			final int fieldVar = instr.getDef();
			final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
			consumer.addRefCandidate(basePts, field, fieldPts);
		}
	}

	@Override
	public void visitPut(final SSAPutInstruction instr) {
		if (!doStaticFields && instr.isStatic()) {
			return;
		}

		final FieldReference f = instr.getDeclaredField();
		final IField ifield = cha.resolveField(f);

		if (ifield == null) {
//			System.err.println("Could not resolve " + f);
			return;
		}

		final ParameterField field = params.getObjectField(ifield);

		if (instr.isStatic()) {
			final int fieldVar = instr.getVal();
			final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
			consumer.addModCandidate(null, field, fieldPts);
		} else {
			final int baseVar = instr.getRef();
			final OrdinalSet<InstanceKey> basePts = pts.getPointsTo(baseVar);
			final int fieldVar = instr.getVal();
			final OrdinalSet<InstanceKey> fieldPts = pts.getPointsTo(fieldVar);
			consumer.addModCandidate(basePts, field, fieldPts);
		}
	}

}
