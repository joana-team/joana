/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;
/**
 * @author Martin Hecker
 */
public class ArrayAccess {
	private static User[] users = new User[256];
	private static User u1 = new User(42,1024);
	private static User u2 = new User(17,SECRET);
	
	public static void main(String[] args) {
		foo(new User(42,0));
	}
	
	static void foo(User u) {
		users[u1.id] = u1;
		users[u2.id] = u2;
		
		int pw = toggle(users[u.id].password);
		leak(pw);
	}
}



class User {
	int id; // UserInformation[this.id]
	int password; //UserInformation[this.id]
	/**
	 * 
	 */
	public User(int id, int password) {
		this.id = id;
		this.password = password;
	}
}

class Report {
	User user;
	int password; //UserInformation[this.user.id]
}