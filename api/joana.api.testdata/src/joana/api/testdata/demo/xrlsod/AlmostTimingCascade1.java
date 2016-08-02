/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo.xrlsod;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class AlmostTimingCascade1 {
	@Source
	public static int HIGH;
	@Sink
	public static int LOW;
	public static int tmp1;
	public static Wrapper tmp2 = new Wrapper();
	public static Wrapper tmp4 = new Wrapper();
	private static class Thread1 extends Thread {
		public void run() {
			tmp1 = 2;
		}
	}
	private static class Thread2 extends Thread {
		public void run() {
			while (tmp1 > 0) {
				tmp1--;
			}
			tmp2.x = 100;
		}
	}
	private static class Thread3 extends Thread {
		Wrapper tmp3;
		
		public Thread3(Wrapper tmp3) {
			this.tmp3 = tmp3;
		}

		public void run() {
			tmp3.x = 2;
		}
	}
	private static class Thread4 extends Thread {
		Wrapper tmp3;
		
		public Thread4(Wrapper tmp3) {
			this.tmp3 = tmp3;
		}
		
		public void run() {
			while (tmp3.x > 0) {
				tmp3.x--;
			}
			tmp4.set(100);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Thread1 t1 = new Thread1();
		t1.start();
		tmp1 = 100;
		t1.join();
		Thread2 t2 = new Thread2();
		t2.start();
		tmp2.x = 2;
		t2.join();
		Wrapper tmp3 = new Wrapper();
		Thread3 t3 = new Thread3(tmp3);
		t3.start();
		while (tmp2.x > 0) {
			tmp2.x--;
		}
		tmp3.x = 100;
		t3.join();
		Thread4 t4 = new Thread4(tmp3);
		t4.start();
		tmp4.x = 2;
		t4.join();
		LOW = tmp4.x;
	}
	
	private static class Wrapper {
		public int x;
		public void set(int param) {
			x = param;
		}
	}
}

