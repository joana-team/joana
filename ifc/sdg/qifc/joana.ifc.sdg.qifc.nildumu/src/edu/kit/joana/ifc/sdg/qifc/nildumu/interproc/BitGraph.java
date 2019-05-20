/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.Context.INFTY;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Context.d;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Context.v;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B.U;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.p;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Context;
import edu.kit.joana.ifc.sdg.qifc.nildumu.DotRegistry;
import edu.kit.joana.ifc.sdg.qifc.nildumu.MinCut;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.DependencySet;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;

/**
 * Graph that represents a function
 */
class BitGraph {

    final Context context;
    final List<Value> parameters;
    final Set<Bit> parameterBits;
    /**
     * bit → parameter number, index
     */
    private final Map<Bit, Pair<Integer, Integer>> bitInfo;

    final Value returnValue;

    final List<Integer> paramBitsPerReturnValue;

    BitGraph(Context context, List<Value> parameters, Value returnValue) {
        this.context = context;
        this.parameters = parameters;
        this.parameterBits = parameters.stream().flatMap(Value::stream).collect(Collectors.toSet());
        this.bitInfo = new HashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            Value param = parameters.get(i);
            for (int j = 1; j <= param.size(); j++) {
                bitInfo.put(param.get(j), p(i, j));
            }
        }
        this.returnValue = returnValue;
        assertThatAllBitsAreNotNull();
        paramBitsPerReturnValue = returnValue.stream().map(b -> calcReachableParamBits(b).size()).collect(Collectors.toList());
    }

    private void assertThatAllBitsAreNotNull(){
        returnValue.forEach(b -> {
            assert b != null: "Return bits shouldn't be null";
        });
        vl.walkBits(Arrays.asList(returnValue), b -> {
            assert b != null: "Bits shouldn't be null";
        });
        vl.walkBits(parameters, b -> {
            assert b != null: "Parameters bits shouldn't null";
        });
    }

    public static Bit cloneBit(Context context, Bit bit, DependencySet deps){
        Bit clone;
        if (bit.isUnknown()) {
            clone = bl.create(U, deps);
        } else {
            clone = bl.create(v(bit));
        }
        return clone;
    }

    /**
     * Applies the function graph to concrete arguments
     */
    public Value applyToArgs(Context context, List<Value> arguments){
        List<Value> extendedArguments = arguments;
        Map<Bit, Bit> newBits = new HashMap<>();
        // populate
        vl.walkBits(returnValue, bit -> {
            if (parameterBits.contains(bit)){
                Pair<Integer, Integer> loc = bitInfo.get(bit);
                Bit argBit = extendedArguments.get(loc.first).get(loc.second);
                newBits.put(bit, argBit);
            } else {
                Bit clone = cloneBit(context, bit, d(bit));
                clone.value(bit.value());
                newBits.put(bit, clone);
            }
        });
        DefaultMap<Value, Value> newValues = new DefaultMap<Value, Value>((map, value) -> {
            if (parameters.contains(value)){
                return arguments.get(parameters.indexOf(value));
            }
            Value clone = value.map(b -> {
                if (!parameterBits.contains(b)) {
                    return newBits.get(b);
                }
                return b;
            });
            clone.node(value.node());
            return value;
        });
        // update dependencies
        newBits.forEach((old, b) -> {
            if (!parameterBits.contains(old)) {
                b.alterDependencies(newBits::get);
            }
            //b.value(old.value());
        });
        return returnValue.map(newBits::get);
    }

    /**
     * Returns the bit of the passed set that are reachable from the bit
     */
    public Set<Bit> calcReachableBits(Bit bit, Set<Bit> bits){
        Set<Bit> reachableBits = new HashSet<>();
        bl.walkBits(bit, b -> {
            if (bits.contains(b)){
                reachableBits.add(b);
            }
        }, b -> false);
        return reachableBits;
    }

    /**
     * Returns the parameter bits that are reachable from the bit
     */
    public Set<Bit> calcReachableParamBits(Bit bit){
        return calcReachableBits(bit, parameterBits);
    }

    public Set<Bit> minCutBits(){
        return minCutBits(returnValue.bitSet(), parameterBits);
    }

    public Set<Bit> minCutBits(Set<Bit> outputBits, Set<Bit> inputBits){
        return MinCut.compute(outputBits, inputBits, context::weight).minCut;
    }

    public Set<Bit> minCutBits(Set<Bit> outputBits, Set<Bit> inputBits, int outputWeight){
        return MinCut.compute(outputBits, inputBits, b -> outputBits.contains(b) ? outputWeight : context.weight(b)).minCut;
    }

    Graph createDotGraph(String name, boolean withMinCut){
        return DotRegistry.createDotGraph(context, name, IntStream.range(0, parameters.size())
                .mapToObj(i -> new DotRegistry.Anchor(String.format("param %d", i), parameters.get(i))
                ).collect(Collectors.toList()),
                new DotRegistry.Anchor("return", returnValue),
                withMinCut ? minCutBits(returnValue.bitSet(), parameterBits, INFTY) : Collections.emptySet());
    }

    public void writeDotGraph(Path folder, String name, boolean withMinCut){
        Path path = folder.resolve(name + ".dot");
        try {
            Files.createDirectories(folder);
            Graphviz.fromGraph(createDotGraph(name, withMinCut)).render(Format.PLAIN).toFile(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A graph is equal to another graph if the number of parameter bits that are reachable 
     * for each return value bit are the same
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BitGraph){
            //assert ((BitGraph)obj).parameterBits == this.parameterBits;
            return paramBitsPerReturnValue.equals(((BitGraph)obj).paramBitsPerReturnValue);
        }
        return false;
    }
}