/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.lg;
import java.util.LinkedList;

/**
 * Priority queue for holding messages.
 *
 * @author Jan Miksatko
 * @author Jan Antolik
 */
class MessageQueue<Msg>  {
    private LinkedList<Msg> queue;

    public MessageQueue() {
        queue = new LinkedList<Msg>();
    }

    /**
     * Adds a message to the queue (i.e. send object) and notify waiting thread(s).
     * @param msg a message to add
     */
    public synchronized void send(Msg msg) {
        queue.add(msg);
        notifyAll();		// msg arrived, notify waiting thread(s)
    }

    /**
     * Removes a message from the queue. If the queue is empty, block the reciever.
     * @return the message that was removed from the queue
     */
    public synchronized Msg recieve() {
        while (queue.isEmpty()) {
            try {
                wait();		// wait for msg

            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }

        return queue.remove(0);
    }
}
