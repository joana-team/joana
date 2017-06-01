/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IR.SSA2LocalMap;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.wala.core.PDGNode.Kind;
import edu.kit.joana.wala.util.PrettyWalaNames;

public final class PDGNodeCreationVisitor implements IVisitor {

	private static final int MAX_CONST_STR = 25;

	private final PDG pdg;
	private final IClassHierarchy cha;
	private final IR ir;
	private final ParameterFieldFactory params;

	private final SymbolTable sym;

	private final boolean ignoreStaticFields;

	public PDGNode lastNode = null;

    private boolean showTypeNameInValue = false;
    private final boolean associateLocalNames;
    private TypeInference typeInf = null;       // Only needed for `showTypeNameInValue = true`
    
    private final Map<Integer, Set<String>> parameterAliases;
    private final Map<Integer, String> parameterToName;
    private final Map<String, Integer> nameToParameter;
    private final Map<Integer, Integer> varNumberToParameter;
    
    private final Map<Integer, Set<String>> valueNumberNames;

	public PDGNodeCreationVisitor(PDG pdg, IClassHierarchy cha, ParameterFieldFactory params,
			SymbolTable sym, boolean ignoreStaticFields, IR ir, boolean associateLocalNames) {
		this.pdg = pdg;
		this.params = params;
		this.cha = cha;
		this.ir = ir;
		this.sym = sym;
		this.ignoreStaticFields = ignoreStaticFields;
		this.associateLocalNames = associateLocalNames;
		this.parameterAliases = new HashMap<>();
		this.parameterToName = new HashMap<>();
		this.nameToParameter = new HashMap<>();
		
		this.varNumberToParameter = new HashMap<>();
		
		for (int p = 0; p < pdg.getMethod().getNumberOfParameters(); p++) {
			final String name = pdg.getMethod().getLocalVariableName(0, p);
			this.parameterToName.put(p, name);
			this.nameToParameter.put(name, p);
			this.varNumberToParameter.put(ir.getParameter(p), p);
		}
		
		if (this.associateLocalNames) {
			final SSA2LocalMap localMap = ir.getLocalMap();
			if (localMap != null) {
				this.valueNumberNames = localMap.getLocalNames();
			} else {
				this.valueNumberNames = null;
			}
			
		} else {
			this.valueNumberNames = null;
		}
	}

    public static PDGNodeCreationVisitor makeWithTypeInf(PDG pdg, IClassHierarchy cha, ParameterFieldFactory params,
            SymbolTable sym, boolean ignoreStaticFields, boolean showTypeNameInValue, TypeInference typeInf, IR ir, boolean associateLocalNames) {
        PDGNodeCreationVisitor visitor = new PDGNodeCreationVisitor(pdg, cha, params, sym, ignoreStaticFields, ir, associateLocalNames);
        visitor.showTypeNameInValue = showTypeNameInValue;
        visitor.typeInf = typeInf;
        return visitor;
    }

	private String tmpName(int var) {
        String type = "";

		if (var < 0) {
			return "v?(" + var + ")";
		} else if (sym.isConstant(var)) {
			String cst = null;

			if (sym.isBooleanConstant(var)) {
				cst = (sym.isTrue(var) ? "true" : "false");
			} else if (sym.isDoubleConstant(var)) {
				cst = sym.getDoubleValue(var) + " d";
			} else if (sym.isFloatConstant(var)) {
				cst = sym.getFloatValue(var) + " f";
			} else if (sym.isIntegerConstant(var)) {
				cst = sym.getIntValue(var) + "";
			} else if (sym.isLongConstant(var)) {
				cst = sym.getLongValue(var) + " l";
			} else if (sym.isNullConstant(var)) {
				cst = "null";
			} else if (sym.isStringConstant(var)) {
				cst = sym.getStringValue(var).replace('"', '\'');
			} else {
				Object obj = sym.getConstantValue(var);
				cst = (obj == null ? "?" : obj.toString());
			}

			//sym.getConstantValue(var).toString();
			if (cst.length() > MAX_CONST_STR) {
				cst = cst.substring(0, MAX_CONST_STR - 4) + "...";
			}

			return "#(" + cst + ")";
		} else {
            if (this.varNumberToParameter.containsKey(var)) {
                if (this.showTypeNameInValue) {
                    // Append the type name to the variables name
                    assert (this.typeInf != null) : "You have to give a TypeInference in order to use showTypeNameInValue";
                    if (var > 0) {
                        if (this.typeInf.getType(var) != null) {
                            if (this.typeInf.getType(var).getTypeReference() != null) {
                                type = " <" + this.typeInf.getType(var).getTypeReference().getName().toString() + ">";
                            } else type = " <unknown>";
                        } else {
                            type = " <NoneType>";
                        }
                    }
                }
                
                final int p = this.varNumberToParameter.get(var);
                String name = " $" + this.parameterToName.get(p) + " ";
                if (pdg.getMethod().isStatic()) {
                	return "p" + (p + 1) + name + type;
                } else {
                	return "p" + (p    ) + name + type;
                }
            } else {
                return "v" + var + type;
            }
        }
	}

	private void recordParameterAliases(String[] localUseNames) {
		if (localUseNames == null) return;
		Arrays.stream(localUseNames).filter(this.nameToParameter::containsKey).forEach( pName -> {
			final Integer p = this.nameToParameter.get(pName);
			this.parameterAliases.computeIfAbsent(p, (pp) -> new HashSet<String>());
			this.parameterAliases.computeIfPresent(p, (pp, aliases) -> {
				for (String alias : localUseNames) aliases.add(alias);
				return aliases;
			});
		});
	}
	
	private String[] allLocalNames(int def) {
		if (this.valueNumberNames == null) return null;
		final Set<String> allLocalNames = this.valueNumberNames.get(def);
		final String[] result;
		if (allLocalNames != null) {
			result = allLocalNames.toArray(new String[allLocalNames.size()]);
		} else {
			result = null;
		}
		
		assert superset(result, allLocalNamesSlow(def));
		return result;
	}
	
	private static boolean superset(String[] a, String[] b) {
		if ( ( a == null || a.length == 0) && (b == null || b.length == 0)) return true;
		if ( a == null ) return false;
		if ( b == null ) return true;

		final Set<String> setA = new HashSet<>(a.length);
		final Set<String> setB = new HashSet<>(a.length);
		Arrays.stream(a).forEach( n -> setA.add(n));
		Arrays.stream(b).forEach( n -> setB.add(n));
		
		return setA.containsAll(setB);
	}
	
	private String[] allLocalNamesSlow(int def) {
		Set<String> allLocalNames = new HashSet<>();
		final SSAInstruction[] insts = ir.getInstructions();
		for (int k = 0; k < insts.length; k++) {
			String[] localNames = ir.getLocalNames(k, def);
			if (localNames != null) {
				for (String local : localNames) {
					if (local != null ) {
						allLocalNames.add(local);
					}
				}
			}
		}
		return allLocalNames.toArray(new String[allLocalNames.size()]);
	}
	
	@Override
	public void visitGoto(SSAGotoInstruction instruction) {
		lastNode = pdg.createNode("goto", Kind.NORMAL, PDGNode.DEFAULT_TYPE, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
	}

	@Override
	public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		String label = tmpName(instruction.getDef()) + " = " + tmpName(instruction.getArrayRef())
			+ "[" + tmpName(instruction.getIndex()) + "]";

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instruction.getDef());
			
			String[] localUseNames1, localUseNames2;
			localUseNames1 = ir.getLocalNames(instruction.iindex,  instruction.getArrayRef());
			localUseNames2 = ir.getLocalNames(instruction.iindex,  instruction.getIndex());
			if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
			if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
			
			recordParameterAliases(localUseNames1);
			recordParameterAliases(localUseNames2);

			localUseNames = new String[localUseNames1.length + localUseNames2.length];
			System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
			System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.HREAD, instruction.getElementType(), localDefNames, localUseNames);

		ParameterField field = params.getArrayField(instruction.getElementType());
		pdg.addFieldRead(field, lastNode);
	}

	@Override
	public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		String label = tmpName(instruction.getArrayRef()) + "[" + tmpName(instruction.getIndex())
			+ "] = " + tmpName(instruction.getValue());
		
		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instruction.getArrayRef());
			
			String[] localUseNames1, localUseNames2;
			localUseNames1 = ir.getLocalNames(instruction.iindex,  instruction.getIndex());
			localUseNames2 = ir.getLocalNames(instruction.iindex,  instruction.getValue());
			if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
			if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
			
			recordParameterAliases(localUseNames1);
			recordParameterAliases(localUseNames2);
			
			localUseNames = new String[localUseNames1.length + localUseNames2.length];
			System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
			System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.HWRITE, instruction.getElementType(), localDefNames, localUseNames);

		ParameterField field = params.getArrayField(instruction.getElementType());
		pdg.addFieldWrite(field, lastNode);
	}

	@Override
	public void visitBinaryOp(SSABinaryOpInstruction instr) {
		assert instr.getNumberOfUses() == 2;

		String label = tmpName(instr.getDef()) + " = " + tmpName(instr.getUse(0)) + " " +  PrettyWalaNames.op2str(instr.getOperator()) + " "
			+ tmpName(instr.getUse(1));
		// The description of IR.getLocalNames() suggests that it is enough to call 
		// ir.getLocalNames(instr.iindex, instr.getDef()).
		// In fact, though, in Bytecode after a binary op, the result has not been written to any variable yet.
		//
		// Example: for an Java Source Code line such as: "x = y + z", translated to: "v1 = binop(v2,v3, add)", 
		// after thi binop v1 is NOT YET associacted with with x.
		//
		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			
			String[] localUseNames1, localUseNames2;
			localUseNames1 = ir.getLocalNames(instr.iindex,  instr.getUse(0));
			localUseNames2 = ir.getLocalNames(instr.iindex,  instr.getUse(1));
			if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
			if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
			
			recordParameterAliases(localUseNames1);
			recordParameterAliases(localUseNames2);
			
			localUseNames = new String[localUseNames1.length + localUseNames2.length];
			System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
			System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, PDGNode.DEFAULT_TYPE, localDefNames, localUseNames);
	}

	@Override
	public void visitUnaryOp(SSAUnaryOpInstruction instr) {
		String label = tmpName(instr.getDef()) + " = " + PrettyWalaNames.op2str(instr.getOpcode()) + "("
			+ tmpName(instr.getUse(0)) + ")";

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			localUseNames = ir.getLocalNames(instr.iindex, instr.getUse(0));
			
			recordParameterAliases(localUseNames);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, PDGNode.DEFAULT_TYPE, localDefNames, localUseNames);
	}

	@Override
	public void visitConversion(SSAConversionInstruction instr) {
		String label = tmpName(instr.getDef()) + " = CONVERT " + instr.getFromType().getName() + " to "
			+ instr.getToType().getName() + " " + tmpName(instr.getUse(0));

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			localUseNames = ir.getLocalNames(instr.iindex, instr.getUse(0));
			
			recordParameterAliases(localUseNames);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, instr.getToType(), localDefNames, localUseNames);
	}

	@Override
	public void visitComparison(SSAComparisonInstruction instr) {
		assert instr.getNumberOfUses() == 2;

		final String label = tmpName(instr.getDef()) + " = " + tmpName(instr.getUse(0)) + " "
			+ PrettyWalaNames.op2str(instr.getOperator()) + " " + tmpName(instr.getUse(1));

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			
			String[] localUseNames1, localUseNames2;
			localUseNames1 = ir.getLocalNames(instr.iindex,  instr.getUse(0));
			localUseNames2 = ir.getLocalNames(instr.iindex,  instr.getUse(1));
			if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
			if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
			
			recordParameterAliases(localUseNames1);
			recordParameterAliases(localUseNames2);
			
			localUseNames = new String[localUseNames1.length + localUseNames2.length];

			System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
			System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, TypeReference.Boolean, localDefNames, localUseNames);
	}

	@Override
	public void visitConditionalBranch(SSAConditionalBranchInstruction instr) {
		String label = "if (" + tmpName(instr.getUse(0)) + " " + PrettyWalaNames.op2str(instr.getOperator())
			+ " " + tmpName(instr.getUse(1)) + ")";
		final String[] localUseNames;
		if (this.associateLocalNames) {
			String[] localUseNames1, localUseNames2;
			localUseNames1 = ir.getLocalNames(instr.iindex,  instr.getUse(0));
			localUseNames2 = ir.getLocalNames(instr.iindex,  instr.getUse(1));
			if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
			if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
			
			recordParameterAliases(localUseNames1);
			recordParameterAliases(localUseNames2);
			
			localUseNames = new String[localUseNames1.length + localUseNames2.length];
			System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
			System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.PREDICATE, TypeReference.Boolean, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
	}

	@Override
	public void visitSwitch(SSASwitchInstruction instruction) {
		String label = "switch " + tmpName(instruction.getUse(0));
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localUseNames = ir.getLocalNames(instruction.iindex,  instruction.getUse(0));
			
			recordParameterAliases(localUseNames);

		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.PREDICATE, PDGNode.DEFAULT_NO_TYPE, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
	}

	@Override
	public void visitReturn(SSAReturnInstruction instr) {
		String label = "return";

		final String[] localUseNames;
		if (!instr.returnsVoid()) {
			label += " " + tmpName(instr.getResult());
			
			if (this.associateLocalNames) {
				localUseNames = ir.getLocalNames(instr.iindex,  instr.getResult());
				
				recordParameterAliases(localUseNames);
				
			} else {
				localUseNames = PDGNode.DEFAULT_NO_LOCAL;
			}
		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}

		lastNode = pdg.createNode(label, Kind.NORMAL, pdg.exit.getTypeRef(), PDGNode.DEFAULT_NO_LOCAL, localUseNames);

		pdg.addReturn(lastNode);
	}

	@Override
	public void visitGet(SSAGetInstruction instr) {
		final int dest = instr.getDef();
		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			
			if (instr.isStatic()) {
				localUseNames = PDGNode.DEFAULT_NO_LOCAL;
			} else {
				localUseNames = ir.getLocalNames(instr.iindex, instr.getRef());
				
				recordParameterAliases(localUseNames);
				
			}
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		
		final FieldReference fRef = instr.getDeclaredField();
		final IField ifield = cha.resolveField(fRef);
		final TypeReference type = instr.getDeclaredFieldType();

		if (ifield != null && !(ignoreStaticFields && instr.isStatic())) {
			ParameterField field = params.getObjectField(ifield);

			
			String label = tmpName(dest) + " = ";
			if (instr.isStatic()) {
				label += PrettyWalaNames.simpleTypeName(fRef.getDeclaringClass()) + "." + fRef.getName();
			} else {
				label += tmpName(instr.getRef()) + "." + fRef.getName();
			}

			lastNode = pdg.createNode(label, Kind.HREAD, type, localDefNames, localUseNames);

			pdg.addFieldRead(field, lastNode);
		} else {
			/**
			 * As wala ignores some classes during the analysis
			 * (see jSDGExclusions.txt) there may be fields that cannot be
			 * resolved. So we have to ignore them in our analysis.
			 */
			String label = tmpName(dest) + " = " + (instr.isStatic() ? "" : tmpName(instr.getRef()) + ".")
				+ fRef.getName();

			lastNode = pdg.createNode(label, Kind.EXPRESSION, type, localDefNames, localUseNames);
		}
	}

	@Override
	public void visitPut(SSAPutInstruction instr) {
		final int val = instr.getVal();

		final FieldReference fRef = instr.getDeclaredField();
		final IField ifield = cha.resolveField(fRef);
		final TypeReference type = instr.getDeclaredFieldType();

		final String[] localUseNames;
		if (this.associateLocalNames) {
			if (instr.isStatic()) {
				localUseNames  = ir.getLocalNames(instr.iindex, instr.getVal());
			} else {
				String[] localUseNames1, localUseNames2;
				localUseNames1 = ir.getLocalNames(instr.iindex, instr.getVal());
				localUseNames2 = ir.getLocalNames(instr.iindex, instr.getRef());
				if (localUseNames1 == null) localUseNames1 = PDGNode.DEFAULT_EMPTY_LOCAL;
				if (localUseNames2 == null) localUseNames2 = PDGNode.DEFAULT_EMPTY_LOCAL;
				
				recordParameterAliases(localUseNames1);
				recordParameterAliases(localUseNames2);

				localUseNames = new String[localUseNames1.length + localUseNames2.length];
				System.arraycopy(localUseNames1, 0, localUseNames, 0,                     localUseNames1.length);
				System.arraycopy(localUseNames2, 0, localUseNames, localUseNames1.length, localUseNames2.length);
			}
		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		
		if (ifield != null && !(ignoreStaticFields && instr.isStatic())) {
			ParameterField field = params.getObjectField(ifield);

			String label = null ;
			if (instr.isStatic()) {
				label = PrettyWalaNames.simpleTypeName(fRef.getDeclaringClass()) + "." + fRef.getName();
			} else {
				label = tmpName(instr.getRef()) + "." + fRef.getName();
			}

			label += " = " + tmpName(val);
			// SSAPutInstruction does not define a value, so we do not record
			// the names of the corresponding (val's) local variables.
			// TODO: maybe change this?
			lastNode = pdg.createNode(label, Kind.HWRITE, type, PDGNode.DEFAULT_NO_LOCAL, localUseNames);

			pdg.addFieldWrite(field, lastNode);
		} else {
			/**
			 * As wala ignores some classes during the analysis
			 * (see jSDGExclusions.txt) there may be fields that cannot be
			 * resolved. So we have to ignore them in our analysis.
			 */
			String label = (instr.isStatic() ? "" : tmpName(instr.getRef()) + ".") + fRef.getName()
				+ " = " + tmpName(val);
			
			// SSAPutInstruction does not define a value, so we do not record
			// the names of the corresponding (val's) local variables.
			// TODO: maybe change this?
			lastNode = pdg.createNode(label, Kind.EXPRESSION, type, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
		}
	}

	@Override
	public void visitInvoke(SSAInvokeInstruction instr) {
		final MethodReference tgt = instr.getDeclaredTarget();

		String label = (instr.isStatic() ? "" : tmpName(instr.getReceiver()) + "." )
			+ tgt.getSelector().getName().toString() + "()";

		if (instr.hasDef()) {
			label = tmpName(instr.getDef()) + " = " + label;
		}

		
		
		final TypeReference type = instr.getDeclaredResultType();
		lastNode = pdg.createNode(label, Kind.CALL, type, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
		lastNode.setUnresolvedCallTarget(tgt.getSignature().toString());
		final PDGNode[] in = new PDGNode[instr.getNumberOfParameters()];

		final StringBuffer extLabel = new StringBuffer(label.substring(0, label.length() - 1));

		for (int i = 0; i < instr.getNumberOfParameters(); i++) {
			final int val = instr.getUse(i);
			final String valLabel = tmpName(val);

			if (instr.isStatic() || i > 0) {
				// do not append this pointer to normal param list
				extLabel.append(valLabel);
				if (i + 1 < instr.getNumberOfParameters()) {
					extLabel.append(", ");
				}
			}

			final String[] localUseNames;
			if (this.associateLocalNames) {
				localUseNames = ir.getLocalNames(instr.iindex, val);
				
				recordParameterAliases(localUseNames);

			} else {
				localUseNames = PDGNode.DEFAULT_NO_LOCAL;
			}

			if (instr.isStatic()) {
				final TypeReference pType = instr.getDeclaredTarget().getParameterType(i);
				// parameter index 0 is reserved for the this pointer. static method params start at 1.
				
				PDGNode actIn = pdg.createNode("param " + (i + 1) + " [" + valLabel + "]", PDGNode.Kind.ACTUAL_IN, pType, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
				in[i] = actIn;
			} else {
				final TypeReference tref;
				if (i == 0) {
					tref = instr.getDeclaredTarget().getDeclaringClass();
				} else {
					tref = instr.getDeclaredTarget().getParameterType(i - 1);
				}

				// parameter index 0 is reserved for the this pointer. static method params start at 1.
				PDGNode actIn = pdg.createNode((i == 0 ? "this" : "param " + i) + " [" + valLabel + "]", PDGNode.Kind.ACTUAL_IN, tref, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
				in[i] = actIn;
			}
		}

		extLabel.append(')');
		lastNode.setLabel(extLabel.toString());

		PDGNode retOut = null;

		if (instr.getNumberOfReturnValues() == 1) {
			String[] localDefNames;
			if (this.associateLocalNames) {
				localDefNames = allLocalNames(instr.getDef());
			} else {
				localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			}
			
			retOut = pdg.createNode("ret 0", PDGNode.Kind.ACTUAL_OUT, type, localDefNames, PDGNode.DEFAULT_NO_LOCAL);
		} else if (instr.getNumberOfReturnValues() > 1) {
			throw new IllegalStateException("Currently not supported - method has more then a single return value: " + tgt);
		}

		// TODO: local Names for handled exception?!?!
		final PDGNode excOut = pdg.createNode("ret _exception_", PDGNode.Kind.ACTUAL_OUT, TypeReference.JavaLangException, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
		final PDGCallReturn out = new PDGCallReturn(retOut, excOut);

		pdg.addCall(lastNode, in, out);
	}

	@Override
	public void visitNew(SSANewInstruction instr) {
		final TypeReference tref = instr.getConcreteType();
		final String label = tmpName(instr.getDef()) + " = new " + PrettyWalaNames.simpleTypeName(tref);

		final String[] localDefNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.NEW, tref, localDefNames, PDGNode.DEFAULT_NO_LOCAL);
	}

	@Override
	public void visitArrayLength(SSAArrayLengthInstruction instr) {
		final String label = tmpName(instr.getDef()) + " = " + tmpName(instr.getArrayRef()) + ".length";

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			
			localUseNames = ir.getLocalNames(instr.iindex, instr.getArrayRef());
			
			recordParameterAliases(localUseNames);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, TypeReference.Int, localDefNames, localUseNames);
	}

	@Override
	public void visitThrow(SSAThrowInstruction instruction) {
		final String label = "throw " + tmpName(instruction.getException());
		
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localUseNames = ir.getLocalNames(instruction.iindex, instruction.getUse(0));
			recordParameterAliases(localUseNames);

		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.NORMAL, TypeReference.JavaLangException, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
	}

	@Override
	public void visitMonitor(SSAMonitorInstruction instr) {
		int ref = instr.getRef();
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localUseNames = ir.getLocalNames(instr.iindex, instr.getRef());

			recordParameterAliases(localUseNames);
		} else {
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		String label = (instr.isMonitorEnter() ? "MONITORENTER " : "MONITOREXIT ") + tmpName(ref);

		lastNode = pdg.createNode(label, Kind.SYNCHRONIZATION, PDGNode.DEFAULT_TYPE, PDGNode.DEFAULT_NO_LOCAL, localUseNames);
	}

	@Override
	public void visitCheckCast(SSACheckCastInstruction instr) {
		String label = tmpName(instr.getDef()) + " = CHECKCAST " + tmpName(instr.getVal());

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			localUseNames = ir.getLocalNames(instr.iindex, instr.getVal());
			
			recordParameterAliases(localUseNames);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, PDGNode.DEFAULT_TYPE, localDefNames, localUseNames);
	}

	@Override
	public void visitInstanceof(SSAInstanceofInstruction instr) {
		String label = tmpName(instr.getDef()) + "=" + tmpName(instr.getRef()) + " INSTANCEOF "
			+ PrettyWalaNames.simpleTypeName(instr.getCheckedType());

		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
			localUseNames = ir.getLocalNames(instr.iindex, instr.getRef());
			
			recordParameterAliases(localUseNames);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, TypeReference.Boolean, localDefNames, localUseNames);
	}

	@Override
	public void visitPhi(SSAPhiInstruction instr) {
		StringBuffer sb = new StringBuffer();
		sb.append("PHI " + tmpName(instr.getDef()) + " = ");

		
		int[] uses = new int[instr.getNumberOfUses()];
		for (int i = 0; i < uses.length; i++) {
			uses[i] = instr.getUse(i);
			sb.append(tmpName(uses[i]));
			if (i < uses.length - 1) {
				sb.append(", ");
			}
		}
		
		final String[] localDefNames;
		final String[] localUseNames;
		if (this.associateLocalNames) {
			final int firsInstuctionOfBB  = ir.getBasicBlockForInstruction(instr).getFirstInstructionIndex();
			localDefNames = allLocalNames(instr.getDef());
			
			ArrayList<String> localUseNamesMerged = new ArrayList<>(instr.getNumberOfUses() * 2);
			for (int i = 0; i < instr.getNumberOfUses(); i++) {
				String[] localUseName = ir.getLocalNames(firsInstuctionOfBB, instr.getUse(i));
				recordParameterAliases(localUseName);

				if (localUseName != null) {
					Collections.addAll(localUseNamesMerged, localUseName);
				}
			}
			localUseNames = localUseNamesMerged.toArray(new String[localUseNamesMerged.size()]);
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
			localUseNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(sb.toString(), Kind.PHI, PDGNode.DEFAULT_NO_TYPE, localDefNames, localUseNames);
	}

	@Override
	public void visitPi(SSAPiInstruction instruction) {
		throw new UnsupportedOperationException("We do not treat pi-instructions.");
	}

	@Override
	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instr) {
		String label = tmpName(instr.getDef()) + " = catch <exc>";

		final String[] localDefNames;
		if (this.associateLocalNames) {
			localDefNames = allLocalNames(instr.getDef());
		} else {
			localDefNames = PDGNode.DEFAULT_NO_LOCAL;
		}
		lastNode = pdg.createNode(label, Kind.EXPRESSION, TypeReference.JavaLangException, localDefNames, PDGNode.DEFAULT_NO_LOCAL);
	}

	@Override
	public void visitLoadMetadata(SSALoadMetadataInstruction instr) {
		String label = tmpName(instr.getDef()) + " = metadata " + instr.getToken() + "->" + instr.getType();

		lastNode = pdg.createNode(label, Kind.EXPRESSION, instr.getType(), PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
	}

	public Map<Integer, Set<String>> getParameterAliases() {
		return parameterAliases;
	}

}
