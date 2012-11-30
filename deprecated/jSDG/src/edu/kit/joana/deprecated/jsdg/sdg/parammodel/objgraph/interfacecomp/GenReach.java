/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.interfacecomp;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Generic dataflow framework to accumulate reachable gen'ned values in a graph.
 *
 * @author sjfink
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class GenReach<T, L> extends BitVectorFramework<T, L> {

  @SuppressWarnings("unchecked")
  public GenReach(Graph<T> flowGraph, Map<T, Collection<L>> gen) {
    super(flowGraph, new GenFunctions<T, L>(gen), makeDomain(gen));
    // ugly but necessary, in order to avoid computing the domain twice.
    GenReach.GenFunctions<T, L> g = (GenReach.GenFunctions<T, L>) getTransferFunctionProvider();
    g.domain = getLatticeValues();
  }

  private static <T, L> OrdinalSetMapping<L> makeDomain(Map<T, Collection<L>> gen) {
    MutableMapping<L> result = MutableMapping.make();
    for (Collection<L> c : gen.values()) {
      for (L p : c) {
        result.add(p);
      }
    }
    return result;
  }

  static class GenFunctions<T, L> implements ITransferFunctionProvider<T, BitVectorVariable> {
    private final Map<T, Collection<L>> gen;

    private OrdinalSetMapping<L> domain;

    public GenFunctions(Map<T, Collection<L>> gen) {
      this.gen = gen;
    }

    public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorUnion.instance();
    }

    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(T node) {
      BitVector v = getGen(node);
      return new BitVectorUnionVector(v);
    }

    private BitVector getGen(T node) {
      Collection<L> g = gen.get(node);
      if (g == null) {
        return new BitVector();
      } else {
        BitVector result = new BitVector();
        for (L p : g) {
          result.set(domain.getMappedIndex(p));
        }
        return result;
      }
    }

    public boolean hasEdgeTransferFunctions() {
      return false;
    }

    public boolean hasNodeTransferFunctions() {
      return true;
    }

    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(T src, T dst) {
      Assertions.UNREACHABLE();
      return null;
    }

  }
}
