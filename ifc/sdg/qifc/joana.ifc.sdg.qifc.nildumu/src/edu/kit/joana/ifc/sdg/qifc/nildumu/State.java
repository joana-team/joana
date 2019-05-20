/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;

/**
 * State of the variables
 */
class State {

    private Value returnValue = vl.bot();

    private final DefaultMap<String, Value> map = new DefaultMap<>(new HashMap<>(), new DefaultMap.Extension<String, Value>() {
        @Override
        public Value defaultValue(Map<String, Value> map, String key) {
            return vl.bot();
        }
    });

    public Value get(String variable){
        return map.get(variable);
    }

    public void set(String variable, Value value){
        this.map.put(variable, value);
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> String.format("%s => %s",e.getKey(), e.getValue().repr())).collect(Collectors.joining("\n"));
    }

    public Set<String> variableNames(){
        return map.keySet();
    }

    public Value getReturnValue(){
        return returnValue;
    }

    public void setReturnValue(Value value){
        this.returnValue = value;
    }
}