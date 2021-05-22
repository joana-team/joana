package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;

public class ConversionException extends Exception {

	public ConversionException(SSAInstruction i) {
		this(String.format("Error: An error occured while trying to convert the following instruction to Nildumu AST representation: %s",
				i.toString()));
	}

	public ConversionException(String msg) {
		super(msg);
	}

	public ConversionException(IBinaryOpInstruction.IOperator op) {
		this(String.format("Error: Couldn't convert operator %s to nildumu representation", op.toString()));
	}

	public ConversionException(Type t) {
		this(String.format("Error: Couldn't convert the following type to Nildumu representation: %s", t.toString()));
	}

	public ConversionException(IUnaryOpInstruction.Operator op) {
		this(String.format("Error: Couldn't convert operator %s to nildumu representation", op.toString()));
	}

	public ConversionException(IComparisonInstruction.Operator operator) {
		this(String.format("Error: Couldn't convert operator %s to nildumu representation", operator.toString()));
	}

	public ConversionException(IConditionalBranchInstruction.Operator operator) {
		this(String.format("Error: Couldn't convert operator %s to nildumu representation", operator.toString()));
	}
}