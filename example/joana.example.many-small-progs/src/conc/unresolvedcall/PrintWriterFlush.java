/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.unresolvedcall;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class PrintWriterFlush {
	
	public static void main(String[] args) throws FileNotFoundException {
		final PrintWriter pw1 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("rofl.tmp"))));
		final PrintWriter pw2 = new PrintWriter(                   new OutputStreamWriter(new FileOutputStream("rofl.tmp")) );
		pw1.flush();
		//pw2.flush();
	}
	

}
