/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * TODO: @author Add your name here.
 */
import java.util.Scanner;

public class ExceptionInConstructor {
	@Source
	static int high;
	
	@Sink
	static int low;
	
	public ExceptionInConstructor() {
		int r = 1/high;
	}

	public static void main(String... args) {
		new ExceptionInConstructor();
		low = 1;
	}
}
