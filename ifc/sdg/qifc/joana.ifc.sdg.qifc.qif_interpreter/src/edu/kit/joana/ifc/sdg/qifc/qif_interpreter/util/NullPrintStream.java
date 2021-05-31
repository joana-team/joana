package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class NullPrintStream extends PrintStream {

	public NullPrintStream() {
		super(new NullOutputStream());
	}

	public static class NullOutputStream extends OutputStream {

		@Override public void write(int b) throws IOException {
			// do nothing
		}
	}
}