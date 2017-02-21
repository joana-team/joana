/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.dataflow;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIntersection;
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

  public static <T,L> GenReach<T,L> createUnionFramework(Graph<T> flowGraph, Map<T, Collection<L>> gen) {
	  return new GenReach<T,L>(flowGraph, new UnionMeetGenFunctions<T, L>(gen), gen);
  }

  public static <T,L> GenReach<T,L> createIntersectionFramework(Graph<T> flowGraph, Map<T, Collection<L>> gen) {
	  return new GenReach<T,L>(flowGraph, new IntersectionMeetGenFunctions<T, L>(gen), gen);
  }

  public GenReach(Graph<T> flowGraph, GenFunctions<T, L> tfp, OrdinalSetMapping<L> domain) {
    super(flowGraph, tfp, domain);
    tfp.domain = domain;
  }

  public GenReach(Graph<T> flowGraph, GenFunctions<T, L> tfp, Map<T, Collection<L>> gen) {
    this(flowGraph, tfp, makeDomain(gen));
  }

  public GenReach(Graph<T> flowGraph, Map<T, Collection<L>> gen) {
    this(flowGraph, new UnionMeetGenFunctions<T, L>(gen), gen);
  }

  public GenReach(Graph<T> flowGraph, Map<T, Collection<L>> gen, OrdinalSetMapping<L> domain) {
    this(flowGraph, new UnionMeetGenFunctions<T, L>(gen), domain);
  }

  private static <T, L> OrdinalSetMapping<L> makeDomain(Map<T, Collection<L>> gen) {
    MutableMapping<L> result = MutableMapping.makeIdentityMapping();
    for (Entry<T, Collection<L>> e : gen.entrySet()) {
      final T t = e.getKey();
      final Collection<L> c = e.getValue();
      
      assert t != null;
      
      for (L p : c) {
        result.add(p);
      }
    }
    return result;
  }

  public static final class IntersectionMeetGenFunctions<T,L> extends GenFunctions<T,L> {

	public IntersectionMeetGenFunctions(Map<T, Collection<L>> gen) {
	  super(gen);
	}

	@Override
	public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorIntersection.instance();
	}

  }

  public static final class UnionMeetGenFunctions<T,L> extends GenFunctions<T,L> {

	public UnionMeetGenFunctions(Map<T, Collection<L>> gen) {
	  super(gen);
	}

	@Override
	public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorUnion.instance();
	}

  }

  public abstract static class GenFunctions<T, L> implements ITransferFunctionProvider<T, BitVectorVariable> {
    private final Map<T, Collection<L>> gen;

    private OrdinalSetMapping<L> domain;

    public GenFunctions(Map<T, Collection<L>> gen) {
      this.gen = gen;
    }

    public abstract AbstractMeetOperator<BitVectorVariable> getMeetOperator();

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
