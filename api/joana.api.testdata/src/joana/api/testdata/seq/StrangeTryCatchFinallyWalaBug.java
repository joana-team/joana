/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;


/**
 * TODO: @author Add your name here.
 */
public class StrangeTryCatchFinallyWalaBug {

	public static void main(String[] args) {
		StrangeTryCatchFinallyWalaBug s = new StrangeTryCatchFinallyWalaBug();
		s.runWorker(new Worker());
	}
	
	Runnable getTask() {
		return null;
	}
	
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt

//                if ((runStateAtLeast(ctl.get(), STOP) ||
//                     (Thread.interrupted() &&
//                      runStateAtLeast(ctl.get(), STOP))) &&
//                    !wt.isInterrupted())
//                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }
    
    void beforeExecute(Thread thread, Runnable task) {
    }
    void afterExecute(Runnable task, Throwable thrown) {
    }
    void processWorkerExit(Worker w, boolean completedAbruptly) {
    }
}

class Worker {
	Runnable firstTask;
	int completedTasks;
	void unlock() {
	}
	
	void lock() {
	}
}