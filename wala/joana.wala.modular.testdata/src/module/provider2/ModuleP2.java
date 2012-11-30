/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package module.provider2;

import module.Module;
import module.Module.A;

public class ModuleP2 implements Module {

	private int store;

	public int encrypt(int msg, int secret) {
		store = secret;
		return msg + secret;
	}

	public Message encrypt(Message msg, int secret) {
		store = secret;

		String newmsg = msg.data;
		for (int i = 0; i < secret; i ++) {
			newmsg = newmsg.substring(1) + newmsg.charAt(0);
		}

		return new Message(msg.title, newmsg);
	}

	public int getStore() {
		return store;
	}

	public int call(A a, A b, A c, int x) {
		c.i = a.i;
		return b.i;
	}

}
