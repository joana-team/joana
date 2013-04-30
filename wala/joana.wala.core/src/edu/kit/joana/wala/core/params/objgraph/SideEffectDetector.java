/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.Collection;
import java.util.Collections;
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
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetectorConfig.CandidateFilter;
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
	private final CandidateFilter filter;
	private final boolean onlyOneLevelFields;
	private final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect;
	
	private SideEffectDetector(final SDGBuilder sdg, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final CallGraph cg,
			final boolean onlyOneLEvelFields, final CandidateFilter filter) {
		this.sdg = sdg;
		this.modRef = modRef;
		this.sideEffectsDirect = sideEffectsDirect;
		this.cg = cg;
		this.filter = filter;
		this.onlyOneLevelFields = onlyOneLEvelFields;
	}

	/**
	 * A Result object captures the detected side-effects for a single candidate filter. A candidate filter may
	 * correspond to multiple attributes.
	 * 
	 * The results are stored as a set of Entry objects. The entries can be searched with the <tt>search</tt> method.
	 * An entry consists of the concrete attribute that has been modified, the cnadidate filter that machted the
	 * attribute, the method that triggers the modification and the type of the modification.
	 * Valid modification types are:
	 * <ul>
	 * <li>DIRECT_MOD: The attribute is modified by an instruction in the given method.</li>
	 * <li>DIRECT_REACHALE: A field reachable from the attribute is modified by an instruction in the given method.</li>
	 * <li>INDIRECT_MOD: The attribute is modified by a method that is called during execution of the
	 *  current method.</li>
	 * <li>INDIRECT_REACHABLE: A field reachable from the attribute is modified by a method that is called during
	 *  execution of the current method.</li>
	 * </ul>  
	 * 
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 */
	public static final class Result {
		private final String desc;
		public final CandidateFilter filter;
		private final Set<ParameterField> selectedFields = new HashSet<ParameterField>();
		private final Set<IMethod> directAndIndirectModification = new HashSet<IMethod>();
		private final Set<IMethod> directModification = new HashSet<IMethod>();
		private final Set<IMethod> directAndIndirectReachableFieldModification = new HashSet<IMethod>();
		private final Set<IMethod> directReachableFieldModification = new HashSet<IMethod>();
		
		/* set of result entries */
		private final Set<Entry> entries = new HashSet<SideEffectDetector.Result.Entry>();
		
		public static enum Type { 
			DIRECT_MOD,				// A variable matching the filter is modified in this method
			DIRECT_REACHABLE,		// A location reachable from a variable matching the filter is modified in this
									// method
			INDIRECT_MOD,			// A variable matching the filter is modified through a method called during the
									// execution of this method
			INDIRECT_REACHABLE		// A location reachable from a variable matching the filter is modified through a
									// method called during the execution of this method
		}
		
		/**
		 * An entry is a representation of a single modification to an attribute.
		 *  
		 * @author Juergen Graf <juergen.graf@gmail.com>
		 */
		public static class Entry {
			public final ParameterField param;
			public final CandidateFilter filter;
			public final Type type;
			public final IMethod method;
			
			public Entry(final ParameterField param, final CandidateFilter filter, final Type type,
					final IMethod method) {
				this.param = param;
				this.filter = filter;
				this.method = method;
				this.type = type;
			}
			
			public int hashCode() {
				return param.hashCode() + 23 * method.hashCode() + 7 * type.hashCode();
			}
			
			public boolean equals(Object o) {
				if (o instanceof Entry) {
					final Entry other = (Entry) o;
					return param.equals(other.param) && filter.equals(other.filter) && method.equals(other.method)
							&& type.equals(other.type);
				}
				
				return false;
			}
		}
		
		private Result(final CandidateFilter filter) {
			this.filter = filter;
			this.desc = filter.toString();
		}

		/**
		 * Searches the set of entries for elements matching the provided query parameter.
		 * If an attribute in the query is set, it is checked for equality. If it is <tt>null</tt> all values are
		 * allowed. E.g. if I want to search for all modification detected in method <tt>IMethod m</tt>, the query
		 * object would be <tt>new Entry(null, null, null, m)</tt>.   
		 * @param query The query object. The set of entries is searched for matching elements.
		 * @return A list of matching entry elements.
		 */
		public List<Entry> search(final Entry query) {
			if (query != null && query.filter != null && !query.filter.equals(filter)) {
				return Collections.emptyList();
			}
			
			if (query == null) {
				return search(null, null, null);
			}
			
			return search(query.param, query.type, query.method);
		}
		
		/**
		 * Searches the set of entries for elements matching the provided query parameters.
		 * If an parameter is set, it is checked for equality. If it is <tt>null</tt> all values are
		 * allowed. E.g. if I want to search for all modification detected in method <tt>IMethod m</tt>, the call
		 * would be <tt>search(null, null, null, m)</tt>.
		 *    
		 * @param param The modified attribute to search for.
		 * @param type The type of modification to search for.
		 * @param method The method to search for.
		 * @return A list of matching entry elements.
		 */
		public List<Entry> search(final ParameterField param, final Type type, final IMethod method) {
			final List<Entry> result = new LinkedList<SideEffectDetector.Result.Entry>();
			
			for (final Entry e : entries) {
				if ((param == null || param.equals(e.param))
						&& (type == null || type == e.type)
						&& (method == null || method.equals(e.method))) {
					result.add(e);
				}
			}
			
			return result;
		}
		
		private void addSelectedField(final ParameterField field) {
			selectedFields.add(field);
		}
		
		private void addDirectModification(final IMethod m, final ParameterField pf) {
			final Entry e = new Entry(pf, filter, Type.DIRECT_MOD, m);
			entries.add(e);
			directModification.add(m);
			directAndIndirectModification.add(m);
		}

		private void addIndirectModification(final IMethod m, final ParameterField pf) {
			final Entry e = new Entry(pf, filter, Type.INDIRECT_MOD, m);
			entries.add(e);
			directAndIndirectModification.add(m);
		}
		
		private void addIndirectFieldModification(final IMethod m, final ParameterField pf) {
			final Entry e = new Entry(pf, filter, Type.INDIRECT_REACHABLE, m);
			entries.add(e);
			directAndIndirectReachableFieldModification.add(m);
		}

		private void addDirectFieldModification(final IMethod m, final ParameterField pf) {
			final Entry e = new Entry(pf, filter, Type.DIRECT_REACHABLE, m);
			entries.add(e);
			directAndIndirectReachableFieldModification.add(m);
			directReachableFieldModification.add(m);
		}
		
		/**
		 * Returns a set of all attributes that match the candidate filter and are modified.
		 * @return a set of all attributes that match the candidate filter and are modified.
		 */
		public Set<ParameterField> getSelectedFields() {
			return Collections.unmodifiableSet(selectedFields);
		}
		
		/**
		 * Returns a set of methods that contain instructions that modify an attribute that matched the candidate
		 * filter.
		 * @return a set of methods that contain instructions that modify an attribute that matched the candidate
		 * filter.
		 */
		public Set<IMethod> getDirectModifies() {
			return Collections.unmodifiableSet(directModification);
		}
		
		/**
		 * Checks if the given methods contains an instruction that modifies an attribute matched by the candidate
		 * filter.
		 * @param m The method to check for.
		 * @return <tt>true</tt> iff <tt>m</tt> contains an instruction that modifies an attribute matched by the
		 * candidate filter.
		 */
		public boolean directModifies(final IMethod m) {
			return directModification.contains(m);
		}

		/**
		 * Checks if the given methods does NOT contain an instruction that modifies an attribute matched by the
		 * candidate filter, BUT may transitively call a meethod that does.
		 * @param m The method to check for.
		 * @return <tt>true</tt> iff <tt>m</tt> may call a method that conatins an instruction that modifies an
		 * attribute matched by the candidate filter.
		 */
		public boolean indirectModifies(final IMethod m) {
			return !directModification.contains(m) && directAndIndirectModification.contains(m);
		}
		
		/**
		 * Returns all methods that contain an instruction that modifies an attribute matching the candidate filter
		 * or any fields reachable from those attributes.
		 * @return A set of all methods that contain an instruction that modifies an attribute matching the candidate
		 * filter or any fields reachable from those attributes.
		 */
		public Set<IMethod> getDirectModifiesReachable() {
			return Collections.unmodifiableSet(directReachableFieldModification);
		}
		
		/**
		 * Checks if a given method contains an instruction that modifies an attribute matching the candidate filter
		 * or any fields reachable from those attributes.
		 * @param m The method to check.
		 * @return <tt>true</tt> if the given method contains an instruction that modifies an attribute matching the
		 * candidate filter or any fields reachable from those attributes.
		 */
		public boolean directModifiesReachable(final IMethod m) {
			return directReachableFieldModification.contains(m);
		}

		/**
		 * Checks if a given method DOES NOT contain an instruction that modifies an attribute matching the candidate
		 * filter or any fields reachable from those attributes, BUT may transitively call a method that does so.
		 * @param m The method to check.
		 * @return <tt>true</tt> if the given method may call a method that directly modifies an attribute matching
		 * the candidate filter or any fields reachable from those attributes
		 */
		public boolean indirectModifiesReachable(final IMethod m) {
			return !directReachableFieldModification.contains(m)
				&& directAndIndirectReachableFieldModification.contains(m);
		}
		
		/**
		 * Checks if the given method directly or indirectly modifies any attribute matching the candidate filter or
		 * fields reachable from those attributes.
		 * @param m The method to check.
		 * @return <tt>true</tt> if the given method directly or indirectly modifies any attribute matching the
		 * candidate filter or fields reachable from those attributes.
		 */
		public boolean modifies(final IMethod m) {
			return directAndIndirectModification.contains(m)
				|| directAndIndirectReachableFieldModification.contains(m);
		}
		
		/**
		 * Checks if the given method directly or indirectly modifies an attribute matching the candidate filter.
		 * @param m The method to check.
		 * @return <tt>true</tt> if the given method directly or indirectly modifies an attribute matching the
		 * candidate filter.
		 */
		public boolean modifiesBase(final IMethod m) {
			return directAndIndirectModification.contains(m);
		}

		/**
		 * Checks if the given method directly or indirectly modifies a field reachable from an attribute matching
		 * the candidate filter, BUT NOT the attribute itself.
		 * @param m The method to check.
		 * @return <tt>true</tt>  if the given method directly or indirectly modifies a field reachable from an
		 * attribute matching the candidate filter, BUT NOT the attribute itself.
		 */
		public boolean modifiesReachableFields(final IMethod m) {
			return directAndIndirectReachableFieldModification.contains(m);
		}
		
		/**
		 * Returns the standard representation of the side-effect analysis results for a single candidate filter.
		 */
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			
			sb.append("detected modifications of '" + desc + "':\n");
			
			if (!directModification.isEmpty()) {
				sb.append("direct base modification:\n");
				for (final IMethod im : directModification) {
					sb.append("\t" + PrettyWalaNames.methodName(im) + "\n");
				}
			}
			
			if (!directAndIndirectModification.isEmpty()
					&& directModification.size() != directAndIndirectModification.size()) {
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
			
			if (!directAndIndirectReachableFieldModification.isEmpty()
					&& directReachableFieldModification.size() != directAndIndirectReachableFieldModification.size()) {
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
	
	/**
	 * Used as input for the interprocedural phase of the side-effect detection.
	 */
	private static class Input {
		public final Set<ModRefFieldCandidate> fieldCandidates = new HashSet<ModRefFieldCandidate>();
		public final Set<ModRefRootCandidate> rootCandidates = new HashSet<ModRefRootCandidate>();
	}
	
	
	public static Result whoModifies(final CandidateFilter filter, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final SDGBuilder sdg,
			final CallGraph cg, final IProgressMonitor monitor) throws CancelException {
		final SideEffectDetector sed = new SideEffectDetector(sdg, modRef, sideEffectsDirect, cg, false, filter);
		final Result result = sed.run(monitor);
		
		return result;
	}

	public static Result whoModifiesOneLevel(final CandidateFilter filter, final ModRefCandidates modRef,
			final Map<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect, final SDGBuilder sdg,
			final CallGraph cg, final IProgressMonitor monitor) throws CancelException {
		final SideEffectDetector sed = new SideEffectDetector(sdg, modRef, sideEffectsDirect, cg, true, filter);
		final Result result = sed.run(monitor);
		
		return result;
	}

	private final Result run(final IProgressMonitor monitor) throws CancelException {
		final Result result = new Result(filter);
		
		final Input input = createInputForFilter(filter, result, monitor);
		
		if (input == null) {
			Log.ERROR.outln("Could not create input for side effect detector. Field name was '" + filter + "'");
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
						result.addDirectFieldModification(n.getMethod(), c.getField());
					} else {
						result.addIndirectFieldModification(n.getMethod(), c.getField());
					}
				}
			}
		}
		
		if (log.isEnabled()) {
			log.outln(result);
		}
		
		return result;
	}

	private Input createInputForFilter(final CandidateFilter filter, final Result result,
			final IProgressMonitor monitor) throws CancelException {
		final Logger log = Log.getLogger(Log.L_SIDEEFFECT_DEBUG);
		final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());
		final Input input = new Input();
		
		for (final CGNode n : cg) {
			MonitorUtil.throwExceptionIfCanceled(monitor);
			final InterProcCandidateModel ipcm = modRef.getCandidates(n);
			final PDG pdg = sdg.getPDGforMethod(n);
			final Collection<ModRefFieldCandidate> direct = sideEffectsDirect.get(n);
			
			for (final ModRefFieldCandidate c : ipcm) {
				final ParameterField pf = c.pc.getField();
				if (filter.isRelevant(pf)) {
					log.outln("found candidate: " + c);
					input.fieldCandidates.add(c);
					result.addSelectedField(pf);
					
					if (c.isMod()) {
						boolean isDirect = false;
						for (final ModRefFieldCandidate dc : direct) {
							if (dc.isMod() && dc.isMayAliased(c)) {
								isDirect = true;
								break;
							}
						}
						
						if (isDirect) {
							result.addDirectModification(n.getMethod(), pf);
						} else {
							result.addIndirectModification(n.getMethod(), pf);
						}
					}
				}
			}
			
			if (pdg != null) {
				if (pdg.staticReads != null) {
					for (int i = 0; i < pdg.staticReads.length; i++) {
						final PDGField f = pdg.staticReads[i];
						if (filter.isRelevant(f.field)) {
							result.addSelectedField(f.field);
							
							final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
							if (pts != null && !pts.isEmpty()) {
								final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
								input.rootCandidates.add(rp);
							}
							if (log.isEnabled()) {
								log.outln("found static root direct read in "
									+ PrettyWalaNames.methodName(pdg.getMethod()));
							}
						}
					}
				}

				if (pdg.staticWrites != null) {
					for (int i = 0; i < pdg.staticWrites.length; i++) {
						final PDGField f = pdg.staticWrites[i];
						if (filter.isRelevant(f.field)) {
							result.addSelectedField(f.field);
							
							final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
							if (pts != null && !pts.isEmpty()) {
								final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
								input.rootCandidates.add(rp);
							}
							result.addDirectModification(pdg.getMethod(), f.field);
							if (log.isEnabled()) {
								log.outln("found static root direct write in "
									+ PrettyWalaNames.methodName(pdg.getMethod()));
							}
						}
					}
				}

				for (final PDGField f : pdg.staticInterprocReads) {
					if (filter.isRelevant(f.field)) {
						result.addSelectedField(f.field);
						
						final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
						if (pts != null && !pts.isEmpty()) {
							final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
							input.rootCandidates.add(rp);
						}
						if (log.isEnabled()) {
							log.outln("found static root indirect read in "
								+ PrettyWalaNames.methodName(pdg.getMethod()));
						}
					}
				}

				for (final PDGField f : pdg.staticInterprocWrites) {
					if (filter.isRelevant(f.field)) {
						result.addSelectedField(f.field);
						
						final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
						if (pts != null && !pts.isEmpty()) {
							final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
							input.rootCandidates.add(rp);
						}
						result.addIndirectModification(pdg.getMethod(), f.field);
						if (log.isEnabled()) {
							log.outln("found static root indirect write in "
								+ PrettyWalaNames.methodName(pdg.getMethod()));
						}
					}
				}
			}
		}
				
		return input;
	}

}
