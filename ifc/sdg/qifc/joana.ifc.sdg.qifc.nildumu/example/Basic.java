//package edu.kit.nildumu.prog;

import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.EntryPoint;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI.*;

/**
 * Just the most rudimentary program:
 * <code>
 * h input int h  = 0bu;
 * l output int o = h;
 * </code>
 */
public class Basic {
	
	public static void main(String[] args) {
		program(1);
	}
	
	@EntryPoint
	public static void program(@Source(level=Level.HIGH) int h) {
		output(h, "l");
	}
}
