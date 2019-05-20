/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.BasicLogger.*;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bs;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap.ForbiddenAction.FORBID_DELETIONS;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap.ForbiddenAction.FORBID_VALUE_UPDATES;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.BasicSecLattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.BitLattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.DependencySet;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Lattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Sec;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.SecurityLattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.ValueLattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Program.NextBlockFilter;
import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.CallSite;
import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.MethodInvocationHandler;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.NildumuException;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box;
import edu.kit.joana.wala.core.PDGNode;

/**
 * The context contains the global state and the global functions from the thesis.
 * <p/>
 * This is this basic idea version, but with the loop extension.
 */
public class Context {

    static class NotAnInputBitException extends NildumuException {
        private NotAnInputBitException(Bit offendingBit, String reason){
            super(String.format("%s is not an input bit: %s", offendingBit.repr(), reason));
        }
    }

    static class InvariantViolationException extends NildumuException {
        private InvariantViolationException(String msg){
            super(msg);
        }
    }
    
    final Program program;
    
    final SecurityLattice<?> sl;

    final int maxBitWidth;

    final IOValues input = new IOValues();

    final IOValues output = new IOValues();
    
    private int unrollCount = 1;

    private final Stack<State> variableStates = new Stack<>();

    private final DefaultMap<Bit, Sec<?>> secMap =
            new DefaultMap<>(
                    new IdentityHashMap<>(),
                    new DefaultMap.Extension<Bit, Sec<?>>() {
                        @Override
                        public Sec<?> defaultValue(Map<Bit, Sec<?>> map, Bit key) {
                            return sl.bot();
                        }
                    },
                    FORBID_DELETIONS,
                    FORBID_VALUE_UPDATES);

    private final DefaultMap<SDGNode, Operator> operatorPerNode = new DefaultMap<>(new IdentityHashMap<>(), new DefaultMap.Extension<SDGNode, Operator>() {

        @Override
        public Operator defaultValue(Map<SDGNode, Operator> map, SDGNode node) {
          	return operatorForNodeNotCached(node);
        }
    }, FORBID_DELETIONS, FORBID_VALUE_UPDATES);

    static class CallPath {
        private final List<CallSite> path;

        CallPath(){
            this(Collections.emptyList());
        }

        CallPath(List<CallSite> path) {
            this.path = path;
        }

        CallPath push(CallSite callSite){
            List<CallSite> newPath = new ArrayList<>(path);
            newPath.add(callSite);
            return new CallPath(newPath);
        }

        CallPath pop(){
            List<CallSite> newPath = new ArrayList<>(path);
            newPath.remove(newPath.size() - 1);
            return new CallPath(newPath);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CallPath && ((CallPath) obj).path.equals(path);
        }

        @Override
        public String toString() {
            return path.stream().map(Object::toString).collect(Collectors.joining(" → "));
        }

        public boolean isEmpty() {
            return path.isEmpty();
        }

        public CallSite peek() {
            return path.get(path.size() - 1);
        }
    }

    public static class NodeValueState {

        final DefaultMap<SDGNode, Value> nodeValueMap = new DefaultMap<>(new LinkedHashMap<>(), new DefaultMap.Extension<SDGNode, Value>() {

            @Override
            public Value defaultValue(Map<SDGNode, Value> map, SDGNode key) {
                return ValueLattice.get().parse("0bxx");
            }
        }, FORBID_DELETIONS);

        final DefaultMap<SDGNode, Integer> count = new DefaultMap<>((map, key) -> 0);
        
        final CallPath path;
        
        final Method method;
        
        final Map<AffectingConditional, Mods> modsMap = new HashMap<>();

        private NodeValueState(CallPath path, Method method) {
            this.path = path;
            this.method = method;
        }
    }
    

    private final Map<CallPath, NodeValueState> nodeValueStates = new HashMap<>();

    private CallPath currentCallPath = new CallPath();

    private NodeValueState nodeValueState;

    private final DefaultMap<Bit, ModsCreator> replMap = new DefaultMap<>((map, bit) -> {
    	return ((c, b, a) -> choose(b, a) == a ? new Mods(b, a) : Mods.empty());
    });
    
    /*-------------------------- loop mode specific -------------------------------*/

    private final HashMap<Bit, Integer> weightMap = new HashMap<>();

    public static final int INFTY = Integer.MAX_VALUE;

    /*-------------------------- methods -------------------------------*/

    private MethodInvocationHandler methodInvocationHandler;

    private Stack<Set<Bit>> methodParameterBits = new Stack<>();

    /*-------------------------- unspecific -------------------------------*/

    Context(Program program) {
        this.sl = BasicSecLattice.get();
        this.maxBitWidth = program.intWidth;
        this.variableStates.push(new State());
        ValueLattice.get().bitWidth = maxBitWidth;
        this.program = program;
        nodeValueStates.put(currentCallPath, new NodeValueState(currentCallPath, program.main));
        nodeValueState = nodeValueStates.get(currentCallPath);
    }

    public static B v(Bit bit) {
        return bit.val();
    }

    public static DependencySet d(Bit bit) {
        return bit.deps();
    }

    /**
     * Returns the security level of the bit
     *
     * @return sec or bot if not assigned
     */
    public Sec sec(Bit bit) {
        return secMap.get(bit);
    }

    /**
     * Sets the security level of the bit
     *
     * <p><b>Important note: updating the security level of a bit is prohibited</b>
     *
     * @return the set level
     */
    private Sec sec(Bit bit, Sec<?> level) {
        return secMap.put(bit, level);
    }

    public Value addInputValue(Sec<?> sec, Value value){
        input.add(sec, value);
        for (Bit bit : value){
            if (bit.val() == B.U){
                if (!bit.deps().isEmpty()){
                    throw new NotAnInputBitException(bit, "has dependencies");
                }
                sec(bit, sec);
            }
        }
        return value;
    }

    Value addOutputValue(Sec<?> sec, Value value){
        output.add(sec, value);
        return value;
    }

    boolean isInputBit(Bit bit) {
        return input.contains(bit);
    }

    Value nodeValue(SDGNode node){
        return nodeValueState.nodeValueMap.get(node);
    }

    Value nodeValue(SDGNode node, Value value){
        return nodeValueState.nodeValueMap.put(node, value);
    }
    
    boolean hasNodeValue(SDGNode node) {
    	return nodeValueState.nodeValueMap.containsKey(node);
    }

    Operator operatorForNode(SDGNode node){
        return operatorPerNode.get(node);
    }

    private Value op(SDGNode node, List<Value> arguments){
    	Operator operator = operatorForNode(node);
    	if (operator == null) {
    		return new Value(bl.create(B.X));
    	}
        return operatorForNode(node).compute(this, node, arguments);
    }

    private List<Value> opArgs(SDGNode node, Function<SDGNode, Value> nodeToValue, List<SDGNode> directParamNodes){
    	SSAInstruction instr = program.getInstruction(node);
    	if (instr == null) {
    		return Collections.emptyList();
    	}
    	List<AffectingConditional> affectingConds = instr instanceof SSAPhiInstruction ? 
    			 nodeValueState.method.getDoms().getPhiOperandAffectingConditionals(node) : null;
    	SymbolTable st = program.getProcSymbolTable(node);
    	Box<Integer> edgeIndex = new Box<>(0); 
    	return IntStream.range(0, instr.getNumberOfUses()).mapToObj(i -> {
    		int use = instr.getUse(i);
    		if (st.isConstant(use)) {
    			Object val = ((ConstantValue)st.getValue(use)).getValue();
    			if (st.isNumberConstant(use)) {
    				return vl.parse(((Number)val).intValue());
    			}
    			if (st.isBooleanConstant(use)) {
    				return vl.parse(((Boolean)val).booleanValue() ? 1 : 0);
    			}
    			throw new NildumuException(String.format("Unsupported constant type %s", val.getClass()));
    		}
    		Value val = null;
    		if (st.isParameter(use)) {
    			val = getParamValue(node, use);
    		} else {
    			val = nodeToValue.apply(directParamNodes.get(edgeIndex.val));
    		}
    		edgeIndex.val++;
    		if (affectingConds != null) {
    			val = replace(Optional.of(affectingConds.get(i)), val);
    		}
    		return val;
    	}).collect(Collectors.toList());
    }
    
    private List<Value> opArgs(SDGNode node){
    	return opArgs(node, this::nodeValue,
    			program.sdg.getIncomingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP).stream()
    			.map(SDGEdge::getSource).collect(Collectors.toList()));
    }
   
    boolean evaluate(SDGNode node){
    	//System.err.println(node.getLabel());
    	final SDGNode resNode;
    	if (node.kind == Kind.CALL) {
    		PDGNode pdgNode = program.getPDG(node).getReturnOut(program.getPDG(node).getNodeWithId(node.getId()));
    		resNode = program.sdg.getNode(pdgNode.getId());
    	} else {
    		resNode = node;
    	}
    	
    	log(" ### " +resNode.getLabel());
    	
    	Value newValue = null;
        if (node.kind == Kind.CALL) {
        	newValue = evaluateCall(node);
        } else {
        	List<Value> args = opArgs(node);
    		
        	newValue = op(node, args);
        }
        log(newValue.repr());
        
        boolean somethingChanged = false;
        if (hasNodeValue(resNode) && nodeValueState.count.get(resNode) >= unrollCount) { // dismiss first iteration
            Value oldValue = nodeValue(resNode);
            somethingChanged = merge(oldValue, newValue);
        } else {
        	nodeValue(resNode, newValue);
            somethingChanged = true;
        }
        nodeValueState.count.put(resNode, nodeValueState.count.get(resNode) + 1);
        newValue.description(node.getLabel()).node(node);
        return somethingChanged;
    }

    /**
     * Returns the unknown output bits with lower or equal security level
     */
    public List<Bit> getOutputBits(Sec<?> maxSec){
        return output.getBits().stream().filter(p -> ((SecurityLattice)sl).lowerEqualsThan(p.first, (Sec)maxSec)).map(p -> p.second).collect(Collectors.toList());
    }

    /**
     * Returns the unknown input bits with not lower security or equal level
     */
    public List<Bit> getInputBits(Sec<?> minSecEx){
        return input.getBits().stream().filter(p -> !(((SecurityLattice)sl).lowerEqualsThan(p.first, (Sec)minSecEx))).map(p -> p.second).collect(Collectors.toList());
    }

    private Value setVariableValue(String variable, Value value){
        if (variableStates.size() == 1) {
            if (!variableStates.get(0).get(variable).equals(vl.bot())) {
                throw new UnsupportedOperationException(String.format("Setting an input variable (%s)", variable));
            }
        }
        variableStates.peek().set(variable, value);
        return value;
    }
    
    /**
     * Parameter indexes start at 1 (as the 0th parameter is reserved for {@code this},
     * which isn't currently supported)
     * 
     * @param i parameter index
     * @param value value of the parameter
     */
    public void setParamValue(int i, Value value) {
    	assert i > 0;
    	setVariableValue(i + "", value);
    }

    public Value getVariableValue(String variable){
        return variableStates.peek().get(variable);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Variable states\n");
        for (int i = 0; i < variableStates.size(); i++){
            builder.append(variableStates.get(i));
        }
        builder.append("Input\n" + input.toString()).append("Output\n" + output.toString());
        return builder.toString();
    }

    public boolean isInputValue(Value value){
        return input.contains(value);
    }

    public Sec<?> getInputSecLevel(Value value){
        assert isInputValue(value);
        return input.getSec(value);
    }

    public Sec<?> getInputSecLevel(Bit bit){
        return input.getSec(bit);
    }

    /**
     * Walk in pre order
     * @param ignoreBit ignore bits (and all that depend on it, if not reached otherwise)
     */
    public void walkBits(Consumer<Bit> consumer, Predicate<Bit> ignoreBit){
        Set<Bit> alreadyVisitedBits = new HashSet<>();
        for (Pair<Sec, Bit> secBitPair : output.getBits()){
            BitLattice.get().walkBits(secBitPair.second, consumer, ignoreBit, alreadyVisitedBits);
        }
    }

    private Map<Sec<?>, MinCut.ComputationResult> leaks = null;


    public Map<Sec<?>, MinCut.ComputationResult> computeLeakage(){
        if (leaks == null){
            leaks = MinCut.compute(this);
        }
        return leaks;
    }

    private int c1(Bit bit){
        Queue<Bit> q = new ArrayDeque<>();
        Set<Bit> alreadyVisitedBits = new HashSet<>();
        q.add(bit);
        Set<Bit> anchors = new HashSet<>();
        while (!q.isEmpty()) {
        	Bit cur = q.poll();
        	if ((!currentCallPath.isEmpty() && methodParameterBits.peek().contains(cur)) ||
        			isInputBit(cur) && sec(cur) != sl.bot()) {
        		anchors.add(cur);
        	} else {
        	cur.deps().stream().filter(Bit::isUnknown).filter(b -> {
                if (alreadyVisitedBits.contains(b)) {
                    return false;
                }
                alreadyVisitedBits.add(b);
                return true;
            }).forEach(q::offer);
        	}
        }
        return anchors.size();
    }

    /* -------------------------- extended mode specific -------------------------------*/
    
    public Bit choose(Bit a, Bit b){
        if (c1(a) <= c1(b) || a.isConstant()){
            return a;
        }
        return b;
    }

    public Bit notChosen(Bit a, Bit b){
        if (choose(a, b) == b){
            return a;
        }
        return b;
    }
    
    public Bit replace(Optional<AffectingConditional> cond, Bit bit){
        if (cond.isPresent()) {
        	if (nodeValueState.modsMap.getOrDefault(cond.get(), Mods.empty()).definedFor(bit)) {
        		return nodeValueState.modsMap.get(cond.get()).replace(bit);
        	}
        	return replace(nodeValueState.method.getDoms().getAffectingConditional(cond.get().conditional), bit);
        }
        return bit;
    }

    public Value replace(Optional<AffectingConditional> cond, Value value) {
        Box<Boolean> replacedABit = new Box<>(false);
        Value newValue = value.stream().map(b -> {
            Bit r = replace(cond, b);
            if (r != b){
                replacedABit.val = true;
            }
            return r;
        }).collect(Value.collector());
        if (replacedABit.val){
            return newValue;
        }
        return value;
    }

    public void repl(Bit bit, ModsCreator modsCreator){
        replMap.put(bit, modsCreator);
    }
    
    public void removeRepl(Bit bit) {
    	replMap.remove(bit);
    }

    /**
     * Applies the repl function to get mods
     * @param bit
     * @param assumed
     */
    public Mods repl(Bit bit, Bit assumed){
        return repl(bit).apply(this, bit, assumed);
    }

    public ModsCreator repl(Bit bit){
        return replMap.get(bit);
    }

    public void addMods(SDGNode condNode, Bit condBit){
    	assert condNode.kind == Kind.PREDICATE;
    	List<B> assumedValues = new ArrayList<>();
    	if (condBit.isUnknown() || condBit.val() == B.ONE) {
    		assumedValues.add(B.ONE);
    	}
    	for (B assumedValue : Arrays.asList(B.ZERO, B.ONE)) {
    		if (condBit.isUnknown() || condBit.val() == assumedValue) {
    			AffectingConditional affCond = new AffectingConditional(condNode, assumedValue == B.ONE);
    			Mods mods = repl(condBit).apply(this, condBit, bl.create(assumedValue));
    			if (nodeValueState.modsMap.containsKey(affCond)) {
    				mods = Mods.empty().add(nodeValueState.modsMap.get(affCond)).merge(mods);
    			}
    			nodeValueState.modsMap.put(affCond, mods);
    		}
    	}
    }
    
    /* -------------------------- loop mode specific -------------------------------*/

    public int weight(Bit bit){
        return weightMap.getOrDefault(bit, 1);
    }

    public void weight(Bit bit, int weight){
        assert weight == 1 || weight == INFTY;
        if (weight == 1){
        	weightMap.remove(bit, weight);
            return;
        }
        weightMap.put(bit, weight);
    }

    public boolean hasInfiniteWeight(Bit bit){
        return weight(bit) == INFTY;
    }

    /**
     * merges n into o
     * @param o
     * @param n
     * @return true if o value equals the merge result
     */
    private boolean merge(Bit o, Bit n){
        B vt = bs.sup(v(o), v(n));
        int oldDepsCount = o.deps().size();
        boolean somethingChanged = false;
        if (vt != v(o)) {
        	o.setVal(vt);
        	somethingChanged = true;
        }
        o.addDependencies(d(n));
        if (oldDepsCount == o.deps().size() && !somethingChanged){
        	replMap.remove(n);
            return false;
        }
        ModsCreator oModsCreator = repl(o);
        ModsCreator nModsCreator = repl(n);
        repl(o, (c, b, a) -> {
            Mods oMods = oModsCreator.apply(c, b, a);
            Mods nMods = nModsCreator.apply(c, b, a);
            return Mods.empty().add(oMods).merge(nMods);
        });
        replMap.remove(n);
        return true;
    }
    
    /**
     * merges n into o
     * @param o
     * @param n
     * @return true if o value equals the merge result
     */
    private boolean merge(Value oldValue, Value newValue){
    	boolean somethingChanged = false;
    	int i = 1;
    	for (; i <= Math.min(oldValue.size(), newValue.size()); i++){
            somethingChanged = merge(oldValue.get(i), newValue.get(i)) || somethingChanged;
        }
    	for (; i <= newValue.size(); i++){
    		oldValue.add(newValue.get(i));
    		somethingChanged = true;
    	}
        return somethingChanged;
    }

    public void setReturnValue(Value value){
        variableStates.get(variableStates.size() - 1).setReturnValue(value);
    }

    public Value getReturnValue(){
        return variableStates.get(variableStates.size() - 1).getReturnValue();
    }

    /*-------------------------- methods -------------------------------*/

    public Context forceMethodInvocationHandler(MethodInvocationHandler handler) {
        methodInvocationHandler = handler;
        return this;
    }

    public void methodInvocationHandler(MethodInvocationHandler handler) {
        assert methodInvocationHandler == null;
        methodInvocationHandler = handler;
    }

    public MethodInvocationHandler methodInvocationHandler(){
        if (methodInvocationHandler == null){
            methodInvocationHandler(MethodInvocationHandler.createDefault());
        }
        return methodInvocationHandler;
    }

    public void pushNewMethodInvocationState(CallSite callSite, List<Value> arguments){
        pushNewMethodInvocationState(callSite,
        		arguments.stream().flatMap(Value::stream).collect(Collectors.toSet()));
    }

    public void pushNewMethodInvocationState(CallSite callSite, Set<Bit> argumentBits){
        currentCallPath = currentCallPath.push(callSite);
        variableStates.push(new State());
        methodParameterBits.push(argumentBits);
        if (!nodeValueStates.containsKey(currentCallPath)) {
        	nodeValueStates.put(currentCallPath, new NodeValueState(currentCallPath, callSite.method));
        }
        nodeValueState = nodeValueStates.get(currentCallPath);
    }

    public void popMethodInvocationState(){
        currentCallPath = currentCallPath.pop();
        variableStates.pop();
        methodParameterBits.pop();
        nodeValueState = nodeValueStates.get(currentCallPath);
    }

    public CallPath callPath(){
        return currentCallPath;
    }

    public int numberOfMethodFrames(){
        return nodeValueStates.size();
    }

    public int numberOfinfiniteWeightNodes(){
        return weightMap.size();
    }

    public void resetNodeValueStates(){
        nodeValueStates.clear();
        nodeValueState = nodeValueStates.get(currentCallPath);
    }

    public Set<Bit> sources(Sec<?> sec){
        return  sl
                .elements()
                .stream()
                .map(s -> (Sec<?>) s)
                .filter(s -> ((Lattice) sl).lowerEqualsThan(s, sec))
                .flatMap(s -> output.getBits((Sec) s).stream())
                .collect(Collectors.toSet());
    }

    public Set<Bit> sinks(Sec<?> sec){
        // an attacker at level sec can see all outputs with level <= sec
        return   sl
                .elements()
                .stream()
                .map(s -> (Sec<?>) s)
                .filter(s -> !((Lattice) sl).lowerEqualsThan(s, sec))
                .flatMap(s -> input.getBits((Sec) s).stream())
                .collect(Collectors.toSet());
    }
    
    /**
     * Does handle parameters, data dependencies and constants to return the
     * correct value
     * @param node
     * @return
     */
    private Value nodeValueRec(SDGNode node) {
    	if (nodeValueState.nodeValueMap.containsKey(node)) {
    		return nodeValueState.nodeValueMap.get(node);
    	}
    	if (node.kind == Kind.ACTUAL_IN) {
        	for (SDGEdge.Kind kind : Arrays.asList(SDGEdge.Kind.DATA_DEP)) {
        		List<SDGEdge> edges = program.sdg.getIncomingEdgesOfKind(node, kind);
        		if (edges.size() > 0) {
        			return nodeValueRec(edges.get(0).getSource());
        		}
        	}
    	}
    	return vl.bot();
    	/*System.err.println(node.getLabel());
    	assert false;
    	return vl.parse(program.getConstantInLabel(node.getLabel()));*/
    }
    
    /**
     * From the Java SE 8 vm spec:
     * 
     * The Java Virtual Machine uses local variables to pass parameters 
     * on method invocation. On class method invocation, any parameters
     * are passed in consecutive local variables starting from local 
     * variable 0. On instance method invocation, local variable 0 is 
     * always used to pass a reference to the object on which the 
     * instance method is being invoked (this in the Java programming 
     * language). Any parameters are subsequently passed in consecutive 
     * local variables starting from local variable 1. 
     */
    private Value getParamValue(SDGNode base, int useId) {
    	return getVariableValue(useId + "");
    }
    
    /**
     * Handles call to {@link CodeUI#output(int, String)} and {@link CodeUI#leak(int)}
     * @param callSite
     */
    private void handleOutputCall(SDGNode callSite) {
		assert isOutputCall(callSite);
		java.lang.reflect.Method method = program.getJavaMethodCallTarget(callSite);
		assert method.getName().equals("output") || method.getName().equals("leak");
		List<SDGNode> param = program.getParamNodes(callSite);
		SSAInvokeInstruction instr = (SSAInvokeInstruction)program.getInstruction(callSite);
    	SymbolTable st = program.getProcSymbolTable(callSite);
		Value value = null;
		int use = instr.getUse(0);
		if (st.isParameter(use)){
			value = getParamValue(callSite, use);
		} else if (st.isConstant(use)) {
			Object val = ((ConstantValue)st.getValue(use)).getValue();
			if (st.isNumberConstant(use)) {
				value = vl.parse(((Number)val).intValue());
			} else if (st.isBooleanConstant(use)) {
				value = vl.parse(((Boolean)val).booleanValue() ? 1 : 0);
			} else {
				throw new NildumuException(String.format("Unsupported constant type %s", val.getClass()));
			}
		} else {
			value = nodeValueRec(param.get(0));
		}
		switch (method.getName()) {
		case "output":
			assert st.isStringConstant(instr.getUse(1));
			Sec<?> sec = sl.parse(st.getStringValue(instr.getUse(1)));
			addOutputValue(sec, value);
			break;
		case "leak":
			addOutputValue(sl.bot(), value);
		}
		//program.builder.getClassHierarchy().getRootClass().getAllMethods().iterator().next().getAnnotations().iterator().next().
    }
	
	private boolean isOutputCall(SDGNode callSite) {
		return callSite.kind == Kind.CALL && program.isOutputMethodCall(callSite);
	}
	
	private Value evaluateCall(SDGNode callSite) {
		assert callSite.kind == Kind.CALL;
		List<Value> args = opArgs(callSite, this::nodeValueRec,program.getParamNodes(callSite));
		return methodInvocationHandler.analyze(this, 
				new CallSite.NodeBasedCallSite(program.getMethodForCallSite(callSite), callSite), args);
	}
	
	/**
	 * Extension of that handles {@link CodeUI#output(int, String)} and {@link CodeUI#leak(int)} calls
	 * @param entryNode
	 * @param nodeConsumer
	 */
	public void workList(SDGNode entryNode, Predicate<SDGNode> nodeConsumer,
			NextBlockFilter nextBlockFilter) {
		program.workList(entryNode, n -> {
			if (isOutputCall(n)) {
				handleOutputCall(n);
				return false;
			} else {
				return nodeConsumer.test(n);
			}
		}, nextBlockFilter);
	}
	
	public void registerLeakageGraphs() {
		sl.elements().forEach(s -> {
            DotRegistry.get().store("main", "Attacker level: " + s,
                    () -> () -> LeakageCalculation.visuDotGraph(this, "", s));
			});
	}
	
	public void storeLeakageGraphs() {
		registerLeakageGraphs();
		DotRegistry.get().storeFiles();
	}
	
	/**
	 * Returns the operator for the passed node or {@code null} if the node can be ignored.
	 * @param node
	 * @return
	 */
	Operator operatorForNodeNotCached(SDGNode node) {
       	if (node.getLabel().equals("return")) {
    		return null;
    	}
       	Box<Operator> op = new Box<>(null);
       	//System.err.println(node.getLabel());
       	//program.getProcIR(node).getPD
       	SSAInstruction instr = program.getInstruction(node);
       	instr.visit(new Visitor() {
       		@Override
       		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
       			IOperator wop = instruction.getOperator();
       			if (wop instanceof IShiftInstruction.Operator) {
       				switch ((IShiftInstruction.Operator)wop) {
       				case SHL:
       					op.val = Operator.LEFT_SHIFT;
       					break;
       				case SHR:
       					op.val = Operator.RIGHT_SHIFT;
       					break;
       				}
       				return;
       			}
       			switch ((IBinaryOpInstruction.Operator)wop) {
				case OR:
					op.val = Operator.OR;
					break;
				case ADD:
					op.val = Operator.ADD;
					break;
				case AND:
					op.val = Operator.AND;
					break;
				case DIV:
					op.val = Operator.DIVIDE;
					break;
				case MUL:
					op.val = Operator.MULTIPLY;
					break;
				case REM:
					op.val = Operator.MODULO;
					break;
				case SUB:
					op.val = new Operator() {
						
						public Value compute(Context c, SDGNode node, java.util.List<Value> arguments) {
							Value negated = Operator.NOT.compute(c, node, Collections.singletonList(arguments.get(1)));
							Value addMinusOne = Operator.ADD.compute(c, node, 
									Arrays.asList(arguments.get(0), negated));
							return Operator.ADD.compute(c, node, 
									Arrays.asList(addMinusOne, vl.parse(1)));
						}
						
						@Override
						public String toString(List<Value> arguments) {
							return String.format("(%s - %s)", arguments.get(0), arguments.get(1));
						}
					};
					break;
				case XOR:
					op.val = Operator.XOR;
					break;
				default:
					break;
				}
       		}       	
       		
       		@Override
       		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
       			switch ((IUnaryOpInstruction.Operator)instruction.getOpcode()) {
       			case NEG:
       				op.val = Operator.NOT;
       			}
       		}
       		
       		@Override
       		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
       			switch ((IConditionalBranchInstruction.Operator)instruction.getOperator()) {
				case EQ:
					op.val = Operator.EQUALS;
					break;
				case GE:
					// → x >= y ? C1 : C2 === x < y ? C2 : C1
					op.val = Operator.negate(Operator.LESS);
					break;
				case GT:
					op.val = Operator.reverseArguments(Operator.LESS);
					break;
				case LE:
					op.val = Operator.reverseArguments(Operator.negate(Operator.LESS));
					break;
				case LT:
					op.val = Operator.LESS;
					break;
				case NE:
					op.val = Operator.UNEQUALS;
					break;
				}
       		}
       		
       		@Override
       		public void visitPhi(SSAPhiInstruction instruction) {
       			op.val = Operator.PHI_GENERIC;
       		}
       		
       		@Override
       		public void visitReturn(SSAReturnInstruction instruction) {
       			op.val = Operator.RETURN;
       		}
       	});
       	//System.err.println(instr);
        if (op.val == null){
            throw new NildumuException(String.format("No operator for %s implemented", Program.toString(node)));
        }
        return op.val;
	}
	
	public void fixPointIteration(SDGNode entryNode) {
		new FixpointIteration(entryNode).run();
	}
	
	private class FixpointIteration extends Visitor {
		
		private final SDGNode entryNode;
		private final Map<SDGNode, ISSABasicBlock> omittedBlocks = new HashMap<>();
		private Set<ISSABasicBlock> omitNextTime = new HashSet<>();
		private boolean changed = false;
		private SDGNode node = null; 
		private Set<SDGNode> partOfLoopConditionNodes;
		private Method method;
		
		private FixpointIteration(SDGNode entryNode) {
			super();
			this.entryNode = entryNode;
			this.method = nodeValueState.method;
		}
		
		/**
		 * Every node that a loop condition node transitively data depends on, excluding
		 * dependencies through comparisons.
		 */
		Set<SDGNode> calculatePartOfLoopConditionNodes(){
			Set<SDGNode> nodes = program.getSDG().getNodesOfProcedure(entryNode);
			Set<SDGNode> comps = nodes.stream()
					.filter(n -> program.getInstruction(n) instanceof SSAConditionalBranchInstruction)
					.collect(Collectors.toSet());
			Set<SDGNode> partOfLoopConds = new HashSet<>();
			for (SDGNode comp : comps) {
				if (method.isPartOfLoop(comp)) {
					// start at a branch condition
					// use breadth first search
					Queue<SDGNode> q = new ArrayDeque<>();
				    // the comp it self is part of a loop condition
					partOfLoopConds.add(comp);
					program.getDataDependencies(comp).forEach(q::add);
					Set<SDGNode> alreadyVisited = new HashSet<>();
					while (!q.isEmpty()) {
						// all nodes in the queue are part of a loop cond
						// if they are not a comparison themselves
						// unless they are in a loop
						SDGNode cur = q.poll();
						alreadyVisited.add(cur);
						if (!comps.contains(cur) && isNotComparison(cur)) {
							partOfLoopConds.add(cur);
							program.getDataDependencies(cur)
								.filter(n -> !alreadyVisited.contains(n))
								.forEach(q::add);
						}
					}
				}
			}
			return partOfLoopConds;
		}
		
		private boolean isNotComparison(SDGNode node) {
			SSAInstruction instr = program.getInstruction(node);
			if (instr == null) {
				return false;
			}
			Operator op = operatorForNodeNotCached(node);
			return !(op == Operator.EQUALS || op == Operator.UNEQUALS || op == Operator.LESS);
		}
		
		private boolean isLogicalOpOrPhi(SDGNode node) {
			SSAInstruction instr = program.getInstruction(node);
			if (instr == null) {
				return false;
			}
			Operator op = operatorForNodeNotCached(node);
			return op == Operator.PHI_GENERIC || op == Operator.AND || op == Operator.NOT || op == Operator.OR || op == Operator.XOR;
		}

		private void run() {
			this.partOfLoopConditionNodes = calculatePartOfLoopConditionNodes();
            workList(entryNode, n -> {
            	if (n.getLabel().equals("many2many")) {
					return false;
				}
				node = n;
				program.getInstruction(n).visit(this);
				return changed;
			}, new NextBlockFilter() {
				
				@Override
				public boolean test(ISSABasicBlock b) {
					return !omitNextTime.contains(b);
				}
				
				@Override
				public void clear() {
					omitNextTime.clear();
				}
			});	
		}
		
		@Override
		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			evaluate(node);
		}
		
		@Override
		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			evaluate(node);
		}
		
		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			if (evaluate(node)) {
				Value cond = nodeValue(node);
				Bit condBit = cond.get(1);
                B condVal = condBit.val();
                if (condVal == B.U && method.isPartOfLoop(node)){
                    weight(condBit, Context.INFTY);
                }
                if (condVal == B.ZERO && condVal != B.U) {
                	omitBlock(node, program.blockForId(node, instruction.getTarget()));
                }
                if (condVal == B.ONE && condVal != B.U) {
                	omitBlock(node, program.getNextBlock(node));
                }
                addMods(node, condBit);
			}
		}
		
		@Override
		public void visitGoto(SSAGotoInstruction instruction) {
			changed = true;
		}
		
		private boolean conditionInLoopDirectlyDependsOn(SDGNode node) {
			return program.getSDG().getOutgoingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP).stream()
					.map(SDGEdge::getTarget)
					.anyMatch(method::isPartOfLoop);
		}
		
		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
			evaluate(node);
			program.getControlDeps(node).forEach(n -> {
				if (omittedBlocks.containsKey(n)) {
					omittedBlocks.remove(n);
				}
			});
		}
		
		@Override
		public void visitInvoke(SSAInvokeInstruction instruction) {
			evaluate(node);
		}
   		
   		@Override
   		public void visitReturn(SSAReturnInstruction instruction) {
   			evaluate(node);
   		}
		
		private boolean evaluate(SDGNode node) {
			changed = Context.this.evaluate(node);
			if (partOfLoopConditionNodes.contains(node)) {
				nodeValue(node).forEach(b -> weight(b, INFTY));
			}
			log("Evaluation of node %s changed", node);
			return changed;
		}
		
		private void omitBlock(SDGNode condNode, ISSABasicBlock block) {
			if (block != null) {
				omittedBlocks.put(condNode, block);
				omitNextTime.add(block);
			}
		}
	}
	
	public void printLeakages() {
		computeLeakage().forEach((sec, res) -> {
			System.out.println(String.format("%10s: %d bit", sec, res.maxFlow));
		}); 
	}
	
	public BasicBlockGraph getCurrentBasicBlockGraph() {
		return nodeValueState.method.getDoms();
	}
}
