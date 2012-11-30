/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.lang.ref.WeakReference;
import java.util.LinkedList;


public class GC {

	public static final class CallBack {

		private GCCallBack gccb = null;

		public static CallBack create() {
			CallBack callb = new CallBack();
			CB cb = new CB(callb);
			callb.gccb = new GCCallBack(cb);

			return callb;
		}

		public void trigger() {
			System.out.print("[gc]");
			CB cb = new CB(this);
			this.gccb = new GCCallBack(cb);
		}

	}

	public static final class GCCallBack extends WeakReference<CB> {

		public GCCallBack(final CB cb) {
			super(cb);
		}

	}

	public static final class CB {

		private final CallBack callback;

		public CB(final CallBack callback) {
			this.callback = callback;
//			System.out.println("\nCB init");
		}

		public void finalize() {
//			System.out.println("\nCB finalize: " + Runtime.getRuntime().freeMemory() + " free");
			callback.trigger();
		}
	}

//	private static GCCallBack gccb = new GCCallBack();

	public static void main(String[] args) {
//		Runtime.getRuntime().g
		LinkedList<Object> list = new LinkedList<Object>();
		CallBack cb = CallBack.create();

		System.out.println("main start.");

		for (int i = 0; i < 2000000; i++) {
			list.add(new Object());
			if (i % 100000 == 0) {
				System.out.print("_");
				System.gc();
			} else if (i % 10000 == 0) {
				System.out.print(".");
			}

		}

		System.out.println("\nmain done.");
	}

}
