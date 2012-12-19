/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.pw;

import static edu.kit.joana.api.annotations.Annotations.*;

class MySystem {
	public static PublicOutput out1 = new PublicOutput();
	public static PublicOutput out2 = new PublicOutput();
}

class PublicOutput {
	byte[] buffer = new byte[1024];
	private int p = 0;

	public void print(boolean b) {
		if (b) {
			buffer[(p + 1) % 1024] = 1;
		} else {
			buffer[(p + 1) % 1024] = 0;
		}
	}
}

public class PasswordFile {
	private String[] names = { "A", "B" };
	private String[] passwords = { SECRET_STRING, SECRET_STRING };


	public String getPassword(int i) {
		return toggle(passwords[i]);
	}

	public boolean check(String user, String password) {
		boolean match = false;

		try {
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(user) && getPassword(i).equals(password)) {
					match = true;
					break;
				}
			}

		} catch (Throwable t) {
		}
		;

		return match;
	}

	public static void main(String[] args) {
		MySystem.out1.print(new PasswordFile().check(args[0], args[1]));
		leak(MySystem.out1.buffer[0]);
		
		leak(new PasswordFile().check(args[0], args[1]));
	}
}
