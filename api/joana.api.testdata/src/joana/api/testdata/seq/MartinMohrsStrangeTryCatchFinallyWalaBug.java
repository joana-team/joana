/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

import java.io.IOException;

import org.omg.CORBA.portable.InputStream;

/**
 * TODO: @author Add your name here.
 * See https://github.com/wala/WALA/issues/123
 */
public class MartinMohrsStrangeTryCatchFinallyWalaBug {
	public static void main(String[] args) {
		MartinMohrsStrangeTryCatchFinallyWalaBug s = new MartinMohrsStrangeTryCatchFinallyWalaBug();
		s.m();
		leak(toggle(SECRET));
	}

	// deliberately chosen to be out of the analysisscop, due to default excludes
	private InputStream b;

	public boolean m() {
		boolean rc = false;
		try {
			b.read();
			rc = true;
		} catch (IOException aexp) {
		} catch (RuntimeException bexp) {
		}
		return (rc);
	}
}