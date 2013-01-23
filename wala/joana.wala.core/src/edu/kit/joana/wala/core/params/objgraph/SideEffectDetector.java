/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;
import edu.kit.joana.wala.core.params.objgraph.dataflow.PointsToWrapper;
import edu.kit.joana.wala.util.PrettyWalaNames;

/**
 * This class checks all methods in the current program for specific side-effects. It aim to answer questions
 * like "which methods may modify the static field myField or any of its reachable subfields?".
 * These results are supposed to be used by the KeY tool.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class SideEffectDetector {

	private final SDGBuilder sdg;
	private final ModRefCandidates modRef;
	private final CallGraph cg;
	private final String fieldName;
	private final boolean onlyOneLevelFields;
	private final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect;
	
	private SideEffectDetector(final SDGBuilder sdg, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final CallGraph cg,
			boolean onlyOneLEvelFields, final String fieldName) {
		this.sdg = sdg;
		this.modRef = modRef;
		this.sideEffectsDirect = sideEffectsDirect;
		this.cg = cg;
		this.fieldName = fieldName;
		this.onlyOneLevelFields = onlyOneLEvelFields;
	}

	public static final class Result {
		private final String desc;
		private final Set<IMethod> directAndIndirectModification = new HashSet<IMethod>();
		private final Set<IMethod> directModification = new HashSet<IMethod>();
		private final Set<IMethod> directAndIndirectReachableFieldModification = new HashSet<IMethod>();
		private final Set<IMethod> directReachableFieldModification = new HashSet<IMethod>();
		
		private Result(final String desc) {
			this.desc = desc;
		}
		
		private void addDirectModification(final IMethod m) {
			directModification.add(m);
			directAndIndirectModification.add(m);
		}

		private void addIndirectModification(final IMethod m) {
			directAndIndirectModification.add(m);
		}

		private void addIndirectFieldModification(final IMethod m) {
			directAndIndirectReachableFieldModification.add(m);
		}

		private void addDirectFieldModification(final IMethod m) {
			directAndIndirectReachableFieldModification.add(m);
			directReachableFieldModification.add(m);
		}
		
		public boolean directModifies(final IMethod m) {
			return directModification.contains(m);
		}

		public boolean indirectModifies(final IMethod m) {
			return !directModification.contains(m) && directAndIndirectModification.contains(m);
		}
		
		public boolean directModifiesReachable(final IMethod m) {
			return directReachableFieldModification.contains(m);
		}

		public boolean indirectModifiesReachable(final IMethod m) {
			return !directReachableFieldModification.contains(m) && directAndIndirectReachableFieldModification.contains(m);
		}
		
		public boolean modifies(final IMethod m) {
			return directAndIndirectModification.contains(m) || directAndIndirectReachableFieldModification.contains(m);
		}
		
		public boolean modifiesBase(final IMethod m) {
			return directAndIndirectModification.contains(m);
		}

		public boolean modifiesReachableFields(final IMethod m) {
			return directAndIndirectReachableFieldModification.contains(m);
		}
		
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			
			sb.append("detected modifications of '" + desc + "':\n");
			
			if (!directModification.isEmpty()) {
				sb.append("direct base modification:\n");
				for (final IMethod im : directModification) {
					sb.append("\t" + PrettyWalaNames.methodName(im) + "\n");
				}
			}
			
			if (!directAndIndirectModification.isEmpty() && directModification.size() != directAndIndirectModification.size()) {
				sb.append("indirect base modification:\n");
				for (final IMethod im : directAndIndirectModification) {
					if (!directModification.contains(im)) {
						sb.append("\t" + PrettyWalaNames.methodName(im) + "\n");
					}
				}
			}
			
			if (!directReachableFieldModification.isEmpty()) {
				sb.append("direct field modification:\n");
				for (final IMethod im : directReachableFieldModification) {
					sb.append("\t" + PrettyWalaNames.methodName(im) + "\n");
				}
			}
			
			if (!directAndIndirectReachableFieldModification.isEmpty() &&
					directReachableFieldModification.size() != directAndIndirectReachableFieldModification.size()) {
				sb.append("indirect field modification:\n");
				for (final IMethod im : directAndIndirectReachableFieldModification) {
					if (!directReachableFieldModification.contains(im)) {
						sb.append("\t" + PrettyWalaNames.methodName(im) + "\n");
					}
				}
			}
			
			return sb.toString();
		}
	}
	
	public static class Input {
		public final Set<ModRefFieldCandidate> fieldCandidates = new HashSet<ModRefFieldCandidate>();
		public final Set<ModRefRootCandidate> rootCandidates = new HashSet<ModRefRootCandidate>();
		public final ParameterField field;
		
		public Input(final ParameterField field) {
			this.field = field;
		}
	}
	
	
	public static Result whoModifies(final String fieldName, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final SDGBuilder sdg,
			final CallGraph cg, final IProgressMonitor monitor) throws CancelException {
		final SideEffectDetector sed = new SideEffectDetector(sdg, modRef, sideEffectsDirect, cg, false, fieldName);
		final Result result = sed.run(monitor);
		
		return result;
	}

	public static Result whoModifiesOneLevel(final String fieldName, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final SDGBuilder sdg,
			final CallGraph cg, final IProgressMonitor monitor) throws CancelException {
		final SideEffectDetector sed = new SideEffectDetector(sdg, modRef, sideEffectsDirect, cg, true, fieldName);
		final Result result = sed.run(monitor);
		
		return result;
	}

	private final Result run(final IProgressMonitor monitor) throws CancelException {
		final Result result = new Result(fieldName);
		
		final Input input = createInputForStaticField(fieldName, result, monitor);
		
		if (input == null) {
			Log.ERROR.outln("Could not create input for side effect detector. Field name was '" + fieldName + "'");
			return null;
		}
		
		final Logger log = Log.getLogger(Log.L_SIDEEFFECT_DEBUG);
		log.out("searching potentially reachable fields");
		final Set<ModRefFieldCandidate> potentiallyReachable = new HashSet<ModRefFieldCandidate>();

		boolean changed = true;
		while (changed) {
			log.out(".");
			changed = false;
			for (final CGNode n : cg) {
				MonitorUtil.throwExceptionIfCanceled(monitor);
				
				for (final ModRefFieldCandidate c : modRef.getCandidates(n)) {
					if (!potentiallyReachable.contains(c)) {
						boolean add = false;
						for (final ModRefRootCandidate parent : input.rootCandidates) {
							if (parent.isRef() && parent.isPotentialParentOf(c)) {
								add = true;
								break;
							}
						}

						if (!add) {
							for (final ModRefFieldCandidate parent : input.fieldCandidates) {
								if (parent.isRef() && parent.isPotentialParentOf(c)) {
									add = true;
									break;
								}
							}
						}
						
						if (!add && !onlyOneLevelFields) {
							for (final ModRefFieldCandidate parent : potentiallyReachable) {
								if (parent.isRef() && parent.isPotentialParentOf(c)) {
									add = true;
									break;
								}
							}
						}
						
						if (add) {
							changed = true;
							potentiallyReachable.add(c);
						}
					}
				}
			}
		}
		log.outln(" (" + potentiallyReachable.size() + " found) done.");
		
		for (final CGNode n : cg) {
			final InterProcCandidateModel ipcm = modRef.getCandidates(n);
			final Collection<ModRefFieldCandidate> direct = sideEffectsDirect.get(n);

			for (final ModRefFieldCandidate c : ipcm) {
				if (c.isMod() && potentiallyReachable.contains(c)) {
					if (direct.contains(c)) {
						result.addDirectFieldModification(n.getMethod());
					} else {
						result.addIndirectFieldModification(n.getMethod());
					}
				}
			}
		}
		
		if (log.isEnabled()) {
			log.outln(result);
		}
		
		return result;
	}

	private Input createInputForStaticField(final String fieldName, final Result result, final IProgressMonitor monitor) throws CancelException {
		final Logger log = Log.getLogger(Log.L_SIDEEFFECT_DEBUG);
		final Set<ModRefFieldCandidate> baseCandidates = new HashSet<ModRefFieldCandidate>();
		final Set<ModRefRootCandidate> rootCandidates = new HashSet<ModRefRootCandidate>();
		final Set<IMethod> modifyingMethods = new HashSet<IMethod>();
		final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());
		ParameterField field = null;
		
		for (final CGNode n : cg) {
			MonitorUtil.throwExceptionIfCanceled(monitor);
			final InterProcCandidateModel ipcm = modRef.getCandidates(n);
			for (final ModRefFieldCandidate c : ipcm) {
				final ParameterField pf = c.pc.getField();
				if (pf != null && pf.getBytecodeName().contains(fieldName)) {
					log.outln("found candidate: " + c);
					if (field == null) {
						field = c.pc.getField();
					}
					baseCandidates.add(c);
					if (c.isMod()) {
						modifyingMethods.add(n.getMethod());
					}
				}
			}
			
			final PDG pdg = sdg.getPDGforMethod(n);
			if (pdg != null) {
				final List<ModRefRootCandidate> roots = new LinkedList<ModRefRootCandidate>();

				if (pdg.staticReads != null) {
					for (int i = 0; i < pdg.staticReads.length; i++) {
						final PDGField f = pdg.staticReads[i];
						if (f.field.getName().contains(fieldName)) {
							if (field == null) {
								field = f.field;
							}
							final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
							if (pts != null && !pts.isEmpty()) {
								final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
								roots.add(rp);
							}
							if (log.isEnabled()) {
								log.outln("found static root direct read in " + PrettyWalaNames.methodName(pdg.getMethod()));
							}
						}
					}
				}

				if (pdg.staticWrites != null) {
					for (int i = 0; i < pdg.staticWrites.length; i++) {
						final PDGField f = pdg.staticWrites[i];
						if (f.field.getName().contains(fieldName)) {
							if (field == null) {
								field = f.field;
							}
							final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
							if (pts != null && !pts.isEmpty()) {
								final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
								roots.add(rp);
							}
							result.addDirectModification(pdg.getMethod());
							if (log.isEnabled()) {
								log.outln("found static root direct write in " + PrettyWalaNames.methodName(pdg.getMethod()));
							}
						}
					}
				}

				for (final PDGField f : pdg.staticInterprocReads) {
					if (f.field.getName().contains(fieldName)) {
						if (field == null) {
							field = f.field;
						}
						final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
						if (pts != null && !pts.isEmpty()) {
							final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
							roots.add(rp);
						}
						if (log.isEnabled()) {
							log.outln("found static root indirect read in " + PrettyWalaNames.methodName(pdg.getMethod()));
						}
					}
				}

				for (final PDGField f : pdg.staticInterprocWrites) {
					if (f.field.getName().contains(fieldName)) {
						if (field == null) {
							field = f.field;
						}
						final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
						if (pts != null && !pts.isEmpty()) {
							final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
							roots.add(rp);
						}
						result.addIndirectModification(pdg.getMethod());
						if (log.isEnabled()) {
							log.outln("found static root indirect write in " + PrettyWalaNames.methodName(pdg.getMethod()));
						}
					}
				}

				rootCandidates.addAll(roots);
			}
		}
		
		if (field != null) {
			final Input input = new Input(field);
			input.fieldCandidates.addAll(baseCandidates);
			input.rootCandidates.addAll(rootCandidates);
			
			return input;
		} else {
			return null;
		}
	}

}
