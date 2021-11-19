/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.pruning.test1.application;

import joana.api.testdata.pruning.test1.library.CallbackInterface;
import joana.api.testdata.pruning.test1.library.Library;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class Application {
	public static void main(String[] args) {
		final CallbackInterface cb = new CallbackImplementation();
		int result = Library.combineAllWrapper(cb, 17, 42);
		leak(toggle(result));
	}

}

class CallbackImplementation implements CallbackInterface {
	/* (non-Javadoc)
	 * @see joana.api.testdata.pruning.test1.library.CallbackInterface#combine(int, int)
	 */
	@Override
	public int combine(int x, int y) {
		return x + y;
	}
	@Override
	public int getNeutral() {
		return SECRET;
	}
}
