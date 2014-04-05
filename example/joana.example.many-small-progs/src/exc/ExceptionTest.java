/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package exc;

import sensitivity.Security;

public class ExceptionTest {

	public static void main(String[] args) throws Exception {
		try {
			exc(args[Security.SECRET]);
		} catch (MyException me) {
			int len = me.mess.length();
			Security.leak(len);
		}
	}

	public static void exc(String str) throws Exception {
		throw new MyException(str);
	}
}

class MyException extends Exception {
	String mess;
	public MyException(String msg) {
		super(msg);
		mess = msg;
	}
}
