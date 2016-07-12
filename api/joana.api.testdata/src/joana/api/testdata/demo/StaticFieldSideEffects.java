package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class StaticFieldSideEffects {

	public static void main(String[] argv) {
		A a = new A();
		B b = new B();
		// ok
		a.print();
		// illegal
		b.print();
		a.print();
	}

	@Sink
	public static void print(int s) {}
	
}

class A {
	public void foo() {}

	public int bar() {
		return S.pub;
	}

	public final void print() {
		foo();
		StaticFieldSideEffects.print(bar());
	}
}

class B extends A {
	public void foo() {
		S.pub = S.sec;
	}
}

class S { 
	public static int pub = 23;
	@Source
	public static int sec = 42; 
}