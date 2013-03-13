/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.io;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.swing.JTextArea;

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
			handleUEE();
			return null;
		}
	}
	
	/**
	 * Returns an output stream writer which writes to the given output stream and uses UTF-8 encoding.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param out output stream to write to
	 * @return an output stream writer which writes to the given output stream and uses UTF-8 encoding
	 */
	public static OutputStreamWriter createUTF8OutputStreamWriter(OutputStream out) {
		try {
			return new OutputStreamWriter(out, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			handleUEE();
			return null;
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
			handleUEE();
			return null;
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
			handleUEE();
			return null;
		}
	}
	
	/**
	 * Constructs a UTF-8 encoded string from the given byte array. The given offset and length are assumed to be in bounds of the given array.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param buf buffer to convert to string
	 * @param off starting position
	 * @param len length
	 * @return UTF-8 encoded string from the given byte array
	 */
	public static String createUTF8String(byte[] buf, int off, int len) {
		try {
			return new String(buf, off, len, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			handleUEE();
			return null;
		}
	}
	
	/**
	 * Returns the UTF-8 encoded bytes from the given string.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param s string to convert into UTF-8 encoded bytes.
	 * @return UTF-8 encoded bytes forming the given string
	 */
	public static byte[] createUTF8Bytes(String s) {
		try {
		return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			handleUEE();
			return null;
		}
	}
	
	
	/**
	 * Returns a print stream, which prints everything to the given text area, and uses UTF-8 encoding.
	 * Throws a runtime exception, if UTF-8 is not supported by the JVM (according to 
	 * <a href="http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">the java api specs</a>this should never be the case...)
	 * @param area text area which the new print stream shall print everything to
	 * @return print stream, which prints everything to the given text area
	 */
	public static PrintStream createPrintStreamFromJTextArea(final JTextArea area) {
		try {
		return new PrintStream(new ByteArrayOutputStream(), false, "UTF-8") {

			public void write(byte[] buf, int off, int len) {
				area.append(IOFactory.createUTF8String(buf, off, len));
				area.setCaretPosition(area.getText().length());
			}
		};
		} catch (UnsupportedEncodingException uee) {
			handleUEE();
			return null;
		}
    }
	
	
	/**
	 * Handler for UnsupportedEncodingExceptions. Basically, this method was introduced to have the same behavior
	 * for all methods in which the exception can occur. Just throws a runtime exception .
	 * @throws RuntimeException 
	 */
	private static void handleUEE() throws RuntimeException {
		throw new RuntimeException("UTF-8 is required to be supported by any java virtual machine, but is not by this one!");
	}
	
}
