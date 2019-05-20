/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.BasicLogger.*;

import java.util.List;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Context;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Method;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Program;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;

/**
 * A call string based handler that just inlines a function.
 * If a function was inlined in the current call path more than a defined number of times,
 * then another handler is used to compute a conservative approximation.
 */
public class InliningHandler extends MethodInvocationHandler {
	
    final int maxRec;

    final MethodInvocationHandler botHandler;

    private DefaultMap<Method, Integer> methodCallCounter = new DefaultMap<>((map, method) -> 0);

    private Program program = null;
    
    InliningHandler(int maxRec, MethodInvocationHandler botHandler) {
        this.maxRec = maxRec;
        this.botHandler = botHandler;
    }

    @Override
    public void setup(Program program) {
        botHandler.setup(program);
        this.program = program;
    }

    @Override
    public Value analyze(Context c, CallSite callSite, List<Value> arguments) {
        Method method = callSite.method;
        log(() -> String.format("                Arguments for rec depth %d: %s\n", 
        		methodCallCounter.get(method), arguments));
        if (methodCallCounter.get(method) < maxRec) {
            methodCallCounter.put(method, methodCallCounter.get(method) + 1);
            c.pushNewMethodInvocationState(callSite, arguments);
            for (int i = 0; i < arguments.size(); i++) {
                c.setParamValue(i + 1, arguments.get(i));
            }
            c.fixPointIteration(method.entry);
            Value ret = c.getReturnValue();
            c.popMethodInvocationState();
            methodCallCounter.put(method, methodCallCounter.get(method) - 1);
            return ret;
        }
        return botHandler.analyze(c, callSite, arguments);
    }
    
    @Override
    public String getName() {
    	return "inlining";
    }
}