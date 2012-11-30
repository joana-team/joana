/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

/**
 * Can be used to create .ev event files as in the Firm project. These files
 * can be read into an sql database and are thought to support evaluation.
 *
 * See http://pp.info.uni-karlsruhe.de/firm/Statistics for details.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class StatEv {

	public static final String EV_SUFFIX = ".ev";
	public static final String GZ_SUFFIX = ".gz";

	private final String prefix;
	private final boolean zipped;
	private PrintWriter out;
	private Stack<String> contexts;

	public StatEv(String prefix) {
		this(prefix, false);
	}

	public StatEv(String prefix, boolean zipped) {
		this.prefix = prefix;
		this.zipped = zipped;
	}

	public void init() throws IOException {
		if (out != null) {
			throw new IllegalStateException("Already initialized.");
		}

		OutputStream fOut;
		if (zipped) {
			fOut = new FileOutputStream(prefix + EV_SUFFIX + GZ_SUFFIX);
			fOut = new GZIPOutputStream(fOut);
		} else {
			fOut = new FileOutputStream(prefix + EV_SUFFIX);
		}

		out = new PrintWriter(fOut);
		contexts = new Stack<String>();
	}

	public void finalize() {
		if (out == null) {
			throw new IllegalStateException("Not initialized.");
		}

		out.close();
		out = null;
	}

	/**
	 * Enter a context
	 * @param context
	 * @param value
	 */
	public void enter(String context, String value) {
		if (out == null) {
			throw new IllegalStateException("Not initialized.");
		}

		contexts.push(context);
		print('P', context, value);
	}

	/**
	 * Leave a context
	 * @param context
	 */
	public void leave(String context) {
		if (out == null) {
			throw new IllegalStateException("Not initialized.");
		} else if (contexts.size() < 1) {
			throw new IllegalStateException("We are not in any context");
		} else if (!context.equals(contexts.peek())) {
			throw new IllegalStateException("We are not in context '" + context
					+ "' but in '" + contexts.peek() + "'");
		}

		contexts.pop();
		print('O', context);
	}

	/**
	 * Log an event
	 * @param event
	 * @param value
	 */
	public void event(String event, String value) {
		if (out == null) {
			throw new IllegalStateException("Not initialized.");
		}

		print('E', event, value);
	}

	private void print(char ev, String key) {
		print(ev, key, null);
	}

	private void print(char ev, String key, String value) {
		out.println(ev + ";" + key + ";" + (value == null ? "" : value));
	}
}
