/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.conc;


class Secret {
	int value;
}

class Public {
	int value;
}


public class SimpleInsecure {

	private static Public pub = new Public();

	private static Secret loadFromFile() {
		return null;
	}

	private static void writeToPublic(Secret s, Public p) {
		p.value = s.value;
	}


	public static void main(String[] args) {
		Secret s = loadFromFile();
		writeToPublic(s, pub);
	}



}
