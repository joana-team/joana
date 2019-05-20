/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap.ForbiddenAction.FORBID_DELETIONS;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap.ForbiddenAction.FORBID_VALUE_UPDATES;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Sec;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.NildumuException;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;

/**
 * Contains the bits that are marked as input or output, that have an unknown value
 */
public class IOValues {

    public static class MultipleLevelsPerValueException extends NildumuException {
        MultipleLevelsPerValueException(Value value){
            super(String.format("Multiple security levels per value are not supported, attempted it for value %s", value));
        }
    }

    private final Map<Sec<?>, Set<Value>> valuesPerSec;
    private final Map<Value, Sec<?>> secPerValue;
    private final Map<Bit, Sec<?>> secPerBit;
    private final Set<Bit> bits;

    IOValues() {
        this.valuesPerSec = new DefaultMap<>(new LinkedHashMap<>(), new DefaultMap.Extension<Sec<?>, Set<Value>>() {
            @Override
            public Set<Value> defaultValue(Map<Sec<?>, Set<Value>> map, Sec<?> key) {
                return new LinkedHashSet<>();
            }
        }, FORBID_DELETIONS);
        this.secPerValue = new DefaultMap<>(new HashMap<>(), FORBID_VALUE_UPDATES);
        this.secPerBit = new DefaultMap<>(new HashMap<>(), FORBID_DELETIONS);
        this.bits = new LinkedHashSet<>();
    }


    public void add(Sec<?> sec, Value value){
        if (contains(value) && !getSec(value).equals(sec)){
            throw new MultipleLevelsPerValueException(value);
        }
        valuesPerSec.get(sec).add(value);
        value.forEach(b -> {
            add(b);
            secPerBit.put(b, sec);
        });
        secPerValue.put(value, sec);
    }

    private void add(Bit bit){
        if (bit.val() == B.U){
            bits.add(bit);
        }
    }

    public List<Pair<Sec, Value>> getValues(){
        return valuesPerSec.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new Pair<>((Sec)e.getKey(), v)))
                .collect(Collectors.toList());
    }

    public boolean contains(Bit bit){
        return bits.contains(bit);
    }

    public boolean contains(Value value){
        return valuesPerSec.values().stream().anyMatch(vs -> vs.contains(value));
    }

    public List<Pair<Sec, Bit>> getBits(){
        return bits.stream().map(b -> new Pair<>((Sec)getSec(b), b)).collect(Collectors.toList());
    }

    public List<Bit> getBits(Sec sec){
        return bits.stream().filter(b -> getSec(b) == sec).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return valuesPerSec.entrySet().stream().map(e -> String.format(" level %s: %s",e.getKey(), e.getValue().stream().map(Value::toString).collect(Collectors.joining(", ")))).collect(Collectors.joining("\n"));
    }

    public Sec<?> getSec(Value value){
        return secPerValue.get(value);
    }

    public Sec<?> getSec(Bit bit){
        return secPerBit.get(bit);
    }

    public boolean hasBitWithoutValue(){
        return bits.stream().anyMatch(b -> b.value() == null);
    }
    
    public boolean hasValuesForSec(Sec<?> sec) {
    	return valuesPerSec.get(sec).size() > 0;
    }
}
