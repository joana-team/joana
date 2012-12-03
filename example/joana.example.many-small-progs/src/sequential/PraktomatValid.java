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
public class PraktomatValid {

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
		public int points;
		
		public Review(Submission sub, int points) {
			this.sub = sub;
			this.points = points;
		}
	}
	
	public static Review review(Submission sub) {
		int points = 4;
		
		if (sub.code.length() < 10) {
			points--;
		}
		if (sub.code.contains("System.out.println")) {
			points--;
		}
		if (sub.code.contains("catch IOException")) {
			points -= 2;
		}
		
//		if (sub.matrNr == 4711) {
//			points = 4;
//		}
		
		return new Review(sub, points);
	}
	
	public static void main(String argv[]) {
		Submission sub = new Submission(Security.SECRET, "System.out.println(\"Hello world.\")");
		Review r = PraktomatValid.review(sub);
		Security.leak(r.points);
	}

}
