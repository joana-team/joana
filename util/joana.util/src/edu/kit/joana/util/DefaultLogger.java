/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.io.PrintStream;

/**
 * 
 * @author Juergen Graf
 */
public class DefaultLogger implements Logger {

	private final PrintStream out;
	private final boolean enabled;
	
	public DefaultLogger(final PrintStream out) {
		this(out, true);
	}
	
	public DefaultLogger(final PrintStream out, final boolean enabled) {
		this.out = out;
		this.enabled = enabled;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#out(java.lang.Object)
	 */
	@Override
	public void out(Object obj) {
		if (obj == null) {
			out("null");
		} else {
			out(obj.toString());
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#outln(java.lang.Object)
	 */
	@Override
	public void outln(Object obj) {
		if (obj == null) {
			outln("null");
		} else {
			outln(obj.toString());
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#outln(java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void outln(Object obj, Throwable t) {
		if (obj == null) {
			outln("null", t);
		} else {
			outln(obj.toString(), t);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#out(java.lang.String)
	 */
	@Override
	public void out(String str) {
		if (enabled) {
			out.print(str);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#outln(java.lang.String)
	 */
	@Override
	public void outln(String str) {
		if (enabled) {
			out.println(str);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.Logger#outln(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void outln(String str, Throwable t) {
		if (enabled) {
			out.println(str);
			if (t != null) {
				t.printStackTrace(out);
			}
		}
	}

}
