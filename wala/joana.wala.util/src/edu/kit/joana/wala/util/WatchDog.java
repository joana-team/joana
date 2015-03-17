/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;


/**
 * This Thread can be used to monitor long running evaluation tasks and trigger termination in case they hit a certain
 * timelimit.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class WatchDog extends Thread {

	private final IProgressMonitor progress;
	private final long msTillAbort;
	private final long msToCleanup;
	private boolean done = false;

	public WatchDog(final IProgressMonitor progress, final long msTillAbort, final long msToCleanup) {
		this.progress = progress;
		this.msTillAbort = msTillAbort;
		this.msToCleanup = msToCleanup;
	}

	public void done() {
		done = true;
	}

	public void run() {
		done = false;
		long current = System.currentTimeMillis();
		long timeToAbort = current + msTillAbort;

		while (!done && current < timeToAbort) {
//			System.out.println("timeout checked: " + (timeToAbort - current) + "ms to go");
			try {
				sleep(1 * 1000 * 30 /* check every 30 seconds */);
			} catch (InterruptedException e) {
				// somebody woke me up -> no problem just check if
				// its time to abort the computation
			}

			current = System.currentTimeMillis();
		}

		if (!done) {
			//System.out.println("TIMEOUT hit - aborting computation. Program has " + msToCleanup + " to exit gracefully.");
			progress.cancel();

			// wait for normal termination, then force quit
			try {
				sleep(msToCleanup);
			} catch (InterruptedException e) {
				// somebody woke me up -> no problem just check if
				// its time to abort the computation
			}

			if (!done) {
				System.out.println("Program did not shut down in time - forcing termination.");
				System.exit(4711);
			}
		} else {
		//	System.out.println("WatchDog: normal termination.");
		}
	}

}
