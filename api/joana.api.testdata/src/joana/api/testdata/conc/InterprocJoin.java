/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class InterprocJoin {
	static class Thread1 extends Thread {
		public void run() {
			System.out.println("1");
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			System.out.println("2");
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			System.out.println("3");
		}
	}

	static Thread1 t1 = new Thread1();
	static Thread2 t2 = new Thread2();
	static Thread3 t3 = new Thread3();
	
	static int x;
	static ChoiceA choice = (x < 3) ? new ChoiceA() : new ChoiceB();
	
	public static void main(String[] args) throws InterruptedException {
		// subtest 1
		t1.start();
		choice.subtest1call1();
		System.out.println(true);
		
		// subtest 2
		t2.start();
		subtest2call1();
		System.out.println(42);
		
		// subtest 3
		t3.start();
		subtest3call1();
		System.out.println("End");
	}
	
	static class ChoiceA {
		//methods for the first test part
		void subtest1call1() throws InterruptedException {
			t1.join();
		}

		void subtest2call3() throws InterruptedException {
			new ChoiceA().subtest2call4();
		}
		
		void subtest2call4() throws InterruptedException {
			if (x > 0) {
				subtest2call5();
			}
			t2.join();
		}

		void subtest2call5() throws InterruptedException {
			subtest2call6();
		}
		
		void subtest2call6() throws InterruptedException {
		}
		
		void subtest3call4() throws InterruptedException {
			subtest3call4a();
		}
		
		void subtest3call4a() throws InterruptedException {
		}
		
		void subtest3call5() throws InterruptedException {
		}
	}
	
	static class ChoiceB extends ChoiceA {
		void subtest1call1() throws InterruptedException {
		}

		void subtest2call3() throws InterruptedException {
			subtest2call4();
		}

		void subtest2call4() throws InterruptedException {
		}
		
		void subtest3call4() throws InterruptedException {
		}
		
		void subtest3call5() throws InterruptedException {
		}
	}
	
	static void subtest2call1() throws InterruptedException {
		subtest2call2();
	}

	static void subtest2call2() throws InterruptedException {
		choice.subtest2call3();
	}
	
	static void subtest3call1() throws InterruptedException {
		subtest3call2();
	}
	
	static void subtest3call2() throws InterruptedException {
		if (x < 1) {
			subtest3call3();
		}
		choice.subtest3call4();
		t3.join();
		choice.subtest3call5();
	}
	
	static void subtest3call3() throws InterruptedException {
	}
}
