/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.rifl.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;



public class Sample1 {


	public static void main(final String[] args) throws IOException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		final String h = br.readLine();
		System.out.println(h);
	}
}