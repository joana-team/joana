/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;

/**
 * A simple set of modifications
 */
public class Mods {

    private final Map<Bit, Bit> replacements;

    public Mods(Map<Bit, Bit> replacements) {
        this.replacements = replacements;
    }

    public Mods(Bit orig, Bit repl){
        this(new HashMap<>());
        add(orig, repl);
    }

    public Mods add(Bit orig, Bit repl){
        this.replacements.put(orig, repl);
        return this;
    }

    public Mods add(Mods otherMods){
        for (Map.Entry<Bit, Bit> entry : otherMods.replacements.entrySet()) {
            if (replacements.containsKey(entry.getKey()) && entry.getValue() == replacements.get(entry.getKey())){
                replacements.remove(entry.getKey());
            }
            replacements.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Mods overwrite(Mods otherMods){
        for (Map.Entry<Bit, Bit> entry : otherMods.replacements.entrySet()) {
            replacements.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public String toString() {
        return "(" + replacements.entrySet().stream().map(e -> String.format("%s ↦ %s", e.getKey(), e.getValue())).collect(Collectors.joining(", ")) + ")";
    }

    public boolean definedFor(Bit bit){
        return replacements.containsKey(bit);
    }

    public Bit replace(Bit bit){
        assert definedFor(bit);
        return replacements.get(bit);
    }

    public static Mods empty(){
        return new Mods(new HashMap<>());
    }

    public static Collector<Mods, ?, Mods> collector(){
        return Collectors.collectingAndThen(Collectors.toList(), xs -> {
            Mods mods = Mods.empty();
            xs.forEach(mods::add);
            return mods;
        });
    }

    public Mods merge(Mods other) {
        for (Map.Entry<Bit, Bit> entry : other.replacements.entrySet()) {
            if (!replacements.containsKey(entry.getKey())){
                replacements.put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }
}
