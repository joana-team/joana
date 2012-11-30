/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package module.provider1;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import module.Module;
import module.Module.A;

public class ModuleP1 implements Module {

	public int encrypt(int msg, int secret) {
		return msg + secret;
	}

	public Message encrypt(Message msg, int secret) {
		final StringBuffer newmsg = new StringBuffer();
		final NumberFormat df = DecimalFormat.getInstance();
		final int len = msg.data.length();

		for (int i = 0; i < len; i++) {
			final char txt = (char) (msg.data.charAt(i) + secret);
			final String num = df.format((byte) txt);
			if (!num.isEmpty()) {
				newmsg.append(num);
				if (i + 1 < len) {
					newmsg.append('-');
				}
			}
		}

		return new Message(msg.title, newmsg.toString());
	}

	public int call(A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;
		return b.i;
	}
}
