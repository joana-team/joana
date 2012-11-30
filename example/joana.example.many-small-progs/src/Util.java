/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

public class Util {
	static double conv(String eingabe) {
		double wert = 0.0;
		double genauigkeit = 1.0;
		boolean nachKomma = false;
		boolean error = false;
		int pos = 0;

		while (pos < eingabe.length() && !error) {
			char zeichen = eingabe.charAt(pos);
			if (Character.isDigit(zeichen)) {
				if (nachKomma) {
					genauigkeit = genauigkeit / 10.0;
				}
				wert = 10.0 * wert + Character.digit(zeichen, 10);
			} else if ((zeichen == '.') && !nachKomma) {
				nachKomma = true;
				if (pos == eingabe.length() - 1) {
					error = true;
				}
			} else {
				error = true;
			}
			pos = pos + 1;
		}

		if (error || (eingabe.length() == 0)) {
			return -1.0;
		} else {
			return wert * genauigkeit;
		}
	}

}
