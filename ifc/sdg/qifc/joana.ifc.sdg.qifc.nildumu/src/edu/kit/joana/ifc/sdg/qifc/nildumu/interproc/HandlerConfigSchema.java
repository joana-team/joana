/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * A basic schema that defines the properties (with their possible default values) for each
 * handler class
 */
public class HandlerConfigSchema {

    private final Map<String, String> defaultValues;

    HandlerConfigSchema() {
        defaultValues = new HashMap<>();
    }

    public HandlerConfigSchema add(String param, String defaultValue){
        defaultValues.put(param, defaultValue);
        return this;
    }

    public HandlerConfigSchema add(String param){
        return add(param, null);
    }

    public Properties parse(String props){
        return parse(props, false);
    }

    public Properties parse(String props, boolean allowAnyProps){
        if (!props.contains("=")){
            props = String.format("handler=%s", props);
        }
        Properties properties = new HandlerConfigParser(props).parse();
        for (Map.Entry<String, String> defaulValEntry : defaultValues.entrySet()) {
            if (!properties.containsKey(defaulValEntry.getKey())){
                if (defaulValEntry.getValue() == null){
                    throw new MethodInvocationHandlerInitializationException(String.format("for string \"%s\": property %s not set", props, defaulValEntry.getKey()));
                }
                properties.setProperty(defaulValEntry.getKey(), defaulValEntry.getValue());
            }
        }
        if (!allowAnyProps) {
            for (String prop : properties.stringPropertyNames()) {
                if (!defaultValues.containsKey(prop)) {
                    throw new MethodInvocationHandlerInitializationException(String.format("for string \"%s\": property %s unknown, valid properties are: %s", props, prop, defaultValues.keySet().stream().sorted().collect(Collectors.joining(", "))));
                }
            }
        }
        return properties;
    }
}