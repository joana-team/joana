/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package program;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import module.Module;
import module.Module.Message;


public class Program {

	private static final String JAR_MODULE = "mojo-test-modules.jar";
	private static final String JAR_PROGRAM = "mojo-test-modules.jar";

	private static final String MODULE_PROVIDER_1 = "module.provider1.ModuleP1";
	private static final String MODULE_PROVIDER_2 = "module.provider2.ModuleP2";

	private static final JARClassLoader CLS_MODULE = new JARClassLoader(new File(JAR_MODULE));
	private static final JARClassLoader CLS_PROGRAM = new JARClassLoader(new File(JAR_MODULE));


	public void run() throws IOException {
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String msgstr = in.readLine();
		Message msg = new Message("1", msgstr);
		System.out.println(msg);

		Module enc = load(MODULE_PROVIDER_1);
		Message sec = enc.encrypt(msg, 4711);
		System.out.println(sec);

		Module enc2 = load(MODULE_PROVIDER_2);
		Message sec2 = enc2.encrypt(msg, 4711);
		System.out.println(sec2);
	}

	public static void main(String [] argv) throws IOException {
		Program p = new Program();
		p.run();
	}

	public Module load(final String name) {
		Module m = null;

		try {
			Class cls = CLS_MODULE.loadClass(name);
			Object obj = cls.newInstance();
			if (obj instanceof Module) {
				m = (Module) obj;

				System.out.println(m.getClass().getSimpleName() + " loaded.");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return m;
	}

}
