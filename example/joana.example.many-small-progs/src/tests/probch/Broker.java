/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests.probch;

import java.util.HashMap;


public class Broker {
	public static final Thread MAIN_THREAD = new Thread(){public void run(){}};

    private static Broker broker = new Broker();

    public static Broker broker() {
        return broker;
    }


    /******************************************/

    private HashMap channels;

    private Broker() {
        channels = new HashMap();
    }

    public void send(Object id, Thread prod, Thread con, Object value)
    throws InterruptedException {
    	// TODO: deep copy von value anlegen
        Key k = new Key(id, prod, con);
        LinearMailbox lm = (LinearMailbox) channels.get(k);

        if (lm == null) {
            lm = new LinearMailbox();
            channels.put(k, lm);
        }

        lm.put(value);
    }

    public Object receive(Object id, Thread prod, Thread con)
    throws InterruptedException {
        Key k = new Key(id, prod, con);
        LinearMailbox lm = (LinearMailbox) channels.get(k);

        if (lm == null) {
            lm = new LinearMailbox();
            channels.put(k, lm);
        }

        return lm.get();
    }



    private static class Key {
        private Object var;
        private Thread prod;
        private Thread con;

        Key(Object var, Thread prod, Thread con) {
            this.var = var;
            this.prod = prod;
            this.con = con;
        }

        public int hashCode() {
            return (var.hashCode() * 31 + prod.hashCode()) * 31 + con.hashCode();
        }

        public boolean equals(Object o) {
            return (o instanceof Key
                    && ((Key)o).var == this.var
                    && ((Key)o).prod == this.prod
                    && ((Key)o).con == this.con);
        }
    }
}
