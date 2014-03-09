/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package edu.kit.joana.wala.core;

import java.io.PrintStream;
import java.util.Stack;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;


/**
 *  A ProgressMonitor optimized for console output.
 *
 * @author Tobias Blaschke <code@tobiasblaschke.de>
 */
public class CliProgressMonitor implements IProgressMonitor {
    /**
     *  When running and a new beginTask arrives this is used.
     */
    private static class Status {
        public final int totalWork;
        public final String taskName;
        public final String subTaskName;
        public final int worked;

        public Status(final int totalWork, final int worked, final String taskName, final String subTaskName) {
            this.totalWork = totalWork;
            this.taskName = taskName;
            this.subTaskName = subTaskName;
            this.worked = worked;
        }
    }
    private final Stack<Status> backgroundedJobs = new Stack<Status>();

    //
    //  General configuration
    //

    /** Crops sub-tasks so progress fits into one line if true    */
    public boolean mayCrop = true;
    /** Crops on the left side instead of the right (only if mayCrop).
     *  Useful for class-names                                      */
    public boolean cropLeft = true;
    /** Start a new line on subTaskSwitch                           */
    public boolean keepSubTasks = false;

    //
    //  Terminal capabilities
    //
    /** Width of the output console in characters (for cropping)    */
    public static int WIDTH = 160;
    /** Terminal interprets the '\b'-character                      */
    public static boolean CAN_BACKSPACE = true;

    //
    //  Formatting options
    //
    /** Width of the progress bar (if workload is known)            */
    public static int BAR_WIDTH = 25;
    /** Between Task-Name and progress indicator                    */
    private static final String nameSeperator = "\t";
    
    /** Characters for the unknown-workload progress indicator      */
    private static final char unknownIndicator[] = {'-', '\\', '|', '/'};

    /*  Characters for the progress-bar                             */
    private static final char barStart = '|';
    private static final char barDone = '=';
    private static final char barPending = ' ';
    private static final char barEnd = '|';

    /** Separates Progress-indicator and subtask                    */
    private static final String indicatorSeperator = "  ";



    private IProgressMonitor delegate = null;
	private final PrintStream out;
	private boolean canceled = false;
	private boolean isWorking = false;

    private int totalWork;
    private int worked = 0;
    private String taskName;
    private String subTaskName;
    private int subTaskLenght;  // Needed for deleting the subtask

    private enum ePosition {
        HOME, TASK, INDICATOR, SUBTASK, OWN
    }
    ePosition position;

	public CliProgressMonitor(PrintStream out) {
		this.out = out;
        position = ePosition.HOME;
	}

    public CliProgressMonitor(PrintStream out, IProgressMonitor delegate) {
		this.out = out;
        this.delegate = delegate;
        position = ePosition.HOME;
	}


    private void printTask() {
        if (position == ePosition.HOME) {
            out.print(taskName + nameSeperator);
            position = ePosition.TASK;
        } else if (position == ePosition.OWN) {
            out.println(".");
            out.print(taskName + nameSeperator);
            position = ePosition.TASK;
        }
    }

    private void printSubTask() {
        if (position == ePosition.SUBTASK) {
            deleteSubTask();
        }
        if (position != ePosition.INDICATOR) {
            return;
        }
        if (subTaskName != null) {
            String printName;
            int cropToSize = WIDTH - (((totalWork == IProgressMonitor.UNKNOWN)?1:(BAR_WIDTH+2)) + taskName.length() + nameSeperator.length() +
                    indicatorSeperator.length());

            if (subTaskName.length() > cropToSize) {
                if (mayCrop) {
                    // Crop it
                    if (cropLeft) {
                        printName = subTaskName.substring(subTaskName.length() - cropToSize);
                    } else {
                        printName = subTaskName.substring(0, cropToSize);
                    }

                    // Print it
                    subTaskLenght = printName.length();
                    out.print(printName);
                    position = ePosition.SUBTASK;
                } else {
                    // Print on own line
                    out.println();
                    out.println("  [SUBTASK] " + subTaskName);
                    position = ePosition.HOME;
                    subTaskLenght = 0;
                }
            } else {
                // Nothing to crop
                printName = subTaskName;
                subTaskLenght = printName.length();
                out.print(printName);
                position = ePosition.SUBTASK;
            }
        }
    }

    private void deleteSubTask() {
        if (position == ePosition.SUBTASK) {
            if  (keepSubTasks) {
                out.println();
                position = ePosition.HOME;
            } else {
                for (int i=0; i < subTaskLenght; ++i) {
                    out.print("\b \b");
                    position = ePosition.INDICATOR;
                }
                if (subTaskLenght == 0) {
                    position = ePosition.INDICATOR;
                }
            }
        }
        /*
        if (subTaskName != null) {
            out.println();
            position = ePosition.HOME;
        }
       
        if (position == ePosition.SUBTASK) {
            position = ePosition.INDICATOR;
        }*/
    }

    private void drawBar(int work) {
        if (work < 0) work = 0;
        if (this.totalWork == IProgressMonitor.UNKNOWN) {
            switch (position) {
                case HOME:
                    printTask();
                case TASK:
                    out.print(unknownIndicator[work % unknownIndicator.length]);
                    position = ePosition.INDICATOR;
                    printSubTask();
                    break;
                case INDICATOR: 
                    out.print("\b" + unknownIndicator[work % unknownIndicator.length]);
                    printSubTask();
                    break;
                case SUBTASK:
                    deleteSubTask();
                    drawBar(work);
                    break;
                case OWN:
                    out.println();
                    position = ePosition.HOME;
                    drawBar(work);
            }
        } else {
             switch (position) {
                case HOME:
                    printTask();
                case TASK:
                    out.print(barStart);
                    int i = 0;
                    for (; i < ((work * BAR_WIDTH) / totalWork); ++i) {
                        out.print(barDone); 
                    }
                    if (work == totalWork) {
                        for (;i < BAR_WIDTH; ++i) {
                            out.print(barDone);
                        }
                    } else {
                        out.print(unknownIndicator[work % unknownIndicator.length]);
                        i++;
                        for (;i < BAR_WIDTH; ++i) {
                            out.print(barPending);
                        }
                    }
                    out.print(barEnd);
                    out.print(indicatorSeperator);

                    position = ePosition.INDICATOR;
                    
                    printSubTask();
                    break;
                case INDICATOR:
                    if (CAN_BACKSPACE) {
                        // XXX: one could optimize here
                        for (int j = 0; j < BAR_WIDTH + 2 + indicatorSeperator.length(); ++j) {
                            out.print("\b");
                        }
                        position = ePosition.TASK;
                        drawBar(work);
                    }
                    break;
                case SUBTASK:
                    deleteSubTask();
                    drawBar(work);
                    break;
                case OWN:
                    out.println();
                    position =  ePosition.HOME;
                    drawBar(work);
            }
        }
    }

	public void beginTask(String name, int totalWork) {
        if (isWorking) {
            backgroundedJobs.push(new Status(this.totalWork, this.worked, this.taskName, this.subTaskName));
            out.println(" - waiting...");
            position =  ePosition.HOME;
        }
        this.taskName = name;
        this.totalWork = totalWork;
        this.worked = 0;
        this.subTaskName = "";
	    subTaskLenght = 0;
        printTask();	
        isWorking = true;
        if (this.delegate != null) {
            this.delegate.beginTask(name, totalWork);
        }
	}

	public void done() {
		if (!isWorking) {
            out.println(" still done");
        } else {
            isWorking = false;
            worked(totalWork);
            deleteSubTask();
            printTask();
            if (totalWork == IProgressMonitor.UNKNOWN) {
                out.print("\b");
            }
            out.println("done");
            this.taskName = "No Task set";
            this.totalWork = IProgressMonitor.UNKNOWN;
            position =  ePosition.HOME;
            subTaskName = null;
        }
        if (this.delegate != null) {
            this.delegate.done();
        }

        if (! backgroundedJobs.isEmpty()) {
            final Status cont = backgroundedJobs.pop();
            beginTask(cont.taskName, cont.totalWork);
            subTask(cont.subTaskName);
            worked(cont.worked);
        }
	}

	/*public void internalWorked(double work) {
		out.print('.');
        if (this.delegate != null) {
            this.delegate.internalWorked(work);
        }
	}*/

	public boolean isCanceled() {
        if (this.delegate != null) {
            return canceled || this.delegate.isCanceled();
        } else {
    		return canceled;
        }
	}

	public void cancel() {
		if (!canceled) {
			isWorking = false;
            deleteSubTask();
            printTask();
			out.println("\b\tcanceled.");
		}
        subTaskName = null;
		canceled = true;
        if (this.delegate != null) {
            this.delegate.cancel();
        }
	}

	/*public void setTaskName(String name) {
        this.taskName = name;
        if (this.delegate != null) {
            this.delegate.setTaskName(name);
        }
	}*/

	public void subTask(String name) {
        isWorking = true;
        deleteSubTask();
        printSubTask();
		subTaskName = name;
        
        if (this.delegate != null) {
            this.delegate.subTask(name);
        }
	}

	public void worked(int work) {
		drawBar(work);
        this.worked = work;
        if (this.delegate != null) {
            this.delegate.worked(work);
        }
	}

}
