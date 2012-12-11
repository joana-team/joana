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
package joana.api.testdata.toy.conc;

class StringWrapper {
	private String inner;

	public StringWrapper(String inner) {
		this.inner = new String(inner);
	}

	public String get() {
		return inner;
	}

	public void set(String newString) {
		inner = new String(newString);
	}
}

class ReadingThread extends Thread {

	private StringWrapper value;

	public ReadingThread(StringWrapper value) {
		this.value = value;
	}

	public void run() {
		System.out.println(value.get());
	}
}

class WritingThread extends Thread {

	private StringWrapper value;

	public WritingThread(StringWrapper value) {
		this.value = value;
	}

	public void run() {
		value.set("Martin");
	}

}

public class ThreadTestRW {
	public static void main(String[] args) {
		StringWrapper value = new StringWrapper("Test");
		ReadingThread tRead = new ReadingThread(value);
		WritingThread tWrite = new WritingThread(value);
		tRead.start();
		tWrite.start();
	}
}
