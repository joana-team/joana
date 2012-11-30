/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.dp;

class Philosopher implements Runnable {
   protected int id = 0;
   protected DiningServer ds = null;
   protected Thread t;

   /*@ behavior
     @   requires ds != null;
     @   assignable this.id, this.ds;
     @   ensures this.id == id && this.ds == ds;
     @*/
   public Philosopher(int id, DiningServer ds) {
      this.id = id;
      this.ds = ds;
      (t = new Thread(this)).start();
   }

   /*@ behavior
     @   ensures \invariant_for(ds);
     @*/
   protected void think() { }

   /*@ behavior
     @   ensures \invariant_for(ds);
     @*/
   protected void eat() { }

   /*@also
     @ behavior
     @   requires ds != null;
     @   ensures false;
     @   diverges true;
     @*/
   public void run() {
      while (true) {
         think();
         ds.takeForks(id);
         eat();
         ds.putForks(id);
      }
   }
}

