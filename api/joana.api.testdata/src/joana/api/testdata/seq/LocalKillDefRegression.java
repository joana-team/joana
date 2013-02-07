/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

/**
 * @author Martin Mohr
 */
public class LocalKillDefRegression {

	public static void main(String[] args) {
		if (Float.floatToRawIntBits(42.0f) < 17) {
			return;
		}
	}
}


class Bundle {
	
}

class MainActivity {

	/**
     * simulates the lifecycle of this activity
     * taken from class 'ActivityModelActivity' in the SCanDroid project
     */
    public void simulateLifecycle(Bundle b) {
    	while(true) { //while loop #1
    		int br1 = Float.floatToRawIntBits(42.0f);
    		if (br1 == 0) {
        		int br2 = Float.floatToRawIntBits(42.0f);
        		if (br2 == 0) {
        			continue;
        		}
        		else {
        			break; //break out of loop #1
        		}
    		}
    		else {
    			while (true) { //while loop #2
    	    		int br3 = Float.floatToRawIntBits(42.0f);
    	    		if (br3 == 0) {
    	    			break; //break out of loop #2
    	    		}
    	    		else {
    	    			continue;
    	    		}
    			}
        		int br4 = Float.floatToRawIntBits(42.0f);
        		if (br4 == 0) {
        			continue;
        		}
        		else {
        			break; //break out of loop #1
        		}
    		}
    	}
    }
}
