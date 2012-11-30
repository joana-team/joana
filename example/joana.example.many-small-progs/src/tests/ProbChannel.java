/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class ProbChannel {

    static Data d;

    public static void main(String[] args) {
        d = new Data();
        Thread1 t1 = new Thread1();
        Thread2 t2 = new Thread2();

        t1.d = d;
        t2.d = d;

        t1.start();
        t2.start();

        System.out.println(d.a);
    }


    public static class Data {
        int a;

        public void set(int x) {a = x;}
        public int get() {return a;}
    }
}

class Thread1 extends Thread	{
    ProbChannel.Data d;

    public void run() {
        int h = 3;
        while (h > 0) {
            h --;
        }
        d.a = 1;
    }
}

class Thread2 extends Thread	{
    ProbChannel.Data d;

    public void run() {
        d.a = 0;
    }
}
