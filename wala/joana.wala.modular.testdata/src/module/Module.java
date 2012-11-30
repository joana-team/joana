/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package module;

public interface Module {

	public static class Message {

		public final String title;
		public final String data;

		public Message(final String title, final String data) {
			this.title = title;
			this.data = data;
		}

		public String toString() {
			return "msg(" + title +", " + data + ")";
		}

		public int hashCode() {
			return data.hashCode();
		}

		public boolean equals(final Object other) {
			if (this == other) {
				return true;
			}

			if (other instanceof Message) {
				final Message msg = (Message) other;

				return title.equals(msg.title) && data.equals(msg.data);
			}

			return false;
		}
	}

	// ifc: => secret -!> \state
	public int encrypt(int msg, int secret);

	// ifc: => secret -!> (\state, msg.*)
	public Message encrypt(Message msg, int secret);

	public static class A {
		public int i;
	}

	//@ ifc: !{a,b} => x-!>\result
	public int call(A a, A b, A c, int x);
}
