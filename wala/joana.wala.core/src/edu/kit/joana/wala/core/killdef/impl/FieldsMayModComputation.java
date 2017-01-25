/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.killdef.IFieldsMayMod;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidate;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;
import edu.kit.joana.wala.core.params.objgraph.ModRefFieldCandidate;

/**
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 * 
 */
public class FieldsMayModComputation implements IFieldsMayMod {

	private final CallGraph cg;
	private final OrdinalSetMapping<ParameterField> mapping;
	private final Map<CGNode, OrdinalSet<ParameterField>> cg2read = new HashMap<CGNode, OrdinalSet<ParameterField>>();
	private final Map<CGNode, OrdinalSet<ParameterField>> cg2write = new HashMap<CGNode, OrdinalSet<ParameterField>>();

	private FieldsMayModComputation(final OrdinalSetMapping<ParameterField> mapping, final CallGraph cg) {
		this.mapping = mapping;
		this.cg = cg;
	}

	public static IFieldsMayMod create(final SDGBuilder sdg, final ModRefCandidates modref,
			final IProgressMonitor progress) throws CancelException {
		final Set<ParameterField> allFields = collectAllFields(sdg, modref, progress);
		final OrdinalSetMapping<ParameterField> map = new ObjectArrayMapping<ParameterField>(
				allFields.toArray(new ParameterField[allFields.size()]));

		final FieldsMayModComputation fmm = new FieldsMayModComputation(map, sdg.getWalaCallGraph());

		fmm.buildMaps(sdg, modref, progress);

		return fmm;
	}

	private void buildMaps(final SDGBuilder sdg, final ModRefCandidates modref, final IProgressMonitor progress)
			throws CancelException {
		for (final PDG pdg : sdg.getAllPDGs()) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final BitVectorIntSet bvMayMod = new BitVectorIntSet();
			final BitVectorIntSet bvMayRead = new BitVectorIntSet();

			final InterProcCandidateModel model = modref.getCandidates(pdg.cgNode);
			if (model != null) {
				for (final ModRefCandidate cand : model) {
					if (cand instanceof ModRefFieldCandidate) {
						final ModRefFieldCandidate fieldCand = (ModRefFieldCandidate) cand;
						final OrdinalSet<ParameterField> fields = fieldCand.getFields();
						for (final ParameterField field : fields) {
							final int id = mapping.getMappedIndex(field);
							if (cand.isMod()) {
								bvMayMod.add(id);
							}

							if (cand.isRef()) {
								bvMayRead.add(id);
							}
						}
					}
				}
			}

			for (final PDGField sField : pdg.staticInterprocReads) {
				final int id = mapping.getMappedIndex(sField.field);
				bvMayRead.add(id);
			}

			for (final PDGField sField : pdg.staticInterprocWrites) {
				final int id = mapping.getMappedIndex(sField.field);
				bvMayMod.add(id);
			}

			final OrdinalSet<ParameterField> mayRead = new OrdinalSet<ParameterField>(bvMayRead, mapping);
			cg2read.put(pdg.cgNode, mayRead);
			final OrdinalSet<ParameterField> mayMod = new OrdinalSet<ParameterField>(bvMayMod, mapping);
			cg2write.put(pdg.cgNode, mayMod);
		}
	}

	private static Set<ParameterField> collectAllFields(final SDGBuilder sdg, final ModRefCandidates modref,
			final IProgressMonitor progress) throws CancelException {
		final Set<ParameterField> all = new HashSet<ParameterField>();

		for (final PDG pdg : sdg.getAllPDGs()) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final InterProcCandidateModel model = modref.getCandidates(pdg.cgNode);
			if (model != null) {
				for (final ModRefCandidate cand : model) {
					if (cand instanceof ModRefFieldCandidate) {
						final ModRefFieldCandidate fieldCand = (ModRefFieldCandidate) cand;
						final OrdinalSet<ParameterField> fields = fieldCand.getFields();
						for (final ParameterField field : fields) {
							all.add(field);
						}
					}
				}

				for (final PDGField sField : pdg.staticInterprocReads) {
					all.add(sField.field);
				}

				for (final PDGField sField : pdg.staticInterprocWrites) {
					all.add(sField.field);
				}
			}
		}

		return all;
	}

	@Override
	public IFieldsMayModMethod getFieldsModFor(final CGNode method) {
		final OrdinalSet<ParameterField> mayRead = cg2read.get(method);
		final OrdinalSet<ParameterField> mayWrite = cg2write.get(method);

		return new FieldsMayModPDG(mayRead, mayWrite);
	}

	private final class FieldsMayModPDG implements IFieldsMayModMethod {

		private final OrdinalSet<ParameterField> mayRead;
		private final OrdinalSet<ParameterField> mayWrite;

		private FieldsMayModPDG(final OrdinalSet<ParameterField> mayRead, final OrdinalSet<ParameterField> mayWrite) {
			this.mayRead = mayRead;
			this.mayWrite = mayWrite;
		}

		@Override
		public boolean mayMod(final ParameterField f) {
			return mayWrite.contains(f);
		}

		@Override
		public boolean mayRef(final ParameterField f) {
			return mayRead.contains(f);
		}

	}

	@Override
	public boolean mayCallModField(final CGNode method, final CallSiteReference csr, final ParameterField field) {
		final Set<CGNode> tgts = cg.getPossibleTargets(method, csr);

		if (tgts != null) {
			for (final CGNode tgt : tgts) {
				final OrdinalSet<ParameterField> mayWrite = cg2write.get(tgt);
				if (mayWrite != null && mayWrite.contains(field)) {
					return true;
				}
			}
		}

		return false;
	}

}
