/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Utility class providing factory methods for e.g. InputStreamReaders or PrintStreams conforming to UTF-8 encoding.
 * @author Martin Mohr
 */
public final class IOFactory {
	
	private IOFactory() {}
	
	/**
	 * Returns an input stream reader which reads from the given input stream and uses UTF-8 encoding.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param in input stream to read from
	 * @return an input stream reader which reads data from the given input stream and uses the UTF-8 encoding
	 */
	public static InputStreamReader createUTF8ISReader(InputStream in) {
		try {
			InputStreamReader ret = new InputStreamReader(in, "UTF-8");
			return ret;
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("UTF-8 is required to be supported by any java virtual machine, but is not by this one!");
		}
	}
	
	/**
	 * Returns print stream which writes to the given output stream and uses UTF-8 encoding.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param out output stream to write to
	 * @param autoFlush whether the returned print stream shall flush automatically
	 * @return a print stream which writes to the given output stream and uses the UTF-8 encoding
	 */
	public static PrintStream createUTF8PrintStream(OutputStream out, boolean autoFlush) {
		try {
			return new PrintStream(out, autoFlush, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("UTF-8 is required to be supported by any java virtual machine, but is not by this one!");
		}
	}
	
	/**
	 * Returns print stream which writes to the given output stream and uses UTF-8 encoding. The returned print stream will not flush automatically.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param out output stream to write to
	 * @return a print stream which writes to the given output stream and uses the UTF-8 encoding
	 */
	public static PrintStream createUTF8PrintStream(OutputStream out) {
		try {
			return new PrintStream(out, false, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("UTF-8 is required to be supported by any java virtual machine, but is not by this one!");
		}
	}
	
	
}
