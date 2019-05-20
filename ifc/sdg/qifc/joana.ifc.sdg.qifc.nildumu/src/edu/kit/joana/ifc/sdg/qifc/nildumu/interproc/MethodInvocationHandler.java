/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B.U;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.p;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Context;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Method;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Program;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.DependencySet;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;

/**
 * Handles the analysis of methods → implements the interprocedural part of the analysis.
 *
 * Handler classes can be registered and configured via property strings.
 */
public abstract class MethodInvocationHandler {

    private static Map<String, Pair<HandlerConfigSchema, Function<Properties, MethodInvocationHandler>>> registry = new HashMap<>();

    private static List<String> examplePropLines = new ArrayList<>();

    /**
     * Register a new class of handlers
     */
    private static void register(String name, Consumer<HandlerConfigSchema> propSchemeCreator, Function<Properties, MethodInvocationHandler> creator){
        HandlerConfigSchema scheme = new HandlerConfigSchema();
        propSchemeCreator.accept(scheme);
        scheme.add("handler", null);
        registry.put(name, p(scheme, creator));
    }

    
    /**
     * Returns the handler for the passed string, the property "handler" defines the handler class
     * to be used
     */
    public static MethodInvocationHandler parse(String props){
        Properties properties = new HandlerConfigSchema().add("handler").parse(props, true);
        String handlerName = properties.getProperty("handler");
        if (!registry.containsKey(handlerName)){
            throw new MethodInvocationHandlerInitializationException(String.format("unknown handler %s, possible handlers are: %s", handlerName, registry.keySet()));
        }
        try {
            Pair<HandlerConfigSchema, Function<Properties, MethodInvocationHandler>> pair = registry.get(handlerName);
            return pair.second.apply(pair.first.parse(props));
        } catch (MethodInvocationHandlerInitializationException error){
            throw error;
        } catch (Error error){
            throw new MethodInvocationHandlerInitializationException(String.format("parsing \"%s\": %s", props, error.getMessage()));
        }
    }
    
    public static MethodInvocationHandler parseAndSetup(Program program, String props){
    	MethodInvocationHandler handler = parse(props);
    	handler.setup(program);
    	return handler;
    }

    public static List<String> getExamplePropLines(){
        return Collections.unmodifiableList(examplePropLines);
    }
    
    static {
        register("all", s -> {}, ps -> new MethodInvocationHandler(){
            @Override
            public Value analyze(Context c, CallSite callSite, List<Value> arguments) {
                if (arguments.isEmpty() || !callSite.method.hasReturnValue()){
                    return vl.bot();
                }
                DependencySet set = arguments.stream().flatMap(Value::stream).collect(DependencySet.collector());
                return IntStream.range(0, arguments.stream().mapToInt(Value::size).max().getAsInt()).mapToObj(i -> bl.create(U, set)).collect(Value.collector());
            }
            
            public String getName() {
            	return "all";
            }
        });
        examplePropLines.add("handler=all");
        register("inlining", s -> s.add("maxrec", "2").add("bot", "all"), ps -> {
            return new InliningHandler(Integer.parseInt(ps.getProperty("maxrec")), parse(ps.getProperty("bot")));
        });
        examplePropLines.add("handler=inlining;maxrec=2;bot=all");
        examplePropLines.add("handler=inlining;maxrec=2;bot=summary");
        Consumer<HandlerConfigSchema> propSchemeCreator = s ->
                s.add("reduction", "mincut").add("dot", "").add("csmaxrec", "2");
        register("summary", propSchemeCreator, ps -> {
            Path dotFolder = ps.getProperty("dot").equals("") ? null : Paths.get(ps.getProperty("dot"));
            return new SummaryHandler(dotFolder, SummaryHandler.Reduction.valueOf(ps.getProperty("reduction").toUpperCase()), Integer.parseInt(ps.getProperty("csmaxrec")));
        });
        examplePropLines.add("handler=summary;reduction=basic");
        examplePropLines.add("handler=summary;reduction=mincut");
        //examplePropLines.add("handler=summary_mc;mode=ind");
    }

    public static MethodInvocationHandler createDefault(){
        return parse(getDefaultPropString());
    }

    public static String getDefaultPropString(){
        return "handler=inlining;maxrec=2;bot=all";
    }

    /**
     * Setup the handler for a given program
     */
    public void setup(Program program){
    }

    /**
     * Analyse a call-site and return the return the return value
     */
    public abstract Lattices.Value analyze(Context c, CallSite callSite, List<Value> arguments);

    public abstract String getName();
}
