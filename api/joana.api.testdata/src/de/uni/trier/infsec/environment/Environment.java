package de.uni.trier.infsec.environment;

class Node 
{
	long value;
	Node next;
	
	Node(long value, Node next){
		this.value = value; 
		this.next = next;
	}
}

public class Environment {

	private static boolean result; // the LOW variable
	
	private static Node list = null;
	private static boolean listInitialized = false;
		
	private static Node initialValue()
	{
		// Unknown specification of the following form:
		// return new Node(U1, new Node(U2, ...));
		// where U1, U2, ...Un are constant integers.
		return new Node(1, new Node(7,null));  // just an example
	}

    public synchronized static long untrustedInput()
    {
/*    	if (!listInitialized) {
    		list = initialValue();
    	    listInitialized = true;        
    	}
    	if (list==null) 
    		return 0;
    	long tmp = list.value;
    	list = list.next;
    	return tmp; */
    	return 42;
	}
		
    public synchronized static void untrustedOutput(long x)
    {
		if (untrustedInput()==0) {
			result = (x==untrustedInput());
			// System.out.println(result);
			throw new Error();  // abort
		}
	}
    
    public static int unstrudtedInputInt() {
    	return (int)untrustedInput();
    }
        
    public static byte[] untrustedInputMessage()
    {
/*		long llen = untrustedInput();
		int len = (int) llen;
		if (llen<0 || len!=llen) // check whether casting to int has changed its value
			return null;
		byte[] returnval = new byte[len];
		for (int i = 0; i < len; i++) {
			returnval[i] = (byte) Environment.untrustedInput();
		}
		return returnval; */
    	return bs; 
    }
    
    static byte[] bs = new byte[] {1,2,3}; 
    
    public static void untrustedOutputMessage(byte[] t)
    {
    	untrustedOutput(t.length);
		for (int i = 0; i < t.length; i++) {
			untrustedOutput(t[i]);
		}
    }
    
    public static void untrustedOutputString(String s)
    {
    	untrustedOutput(s.length());
    	for (int i = 0; i < s.length(); i++) {
    		untrustedOutput((long)s.charAt(i));
    	}
    }
}        
