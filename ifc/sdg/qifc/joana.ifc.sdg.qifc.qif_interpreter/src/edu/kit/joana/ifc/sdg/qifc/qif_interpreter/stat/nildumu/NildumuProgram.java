package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import nildumu.Context;
import nildumu.Parser;
import nildumu.Processor;

/**
 * wrapper for Nildumu representation of program (or program parts respectively) + additional information about inputs 7 outputs we might have from previous analyses on the program
 */
public class NildumuProgram {

	public Context context;
	// public Parser.ProgramNode ast;
	public NildumuOptions options;
	public Parser.ProgramNode ast;

	public NildumuProgram(Parser.ProgramNode p, NildumuOptions options) {
		this.options = options;
		this.context = Processor.process(p.toPrettyString());
		this.ast = p;
	}
}