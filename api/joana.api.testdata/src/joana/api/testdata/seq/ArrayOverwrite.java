/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;


import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET_BOOL;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;
/**
 * @author Martin Hecker
 */
public class ArrayOverwrite {
	
	static class B extends A {
		
	}
	
	public static void main(String[] args) {
		A[] as = new A[1];
		as[0] = null;

		B[] bs = new B[1];
		bs[0] = new B();

		foo(as, bs);
		boolean isNull = as[0] == null;
		leak(toggle(isNull)); 


		
		A[][] as2 = new A[1][1];
		as2[0][0] = null;
				
		B[][] bs2 = new B[1][1];
		bs2[0][0] = new B();
		bar(as2, bs2);
		
		boolean isNull2 = as2[0] == null;
		leak(toggle(isNull2)); 
		

		int[] is = new int[1];
		is[0] = 0;

		double [] ds = new double[1];
		ds[0] = 1.0;

		baz(is, ds);
		boolean isNull3 = is[0] == 0;
		leak(toggle(isNull3)); 
		
		Object[] os = new Object[1];
		os[0] = null;

		String[] ss = new String[1];
		ss[0] = "rolf";

		bam(os, ss);
		boolean isNull4 = as[0] == null;
		leak(toggle(isNull4)); 
		
		
	}
	
	static void foo(A[] as, B[] bs) {
		B b = null;
		if (SECRET_BOOL) {
			 b = new B(); 
		}
		as[0] = b;
	}
	
	static void bar(A[][] as, B[][] bs) {
		B[] b = null;
		if (SECRET_BOOL) {
			 b = new B[1]; 
		}
		as[0] = b;
	}
	
	static void baz(int[] as, double[] bs) {
		double b = 0.0;
		if (SECRET_BOOL) {
			 b = 1.0;
		}
		as[0] = (int) b;
	}
	
	static void bam(Object[] as, String[] bs) {
		String b = null;
		if (SECRET_BOOL) {
			 b = "lol"; 
		}
		as[0] = b;
	}

}
