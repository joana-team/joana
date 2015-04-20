package rs3.cloud.environment;

class Node {
	int value;
	Node next;
	Node(int v, Node n) {
		value = v; next = n;
	}
}

public class Environment {

	private static boolean result; // the LOW variable
	
	private static Node list = null;
	private static boolean listInitialized = false;
		
	private static Node initialValue() {
		// Unknown specification of the following form:
		// return new Node(U1, new Node(U2, ...));
		// where U1, U2, ...Un are constant integers.
		return new Node(1, new Node(7,null));  // just an example
	}

    public static int untrustedInput() {
    	if (!listInitialized) {
    		list = initialValue();
    	    listInitialized = true;        
    	}
    	if (list==null) return 0;
    	int tmp = list.value;
    	list = list.next;
    	return tmp;
	}
		
    public static void untrustedOutput(int x) {
		if (untrustedInput()==0) {
			result = (x==untrustedInput());
			throw new Error();  // abort
		}
	}
    
    public static byte[] untrustedInputMessage() {
		int len = untrustedInput();
		if (len<0) return null;
		byte[] returnval = new byte[len];
		for (int i = 0; i < len; i++) {
			returnval[i] = (byte) Environment.untrustedInput();
		}
		return returnval;    
    }
    
    public static void untrustedOutputMessage(byte[] t) {
    	untrustedOutput(t.length);
		for (int i = 0; i < t.length; i++) {
			untrustedOutput(t[i]);
		}
    }
    
    public static void untrustedOutputString(String s) {
    	untrustedOutput(s.length());
    	for (int i = 0; i < s.length(); i++) {
    		untrustedOutput((int)s.charAt(i));
    	}
    }
}        
