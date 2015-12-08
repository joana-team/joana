package edu.kit.joana.ifc.wala.constraints;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;

public class ConstraintBasedIFC<T> {
	private SDG sdg;
	private Map<SDGNode, T> sourceAnnotations;
	private Map<SDGNode, T> sinkAnnotations;
	private IStaticLattice<T> secLattice;
	private Map<SDGNode, T> forwardClassification;
	private Map<SDGNode, T> backwardClassification;
	private Map<SDGNode, T> classification;

	public ConstraintBasedIFC(SDG sdg, Map<SDGNode, T> sourceAnnotations,
			Map<SDGNode, T> sinkAnnotations, IStaticLattice<T> secLattice) {
		this.sdg = sdg;
		this.sourceAnnotations = sourceAnnotations;
		this.sinkAnnotations = sinkAnnotations;
		this.secLattice = secLattice;
	}

	public void computeClassification() {
		Solver forwardSolver = new ForwardSolver();
		Solver backwardSolver = new BackwardSolver();
		try {
			forwardSolver.solve(null);
			backwardSolver.solve(null);
			this.forwardClassification = forwardSolver.transferResult();
			this.backwardClassification = backwardSolver.transferResult();
		} catch (CancelException cancelled) {
		}
	}

	public boolean hasClassification(SDGNode n) {
		return classification.containsKey(n);
	}

	public T getForwardClassification(SDGNode n) {
		if (!forwardClassification.containsKey(n)) {
			return null;
		} else {
			return forwardClassification.get(n);
		}
	}

	public T getBackwardClassification(SDGNode n) {
		if (!backwardClassification.containsKey(n)) {
			return null;
		} else {
			return backwardClassification.get(n);
		}
	}

	public boolean isForwardClassificationCompatibleWithSinkAnnotation() {
		for (Map.Entry<SDGNode, T> sinkAnn : sinkAnnotations.entrySet()) {
			T forwLevel = forwardClassification.get(sinkAnn.getKey());
			if (forwLevel == null) continue;
			if (!LatticeUtil.isLeq(this.secLattice, forwLevel, sinkAnn.getValue())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return all nodes for which the forward classification level contradicts the annotated
	 * sink level.
	 */
	public Set<SDGNode> getNodesWithForwardViolation() {
		Set<SDGNode> ret = new LinkedHashSet<SDGNode>();
		for (Map.Entry<SDGNode, T> sinkAnn : sinkAnnotations.entrySet()) {
			T forwLevel = forwardClassification.get(sinkAnn.getKey());
			if (forwLevel == null) continue;
			if (!LatticeUtil.isLeq(this.secLattice, forwLevel, sinkAnn.getValue())) {
				ret.add(sinkAnn.getKey());
			}
		}
		return ret;
	}

	public boolean isBackwardClassificationCompatibleWithSourceAnnotation() {
		for (Map.Entry<SDGNode, T> sourceAnn : sourceAnnotations.entrySet()) {
			T backwLevel = backwardClassification.get(sourceAnn.getKey());
			if (backwLevel == null) continue;
			if (!LatticeUtil.isLeq(this.secLattice, sourceAnn.getValue(), backwLevel)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return all nodes for which the backward classification level contradicts the annotated
	 * source level.
	 */
	public Set<SDGNode> getNodesWithBackwardViolation() {
		Set<SDGNode> ret = new LinkedHashSet<SDGNode>();
		for (Map.Entry<SDGNode, T> sourceAnn : sourceAnnotations.entrySet()) {
			T backwLevel = backwardClassification.get(sourceAnn.getKey());
			if (backwLevel == null) continue;
			if (!LatticeUtil.isLeq(this.secLattice, sourceAnn.getValue(), backwLevel)) {
				ret.add(sourceAnn.getKey());
			}
		}
		return ret;
	}

	private abstract class Solver extends
			DefaultFixedPointSolver<SecLevelVariable<T>> {

		protected Map<SDGNode, SecLevelVariable<T>> node2var = new HashMap<SDGNode, SecLevelVariable<T>>();
		private final UnaryOperator<SecLevelVariable<T>> thePropagator;

		Solver() {
			this.thePropagator = initPropagator();
		}
		protected abstract UnaryOperator<SecLevelVariable<T>> initPropagator();

		@SuppressWarnings("unchecked")
		@Override
		protected SecLevelVariable<T>[] makeStmtRHS(int size) {
			return new SecLevelVariable[size];
		}

		@Override
		protected abstract void initializeVariables();

		@Override
		protected void initializeWorkList() {
			addConstraintsFromAnnotations();
			markVerticesOfSlice();
			System.out.println(getFixedPointSystem());
		}

		protected abstract void addConstraintsFromAnnotations();

		protected abstract Set<SDGNode> markVerticesOfSlice();

		protected UnaryOperator<SecLevelVariable<T>> getPropagator() {
			return this.thePropagator;
		}

		public Map<SDGNode, T> transferResult() {
			Map<SDGNode, T> classification = new HashMap<SDGNode, T>();
			for (Map.Entry<SDGNode, SecLevelVariable<T>> nodeAndVar : node2var
					.entrySet()) {
				classification.put(nodeAndVar.getKey(), nodeAndVar.getValue()
						.getSecLevel());
			}
			return classification;
		}
	}

	private class ForwardSolver extends Solver {
		protected void addConstraintsFromAnnotations() {
			for (Map.Entry<SDGNode, T> sourceAnn : sourceAnnotations.entrySet()) {
				newStatement(
						node2var.get(sourceAnn.getKey()),
						new LowerBoundOperator<T>(secLattice, sourceAnn
								.getValue()), true, false);
			}
		}

		protected Set<SDGNode> markVerticesOfSlice() {
			Set<SDGNode> bs0 = markReachingVerticesForward(
					sourceAnnotations.keySet(), EnumSet.of(
							SDGEdge.Kind.INTERFERENCE,
							SDGEdge.Kind.INTERFERENCE_WRITE, SDGEdge.Kind.FORK,
							SDGEdge.Kind.FORK_IN, SDGEdge.Kind.PARAMETER_IN,
							SDGEdge.Kind.CALL));
			Set<SDGNode> bs1 = markReachingVerticesForward(bs0, EnumSet.of(
					SDGEdge.Kind.INTERFERENCE, SDGEdge.Kind.INTERFERENCE_WRITE,
					SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
					SDGEdge.Kind.PARAMETER_OUT));
			return bs1;
		}

		private Set<SDGNode> markReachingVerticesForward(Set<SDGNode> start,
				Set<SDGEdge.Kind> forbiddenEdges) {
			Set<SDGNode> done = new HashSet<SDGNode>();
			LinkedList<SDGNode> worklist = new LinkedList<SDGNode>(start);

			while (!worklist.isEmpty()) {
				SDGNode v = worklist.poll();
				if (done.contains(v))
					continue;
				SecLevelVariable<T> vVar = node2var.get(v);
				for (SDGEdge e : sdg.outgoingEdgesOf(v)) {
					if (!e.getKind().isSDGEdge())
						continue;
					if (forbiddenEdges.contains(e.getKind()))
						continue;
					SDGNode w = e.getTarget();
					worklist.add(w);
					SecLevelVariable<T> wVar = node2var.get(w);
					newStatement(wVar, getPropagator(), vVar, true, false);
				}
				done.add(v);
			}
			return done;
		}

		@Override
		protected void initializeVariables() {
			for (SDGNode node : sdg.vertexSet()) {
				node2var.put(node,
						new SecLevelVariable<T>(node));
			}
		}

		@Override
		protected UnaryOperator<SecLevelVariable<T>> initPropagator() {
			return new SimpleGeqPropagator<T>(secLattice);
		}
	}

	private class BackwardSolver extends Solver {
		@Override
		protected void initializeVariables() {
			for (SDGNode node : sdg.vertexSet()) {
				node2var.put(node,
						new SecLevelVariable<T>(node));
			}
		}

		protected void addConstraintsFromAnnotations() {
			for (Map.Entry<SDGNode, T> sinkAnn : sinkAnnotations.entrySet()) {
				newStatement(
						node2var.get(sinkAnn.getKey()),
						new UpperBoundOperator<T>(secLattice, sinkAnn
								.getValue()), true, false);
			}
		}

		protected Set<SDGNode> markVerticesOfSlice() {
			Set<SDGNode> bs0 = markReachingVertices(sinkAnnotations.keySet(),
					EnumSet.of(SDGEdge.Kind.INTERFERENCE,
							SDGEdge.Kind.INTERFERENCE_WRITE, SDGEdge.Kind.FORK,
							SDGEdge.Kind.FORK_IN, SDGEdge.Kind.PARAMETER_OUT));
			Set<SDGNode> bs1 = markReachingVertices(bs0, EnumSet.of(
					SDGEdge.Kind.INTERFERENCE, SDGEdge.Kind.INTERFERENCE_WRITE,
					SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
					SDGEdge.Kind.PARAMETER_IN, SDGEdge.Kind.CALL));
			return bs1;
		}

		private Set<SDGNode> markReachingVertices(Set<SDGNode> start,
				Set<SDGEdge.Kind> forbiddenEdges) {
			Set<SDGNode> done = new HashSet<SDGNode>();
			LinkedList<SDGNode> worklist = new LinkedList<SDGNode>(start);
			while (!worklist.isEmpty()) {
				SDGNode v = worklist.poll();
				if (done.contains(v))
					continue;
				SecLevelVariable<T> vVar = node2var.get(v);
				for (SDGEdge e : sdg.incomingEdgesOf(v)) {
					if (!e.getKind().isSDGEdge())
						continue;
					if (forbiddenEdges.contains(e.getKind()))
						continue;
					SDGNode w = e.getSource();
					worklist.add(w);
					SecLevelVariable<T> wVar = node2var.get(w);
					newStatement(wVar, getPropagator(), vVar, true, false);
				}
				done.add(v);
			}
			return done;
		}

		@Override
		protected UnaryOperator<SecLevelVariable<T>> initPropagator() {
			return new SimpleLeqPropagator<T>(secLattice);
		}
	}
}
