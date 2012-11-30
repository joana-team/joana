/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import tests.ProbChannel.Data;

public class FixedProbChannel {
    static Data d;

    public static void main(String[] args) {
        d = new Data();
        Thread1a t1 = new Thread1a();
        Thread2a t2 = new Thread2a();

        t1.d = d;
        t2.d = d;

        t1.start();
        t2.start();

        try {
        	d.a = ((Integer)Broker.broker().receive(d, t2, Broker.MAIN_THREAD)).intValue();
        } catch(Exception e) {}

        System.out.println(d.a);
    }
}

class Thread1a extends Thread {
    Data d;

    public void run() {
        int h = 3;
        while (h > 0) {
            h --;
        }
        try {
        	Broker.broker().send(d, this, Broker.MAIN_THREAD, new Integer(1)); // statt   d.a = 1;
        } catch(Exception e) {}
    }
}

class Thread2a extends Thread	{
    Data d;

    public void run() {
    	try {
        	Broker.broker().send(d, this, Broker.MAIN_THREAD, new Integer(1)); // statt   d.a = 0;
        } catch(Exception e) {}
    }
}
