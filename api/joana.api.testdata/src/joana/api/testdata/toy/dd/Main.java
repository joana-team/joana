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
package joana.api.testdata.toy.dd;

import java.io.PrintStream;


class A {
	int sec;
	B b = new B();
}

class B {

	int x;

	public void set(int val) {
		x = val;
	}
}

class InfoLeaker {
	PrintStream myOut;

	public InfoLeaker(PrintStream out) {
		this.myOut = out;
	}

	public void leak(A a) {
		myOut.println(a.b.x);
	}
}

class InfoLeaker2 {
	PrintStream myOut;

	public InfoLeaker2(PrintStream out) {
		this.myOut = out;
	}

	public void leak(Secret s) {
		myOut.println(s.secretValue);
	}
}

class SetLeaker {
	Public sink;

	public SetLeaker(Public sink) {
		this.sink = sink;
	}

	public void leak(Secret sec) {
		this.sink.setValue(sec.secretValue);
	}
}

class Secret {

	public Secret(int secretValue) {
		this.secretValue = secretValue;
	}

	int secretValue;
}

class IndirectSecret {

	Secret sec;

	public IndirectSecret(Secret sec) {
		this.sec = sec;
	}
}

class Public {
	int publicValue;

	public void setValue(int newValue) {
		this.publicValue = newValue;
	}
}

class PrintLeaker {
	PrintStream myOut;

	public PrintLeaker(PrintStream out) {
		this.myOut = out;
	}

	public void leak(Secret sec) {
		myOut.println(sec.secretValue);
	}
}

class IndirectPrintLeaker {
	PrintLeaker myOut;

	public IndirectPrintLeaker(PrintLeaker out) {
		this.myOut = out;
	}

	public void leak(IndirectSecret sec) {
		myOut.leak(sec.sec);
	}
}

class IndirectLeakToPrintStreamWithoutStaticField {
	public static void main(String[] args) {
		IndirectSecret sec = new IndirectSecret(new Secret(42));
		IndirectPrintLeaker leaker = new IndirectPrintLeaker(new PrintLeaker(System.out));
		leaker.leak(sec);
	}
}

class LeakToPrintStreamWithoutStaticField {
	public static void main(String[] args) {
		Secret sec = new Secret(42);
		PrintLeaker leaker = new PrintLeaker(System.out);
		leaker.leak(sec);
	}
}

class LeakToPrintStreamWithStaticField {

	public static Secret sec = new Secret(42);

	public static void main(String[] args) {
		PrintLeaker leaker = new PrintLeaker(System.out);
		leaker.leak(sec);
	}
}

class SimpleLeak {

	public static void leak(A a, B b) {
		b.set(a.sec);
	}

	public static void main(String[] args) {
		A a = new A();
		B b = new B();
		leak(a,b);
	}
}

class LeakAtoB {

	public void leak(A a, B b) {
		b.set(a.sec);
	}
}

class SlightlyMoreComplexLeak {
	public static void main(String[] args) {
		A a = new A();
		B b = new B();
		LeakAtoB leaker = new LeakAtoB();
		leaker.leak(a, b);
	}
}


class ShouldLeakWithStaticField {

	public static A a;
	public static B b;

	public static void main(String[] args) {
		InfoLeaker leaker = new InfoLeaker(System.out);
		a = new A();
		b = new B();
		b.x = 42;
		a.b = b;
		leaker.leak(a);
	}
}

class ShouldLeakWithoutStaticField {

	public static void main(String[] args) {
		InfoLeaker leaker = new InfoLeaker(System.out);
		A a = new A();
		B b = new B();
		b.x = 42;
		a.b = b;
		leaker.leak(a);
	}
}

class Main5 {

	//public static A a2 = new A();
	//public static Public pub = new Public();
	//public static Secret sec = new Secret();

	public static Object o = new Object();

	public static void main(String[] args) {
		SetLeaker l = new SetLeaker(new Public());
		l.leak(new Secret(42));
//		InfoLeaker l0 = new InfoLeaker(System.out);
//		A a = new A();
//		B b = new B();
//		b.x = 42;
//		a.b = b;
//		l0.leak(a);

		//InfoLeaker2 l2 = new InfoLeaker2(System.out);
		//l2.leak(sec);
	}
}


class Main4 {

	public static void main(String[] args) {
		InfoLeaker leaker = new InfoLeaker(System.out);

		// nach Aufruf ist System.out mit leaker.out gealiased

		A a = new A();
		leaker.leak(a);
	}
}


class Main2 {

	public static void main(String[] args) {
		A a1 = new A();
		A a2 = new A();
		A a3 = new A();
		B b0;
		if (a3.b.x != 1) {
			a3 = a1;
			b0 = a1.b;
		} else {
			b0 = new B();
		}
		b0.x = a3.sec;
	}
}

public class Main {

	public static void main(String[] args) {
		A a = new A();
		B b0 = a.b;

		/**
		 * Since b0 and a.b are aliased, a flow to b0 is also a flow to a.b.
		 **/
		b0.x = a.sec;

		/**
		 * In general, suppose we want to annotate the attribute b of class A as a sink,
		 * and n is a node in the sdg representing a reference to b. After that,
		 * there can be subsequent modifications of b or an object which is aliased
		 * to b, so we also have to annotate all modifications and actual-out-parameters,
		 * which are data-dependent on n.
		 */

	}
}
