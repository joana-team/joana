package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import edu.kit.joana.util.Triple;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class RecursiveReturnValue<T> implements IReturnValue<T>, IRecursiveReturnValue<T> {

	private Method m;
	private SSAInvokeInstruction recCall;
	private int[] paramValNums;
	private int[] argValNums;
	private T returnValVars;
	private Set<Variable> recVars;

	private final TreeSet<Triple<Integer, Formula, T>> possibleReturns = new TreeSet<>(
			Comparator.comparing(Triple::getLeft));

	private Map<Integer, Formula[]> primitiveArgsForNextCall = new HashMap<>();
	private Map<Integer, Formula[][]> arrayArgsForNextCall = new HashMap<>();

	public void registerRecCall(Method m, SSAInvokeInstruction recCall, T returnValVars) {
		this.m = m;
		this.recCall = recCall;
		this.paramValNums = Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum());
		this.argValNums = new int[recCall.getNumberOfUses() - 1];
		for (int i = 1; i < recCall.getNumberOfUses(); i++) {
			argValNums[i - 1] = recCall.getUse(i);
		}
		this.returnValVars = returnValVars;
		this.recVars = collectRecVars();
	}

	/**
	 * computes formulas that represents the return value of the called method {@code m} @ callsite {@code callsite} by {@code caller}.
	 * <p>
	 * Result is computed by obtaining the used arguments from {@code caller} and substituting those values in for the parameter variables of {@code returnDeps}
	 *
	 * @param callSite invokeInstruction where the return value is used
	 * @param caller   Method that calls the function. Has to contain value dependencies for the arguments used in {@code callSite}
	 * @return formulas that represents the return value of the called method
	 */
	@Override public T getReturnValueForCallSite(SSAInvokeInstruction callSite, Method caller) {
		if (this.isRecursive()) {
			return this.getRecursiveReturnValueForCallSite(callSite, caller);
		} else {
			return getReturnValueNonRecursiveCallsite(callSite, caller);
		}
	}

	@Override public T getRecursiveReturnValueForCallSite(SSAInvokeInstruction instruction, Method caller) {

		// args w/ which the recursive function was called
		for (int j = 1; j < instruction.getNumberOfUses(); j++) {
			if (caller.isArrayType(instruction.getUse(j))) {
				arrayArgsForNextCall
						.put(paramValNums[j - 1], caller.getArray(instruction.getUse(j)).getValueDependencies());
			} else {
				primitiveArgsForNextCall.put(paramValNums[j - 1], caller.getDepsForValue(instruction.getUse(j)));
			}
		}

		T singleCall = substituteAll(this.getReturnValue(), primitiveArgsForNextCall, arrayArgsForNextCall);

		for (int i = 0; i < this.m.getProg().getEnv().config.recDepthMax(); i++) {
			computeNextArgs();
			T nextCall = substituteAll(this.getReturnValue(), primitiveArgsForNextCall, arrayArgsForNextCall);
			singleCall = substituteReturnValue(singleCall, nextCall, returnValVars);
		}

		computeNextArgs();
		T lastRun = substituteAll(this.getReturnValueNoRecursion(), primitiveArgsForNextCall, arrayArgsForNextCall);
		this.m.getProg().dlRestrictions.add(this.lastRunRestriction(lastRun, returnValVars));

		return singleCall;
	}

	public abstract Formula lastRunRestriction(T lastRun, T vars);

	/*
	We have the arguments used for the current call, saved in the maps
	{@code primitiveArgsForNextCall} and {@code arrayArgsForNextCall}

	The method computes the arguments used for the next recursive call, and updates the two maps accordingly
	 */
	private void computeNextArgs() {
		HashMap<Integer, Formula[]> newPrimArgs = new HashMap<>();
		HashMap<Integer, Formula[][]> newArrArgs = new HashMap<>();

		for (int i = 0; i < paramValNums.length; i++) {
			if (this.primitiveArgsForNextCall.keySet().contains(paramValNums[i])) {
				Formula[] arg = m.getDepsForValue(argValNums[i]);
				newPrimArgs.put(paramValNums[i], substituteAll(arg, primitiveArgsForNextCall, arrayArgsForNextCall));
			} else {
				Formula[][] arg = m.getArray(argValNums[i]).getValueDependencies();
				newArrArgs.put(paramValNums[i], substituteAll(arg, primitiveArgsForNextCall, arrayArgsForNextCall));
			}
		}
		primitiveArgsForNextCall = newPrimArgs;
		arrayArgsForNextCall = newArrArgs;
	}

	private Formula[] substituteAll(Formula[] f, Map<Integer, Formula[]> primArgs, Map<Integer, Formula[][]> arrArgs) {
		return LogicUtil.applySubstitution(f, createSubstitution(primArgs, arrArgs));
	}

	private Formula[][] substituteAll(Formula[][] f, Map<Integer, Formula[]> primArgs,
			Map<Integer, Formula[][]> arrArgs) {
		return LogicUtil.applySubstitution(f, createSubstitution(primArgs, arrArgs));
	}

	private Substitution createSubstitution(Map<Integer, Formula[]> primArgs, Map<Integer, Formula[][]> arrArgs) {
		Substitution s = new Substitution();
		for (int i : primArgs.keySet()) {
			s.addMapping(m.getVarsForValue(i), primArgs.get(i));
		}
		for (int i : arrArgs.keySet()) {
			s.addMapping(m.getArray(i).getArrayVars(), arrArgs.get(i));
		}
		return s;
	}

	public void addReturn(Triple<Integer, Formula, T> newReturn) {
		this.possibleReturns.add(newReturn);
	}

	@Override
	public T getReturnValue() {
		return computeReturnValue(this.possibleReturns);
	}

	@Override public T getReturnValueNoRecursion() {
		return computeReturnValue(this.possibleReturns.stream().filter(t -> !containsRecursionVar(t.getRight())).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Triple::getLeft)))));
	}

	private T computeReturnValue(TreeSet<Triple<Integer, Formula, T>> possibleReturns) {
		if (possibleReturns.size() == 1) {
			return possibleReturns.first().getRight();
		} else {
			T last = possibleReturns.last().getRight();
			Iterator<Triple<Integer, Formula, T>> iter = possibleReturns.descendingIterator();
			iter.next(); // skip last map entry --> already in use
			while (iter.hasNext()) {
				Triple<Integer, Formula, T> next = iter.next();
				last = this.getOperator().apply(next.getMiddle(), next.getRight(), last);
			}
			return last;
		}
	}

	@Override public boolean isRecursiveCall(SSAInvokeInstruction call) {
		return this.isRecursive() && this.recCall.equals(call);
	}

	@Override public boolean isRecursive() {
		return recCall != null;
	}

	@Override public Set<Variable> getRecVars() {
		return this.recVars;
	}

	@Override public T getReturnValVars() {
		return this.returnValVars;
	}

	protected abstract T substituteAll(T returnValueNoRecursion, Map<Integer, Formula[]> primitiveArgsForNextCall,
			Map<Integer, Formula[][]> arrayArgsForNextCall);

	protected abstract T substituteReturnValue(T containsRecCall, T recCallReturnValue, T vars);
}