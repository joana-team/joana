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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;

import edu.kit.joana.wala.core.interference.ThreadInformationProvider;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class NoThreadStartToRunCallGraph implements CallGraph {

	private final CallGraph cg;
	private final Set<CGNode> from;
	private final Set<CGNode> to;
	private final BitVectorIntSet fromSet;
	private final BitVectorIntSet toSet;

	private NoThreadStartToRunCallGraph(final CallGraph cg, final Set<CGNode> from, final Set<CGNode> to) {
		this.cg = cg;
		this.from = from;
		this.to = to;

		{
			final BitVectorIntSet fs = new BitVectorIntSet();
			for (final CGNode n : from) {
				final int id = cg.getNumber(n);
				fs.add(id);
			}
			this.fromSet = fs;
		}

		{
			final BitVectorIntSet ts = new BitVectorIntSet();
			for (final CGNode n : to) {
				final int id = cg.getNumber(n);
				ts.add(id);
			}
			this.toSet = ts;
		}
	}

	public static CallGraph create(final CallGraph cg) {
		final Set<CGNode> threadStarts = cg.getNodes(ThreadInformationProvider.JavaLangThreadStart);
		if (threadStarts == null || threadStarts.isEmpty()) {
			return cg;
		}

		final IClassHierarchy cha = cg.getClassHierarchy();
		final IClass runnable = cha.lookupClass(ThreadInformationProvider.JavaLangRunnable);
		final Selector selRun = ThreadInformationProvider.JavaLangRunnableRun.getSelector();
		final Set<CGNode> runnableRuns = new HashSet<CGNode>();

		for (final CGNode start : threadStarts) {
			final Iterator<CGNode> runIt = cg.getSuccNodes(start);
			while (runIt.hasNext()) {
				final CGNode tgt = runIt.next();
				final IMethod im = tgt.getMethod();

				if (cha.implementsInterface(im.getDeclaringClass(), runnable) && selRun.equals(im.getSelector())) {
					runnableRuns.add(tgt);
				}
			}
		}

		if (runnableRuns.isEmpty()) {
			return cg;
		}

		final NoThreadStartToRunCallGraph noThread = new NoThreadStartToRunCallGraph(cg, threadStarts, runnableRuns);

		return noThread;
	}

	@Override
	public void removeNodeAndEdges(CGNode n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<CGNode> iterator() {
		return cg.iterator();
	}

	@Override
	public int getNumberOfNodes() {
		return cg.getNumberOfNodes();
	}

	@Override
	public void addNode(CGNode n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeNode(CGNode n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsNode(CGNode n) {
		return cg.containsNode(n);
	}

	@Override
	public Iterator<CGNode> getPredNodes(CGNode n) {
		if (to.contains(n)) {
			return new FilterIterator<CGNode>(cg.getPredNodes(n), new Predicate<CGNode>() {

				@Override
				public boolean test(CGNode o) {
					return !from.contains(o);
				}
			});
		} else {
			return cg.getPredNodes(n);
		}
	}

	@Override
	public int getPredNodeCount(CGNode n) {
		if (to.contains(n)) {
			int count = 0;
			final Iterator<CGNode> it = getPredNodes(n);
			while (it.hasNext()) {
				it.next();
				count++;
			}

			return count;
		} else {
			return cg.getPredNodeCount(n);
		}
	}

	@Override
	public Iterator<CGNode> getSuccNodes(CGNode n) {
		if (from.contains(n)) {
			return new FilterIterator<CGNode>(cg.getSuccNodes(n), new Predicate<CGNode>() {

				@Override
				public boolean test(CGNode o) {
					return !to.contains(o);
				}
			});
		} else {
			return cg.getSuccNodes(n);
		}
	}

	@Override
	public int getSuccNodeCount(CGNode N) {
		if (from.contains(N)) {
			int count = 0;
			final Iterator<CGNode> it = getSuccNodes(N);
			while (it.hasNext()) {
				it.next();
				count++;
			}

			return count;
		} else {
			return cg.getSuccNodeCount(N);
		}
	}

	@Override
	public void addEdge(CGNode src, CGNode dst) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEdge(CGNode src, CGNode dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAllIncidentEdges(CGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeIncomingEdges(CGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeOutgoingEdges(CGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEdge(CGNode src, CGNode dst) {
		if (from.contains(src) && to.contains(dst)) {
			return false;
		}

		return cg.hasEdge(src, dst);
	}

	@Override
	public int getNumber(CGNode N) {
		return cg.getNumber(N);
	}

	@Override
	public CGNode getNode(int number) {
		return cg.getNode(number);
	}

	@Override
	public int getMaxNumber() {
		return cg.getMaxNumber();
	}

	@Override
	public Iterator<CGNode> iterateNodes(IntSet s) {
		return cg.iterateNodes(s);
	}

	@Override
	public IntSet getSuccNodeNumbers(CGNode node) {
		if (from.contains(node)) {
			final IntSet is = cg.getSuccNodeNumbers(node);
			final BitVectorIntSet bv = new BitVectorIntSet(is);
			bv.removeAll(this.toSet);

			return bv;
		}

		return cg.getSuccNodeNumbers(node);
	}

	@Override
	public IntSet getPredNodeNumbers(CGNode node) {
		if (to.contains(node)) {
			final IntSet is = cg.getPredNodeNumbers(node);
			final BitVectorIntSet bv = new BitVectorIntSet(is);
			bv.removeAll(this.fromSet);

			return bv;
		}

		return cg.getPredNodeNumbers(node);
	}

	@Override
	public CGNode getFakeRootNode() {
		return cg.getFakeRootNode();
	}

	@Override
	public CGNode getFakeWorldClinitNode() {
		return cg.getFakeWorldClinitNode();
	}

	@Override
	public Collection<CGNode> getEntrypointNodes() {
		return cg.getEntrypointNodes();
	}

	@Override
	public CGNode getNode(IMethod method, Context C) {
		return cg.getNode(method, C);
	}

	@Override
	public Set<CGNode> getNodes(MethodReference m) {
		return cg.getNodes(m);
	}

	@Override
	public IClassHierarchy getClassHierarchy() {
		return cg.getClassHierarchy();
	}

	@Override
	public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
		if (from.contains(node)) {
			final Set<CGNode> orig = cg.getPossibleTargets(node, site);
			final Set<CGNode> adjust = new HashSet<CGNode>();
			for (final CGNode n : orig) {
				if (!to.contains(n)) {
					adjust.add(n);
				}
			}

			return adjust;
		}

		return cg.getPossibleTargets(node, site);
	}

	@Override
	public int getNumberOfTargets(CGNode node, CallSiteReference site) {
		if (from.contains(node)) {
			return getPossibleTargets(node, site).size();
		}

		return cg.getNumberOfTargets(node, site);
	}

	@SuppressWarnings("unchecked")
	private static final Iterator<CallSiteReference> EMPTY_IT =
			((List<CallSiteReference>) Collections.EMPTY_LIST).iterator();

	@Override
	public Iterator<CallSiteReference> getPossibleSites(CGNode src, CGNode target) {
		if (from.contains(src) && to.contains(target)) {
			return EMPTY_IT;
		}

		return cg.getPossibleSites(src, target);
	}
}
