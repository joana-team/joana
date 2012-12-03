/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sequential;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class PraktomatLeak {

	public static class Submission {
		public int matrNr;
		public String code;
		
		public Submission(int matrNr, String code) {
			this.code= code;
			this.matrNr = matrNr;
		}
	}
	
	public static class Review {
		public Submission sub;
		public int failures;
		
		public Review(Submission sub, int failures) {
			this.sub = sub;
			this.failures = failures;
		}
	}
	
	public static Review runChecks(Submission sub) {
		int failures = 0;
		
		if (sub.code.contains("System.out.println")) {
			failures++;
		}
		if (sub.code.contains("catch IOException")) {
			failures += 2;
		}
		
		if (sub.matrNr == 4711) {
			failures = 0;
		}
		
		return new Review(sub, failures);
	}
	
	public static void main(String argv[]) {
		Submission sub = new Submission(Security.SECRET, "System.out.println(\"Hello world.\")");
		Review r = PraktomatLeak.runChecks(sub);
		Security.leak(r.failures);
	}

}
