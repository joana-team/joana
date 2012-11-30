/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc;

public class TimeTravel {
    Data d = new Data(11);

    public static void main(String[] args) {

        new TimeTravel().m();
    }

    public void m() {

        Thread1 t = new Thread1();
        t.data = d;
        t.start();


        int x = d.x;
        System.out.println(x);
        d.x = 0;
        System.out.println(d.x);
    }

    static class Thread1 extends Thread {
        Data data;

        public void run() {
            int a = data.x + 4;
            data.x = data.x * a;
        }
    }

    static class Data {
        int x;

        public Data(int y) {
            x = y;
        }
    }
}
