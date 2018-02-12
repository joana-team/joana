/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import static edu.kit.joana.wala.util.pointsto.WalaPointsToUtil.unify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;
import edu.kit.joana.wala.core.params.objgraph.candidates.UniqueParameterCandidate;
import edu.kit.joana.wala.core.params.objgraph.dataflow.PointsToWrapper;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefCandidateGraph implements Graph<ModRefCandidate> {

	private final InterProcCandidateModel modref;
	private final LinkedHashSet<ModRefRootCandidate> roots;
	private final OrdinalSet<InstanceKey> rootsPts;
	
	private final Map<UniqueParameterCandidate, Set<UniqueParameterCandidate>> isReachableFrom;

	private ModRefCandidateGraph(final InterProcCandidateModel modref, final LinkedHashSet<ModRefRootCandidate> roots,
			final OrdinalSet<InstanceKey> rootsPts,
			final Map<UniqueParameterCandidate, Set<UniqueParameterCandidate>> isReachableFrom) {
		if (modref == null) {
			throw new IllegalArgumentException();
		} else if (roots == null) {
			throw new IllegalArgumentException();
		}

		this.modref = modref;
		this.roots = roots;
		this.rootsPts = rootsPts;
		this.isReachableFrom = isReachableFrom;
	}

	public static ModRefCandidateGraph compute(
			final PointsToWrapper pa,
			final ModRefCandidates modref,
			final PDG pdg,
			final Map<UniqueParameterCandidate, Set<UniqueParameterCandidate>> isReachableFrom) {
		final InterProcCandidateModel pdgModRef = modref.getCandidates(pdg.cgNode);
		final LinkedHashSet<ModRefRootCandidate> roots = new LinkedHashSet<>(findMethodRoots(pa, pdg, modref.ignoreExceptions));
		final OrdinalSet<InstanceKey> rootsPts = findMethodRootPts(pa, pdg, modref.ignoreExceptions);

		final ModRefCandidateGraph g = new ModRefCandidateGraph(pdgModRef, roots, rootsPts, isReachableFrom);

		return g;
	}

	public Collection<ModRefRootCandidate> getRoots() {
		return Collections.unmodifiableSet(roots);
	}
	
	public OrdinalSet<InstanceKey> getRootsPts() {
		return rootsPts;
	}

	public static OrdinalSet<InstanceKey> findMethodRootPts(final PointsToWrapper pa, final PDG pdg,
			final boolean ignoreExceptions) {
		OrdinalSet<InstanceKey> roots = null;
		final CGNode n = pdg.cgNode;

		for (int i = 0; i < pdg.params.length; i++) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodParamPTS(n, i);
			roots = unify(roots, pts);
		}

		if (pdg.staticReads != null) {
			for (int i = 0; i < pdg.staticReads.length; i++) {
				final PDGField f = pdg.staticReads[i];
				final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
				roots = unify(roots, pts);
			}
		}

		if (pdg.staticWrites != null) {
			for (int i = 0; i < pdg.staticWrites.length; i++) {
				final PDGField f = pdg.staticWrites[i];
				final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
				roots = unify(roots, pts);
			}
		}

		for (final PDGField f : pdg.staticInterprocReads) {
			final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
			roots = unify(roots, pts);
		}

		for (final PDGField f : pdg.staticInterprocWrites) {
			final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
			roots = unify(roots, pts);
		}

		if (!pdg.isVoid()) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodReturnPTS(n);
			roots = unify(roots, pts);
		}

		if (!ignoreExceptions) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodExceptionPTS(n);
			roots = unify(roots, pts);
		}

		return roots;
	}

	/**
	 * Get the root candidates for a pdg. This method searches all root-formal-in and out nodes and creates/finds
	 * the proper ModRefCandidates.
	 */
	public static List<ModRefRootCandidate> findMethodRoots(final PointsToWrapper pa, final PDG pdg,
			final boolean ignoreExceptions) {
		final List<ModRefRootCandidate> roots = new LinkedList<ModRefRootCandidate>();
		final CGNode n = pdg.cgNode;

		for (int i = 0; i < pdg.params.length; i++) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodParamPTS(n, i);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(pdg.params[i], pts);
				roots.add(rp);
			}
		}

		if (pdg.staticReads != null) {
			for (int i = 0; i < pdg.staticReads.length; i++) {
				final PDGField f = pdg.staticReads[i];
				final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
				if (pts != null && !pts.isEmpty()) {
					final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
					roots.add(rp);
				}
			}
		}

		if (pdg.staticWrites != null) {
			for (int i = 0; i < pdg.staticWrites.length; i++) {
				final PDGField f = pdg.staticWrites[i];
				final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
				if (pts != null && !pts.isEmpty()) {
					final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
					roots.add(rp);
				}
			}
		}

		for (final PDGField f : pdg.staticInterprocReads) {
			final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
				roots.add(rp);
			}
		}

		for (final PDGField f : pdg.staticInterprocWrites) {
			final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
				roots.add(rp);
			}
		}

		if (!pdg.isVoid()) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodReturnPTS(n);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createMod(pdg.exit, pts);
				roots.add(rp);
			}
		}

		if (!ignoreExceptions) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodExceptionPTS(n);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createMod(pdg.exception, pts);
				roots.add(rp);
			}
		}

		return roots;
	}

	public static OrdinalSet<InstanceKey> findCallRootsPts(final PointsToWrapper pa, final PDG pdg, final PDGNode call,
			final boolean ignoreExceptions) {
		OrdinalSet<InstanceKey> roots = null;
		final SSAInvokeInstruction invk = (SSAInvokeInstruction) pdg.getInstruction(call);
		final CGNode n = pdg.cgNode;

		{
			final PDGNode pIns[] = pdg.getParamIn(call);
			for (int i = 0; i < pIns.length; i++) {
				final OrdinalSet<InstanceKey> pts = pa.getCallParamPTS(n, invk, i);
				roots = unify(roots, pts);
			}
		}

		{
			final List<PDGField> sIns = pdg.getStaticIn(call);
			if (sIns != null) {
				for (final PDGField f : sIns) {
					final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
					roots = unify(roots, pts);
				}
			}
		}

		{
			final List<PDGField> sOuts = pdg.getStaticOut(call);
			if (sOuts != null) {
				for (final PDGField f : sOuts) {
					final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
					roots = unify(roots, pts);
				}
			}
		}

		if (invk.getNumberOfReturnValues() > 0) {
			final OrdinalSet<InstanceKey> pts = pa.getCallReturnPTS(n, invk);
			roots = unify(roots, pts);
		}

		if (!ignoreExceptions) {
			final OrdinalSet<InstanceKey> pts = pa.getCallExceptionPTS(n, invk);
			roots = unify(roots, pts);
		}
		
		return roots;
	}
	
	public static List<ModRefRootCandidate> findCallRoots(final PointsToWrapper pa, final PDG pdg, final PDGNode call,
			final boolean ignoreExceptions) {
		final List<ModRefRootCandidate> roots = new LinkedList<ModRefRootCandidate>();
		final SSAInvokeInstruction invk = (SSAInvokeInstruction) pdg.getInstruction(call);
		final CGNode n = pdg.cgNode;

		{
			final PDGNode pIns[] = pdg.getParamIn(call);
			for (int i = 0; i < pIns.length; i++) {
				final OrdinalSet<InstanceKey> pts = pa.getCallParamPTS(n, invk, i);
				if (pts != null && !pts.isEmpty()) {
					final ModRefRootCandidate rp = ModRefRootCandidate.createRef(pIns[i], pts);
					roots.add(rp);
				}
			}
		}

		{
			final List<PDGField> sIns = pdg.getStaticIn(call);
			if (sIns != null) {
				for (final PDGField f : sIns) {
					final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
					if (pts != null && !pts.isEmpty()) {
						final ModRefRootCandidate rp = ModRefRootCandidate.createRef(f, pts);
						roots.add(rp);
					}
				}
			}
		}

		{
			final List<PDGField> sOuts = pdg.getStaticOut(call);
			if (sOuts != null) {
				for (final PDGField f : sOuts) {
					final OrdinalSet<InstanceKey> pts = pa.getStaticFieldPTS(f);
					if (pts != null && !pts.isEmpty()) {
						final ModRefRootCandidate rp = ModRefRootCandidate.createMod(f, pts);
						roots.add(rp);
					}
				}
			}
		}

		if (invk.getNumberOfReturnValues() > 0) {
			final PDGNode cReturn = pdg.getReturnOut(call);
			final OrdinalSet<InstanceKey> pts = pa.getCallReturnPTS(n, invk);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createMod(cReturn, pts);
				roots.add(rp);
			}
		}

		if (!ignoreExceptions) {
			final PDGNode cException = pdg.getExceptionOut(call);
			final OrdinalSet<InstanceKey> pts = pa.getCallExceptionPTS(n, invk);
			if (pts != null && !pts.isEmpty()) {
				final ModRefRootCandidate rp = ModRefRootCandidate.createMod(cException, pts);
				roots.add(rp);
			}
		}

		return roots;
	}

	@Override
	public Iterator<ModRefCandidate> iterator() {
		return new Iterator<ModRefCandidate>() {

			private final Iterator<ModRefRootCandidate> rootIt = roots.iterator();
			private final Iterator<ModRefFieldCandidate> fieldsIt = modref.iterator();

			@Override
			public final boolean hasNext() {
				return rootIt.hasNext() || fieldsIt.hasNext();
			}

			@Override
			public final ModRefCandidate next() {
				return (rootIt.hasNext() ? rootIt.next() : fieldsIt.next());
			}

			@Override
			public final void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int getNumberOfNodes() {
		return roots.size() + modref.size();
	}

	@Override
	public void addNode(final ModRefCandidate n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeNode(final ModRefCandidate n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsNode(final ModRefCandidate n) {
		return roots.contains(n) || modref.contains(n);
	}

	@Override
	public Iterator<ModRefCandidate> getPredNodes(final ModRefCandidate n) {
		if (n instanceof ModRefFieldCandidate) {
			final ModRefFieldCandidate fc = (ModRefFieldCandidate) n;
			return new Iterator<ModRefCandidate>() {

				private ModRefCandidate next = null;
				private Iterator<ModRefRootCandidate> itRoot = roots.iterator();
				private Iterator<ModRefFieldCandidate> itField = modref.iterator();

				private void searchNext() {
					while (next == null && itRoot.hasNext()) {
						final ModRefRootCandidate rc = itRoot.next();
						if (rc.isPotentialParentOf(fc)) {
							next = rc;
						}
					}

					while (next == null && itField.hasNext()) {
						final ModRefFieldCandidate rc = itField.next();
						if (rc != fc && rc.isPotentialParentOf(fc)) {
							next = rc;
						}
					}
				}

				@Override
				public boolean hasNext() {
					if (next == null) {
						searchNext();
					}

					return next != null;
				}

				@Override
				public ModRefCandidate next() {
					if (next == null) {
						searchNext();
					}

					final ModRefCandidate cur = next;
					next = null;

					return cur;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		return EMPTY_IT;
	}

	@Override
	public int getPredNodeCount(final ModRefCandidate n) {
		if (n instanceof ModRefFieldCandidate) {
			final ModRefFieldCandidate fc = (ModRefFieldCandidate) n;
			int counter = 0;

			for (final ModRefRootCandidate r : roots) {
				if (r.isPotentialParentOf(fc)) {
					counter ++;
				}
			}

			for (final ModRefFieldCandidate c : modref) {
				if (c != n && c.isPotentialParentOf(fc)) {
					counter++;
				}
			}

			return counter;
		}

		return 0;
	}

	private static final Iterator<ModRefCandidate> EMPTY_IT = new Iterator<ModRefCandidate>() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public ModRefCandidate next() {
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	};

	private Iterator<ModRefCandidate> getSuccNodesSlow(final ModRefCandidate n) {
		if (!n.getType().isPrimitiveType()) {
			return new Iterator<ModRefCandidate>() {

				private ModRefCandidate next = null;
				private Iterator<ModRefFieldCandidate> it = modref.iterator();

				private void searchNext() {
					while (next == null && it.hasNext()) {
						final ModRefFieldCandidate fc = it.next();

						if (n != fc && n.isPotentialParentOf(fc)) {
							next = fc;
						}
					}
				}

				@Override
				public boolean hasNext() {
					if (next == null) {
						searchNext();
					}

					return next != null;
				}

				@Override
				public ModRefCandidate next() {
					if (next == null) {
						searchNext();
					}

					final ModRefCandidate cur = next;
					next = null;

					return cur;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		return EMPTY_IT;
	}
	
	
	
	private Set<ModRefCandidate> getSuccNodeSet(final ModRefFieldCandidate nField) {
		final Set<ModRefCandidate> successors = new HashSet<>();
		
		for (UniqueParameterCandidate nUnique : nField.getUniques()) {
			for (UniqueParameterCandidate fcUnique : isReachableFrom.getOrDefault(nUnique, Collections.emptySet())) {
				final ModRefFieldCandidate fc = modref.getParameterCandidate(fcUnique);
				
				if (fc != null) {
					assert (Iterators.contains(fc.getUniques().iterator(), fcUnique));
					if (nField != fc) {
						assert nField.isPotentialParentOf(fc);
						successors.add(fc);
					}
				}
			}
		}
		return successors;
	};

	@Override
	public Iterator<ModRefCandidate> getSuccNodes(final ModRefCandidate n) {
		if (!n.getType().isPrimitiveType()) {
			if (n instanceof ModRefRootCandidate) {
				return getSuccNodesSlow(n);
			}
			
			final ModRefFieldCandidate nField = (ModRefFieldCandidate) n;
			final Set<ModRefCandidate> successors = getSuccNodeSet(nField);
			
			boolean assertionsEnabled = false;
			assert (assertionsEnabled = true);
			if (assertionsEnabled) {
				final HashSet<ModRefCandidate> successorsSlow = new HashSet<>();
				Iterators.addAll(successorsSlow, getSuccNodesSlow(n));
				
				assert successors.equals(successorsSlow);
			}
			
			return successors.iterator();
		}

		return EMPTY_IT;
	}


	@Override
	public int getSuccNodeCount(final ModRefCandidate n) {
		if (!n.getType().isPrimitiveType()) {
			int counter = 0;

			for (final ModRefFieldCandidate f : modref) {
				if (n != f && n.isPotentialParentOf(f)) {
					counter++;
				}
			}

			return counter;
		}

		return 0;
	}

	@Override
	public void addEdge(final ModRefCandidate src, final ModRefCandidate dst) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEdge(final ModRefCandidate src, final ModRefCandidate dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAllIncidentEdges(final ModRefCandidate node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeIncomingEdges(final ModRefCandidate node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeOutgoingEdges(final ModRefCandidate node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEdge(final ModRefCandidate src, final ModRefCandidate dst) {
		if (dst instanceof ModRefFieldCandidate) {
			return src.isPotentialParentOf((ModRefFieldCandidate) dst);
		}

		return false;
	}

	@Override
	public void removeNodeAndEdges(final ModRefCandidate n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
