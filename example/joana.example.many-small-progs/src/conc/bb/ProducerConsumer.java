/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.bb;

public class ProducerConsumer {
    //~ Methods ................................................................

    public static void main(String[] args) {
        int numSlots = 4;

        // create the bounded buffer
        BoundedBuffer bb = new BoundedBuffer(numSlots);

        // start the Producer and Consumer threads
        int count = 3;
        for(int i = 0; i < count; i++) {
			Thread producer = new Thread(new Producer(bb));
			Thread consumer = new Thread(new Consumer(bb));
			producer.start();
			consumer.start();
        }
    }
}
