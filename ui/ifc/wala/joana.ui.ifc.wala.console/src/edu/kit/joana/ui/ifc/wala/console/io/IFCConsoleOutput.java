/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.io;

import edu.kit.joana.util.NullPrintStream;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface IFCConsoleOutput {

    public enum Answer {
	YES,NO;
    }

    public void log(String logMessage);
    public void logln(String logMessage);
    public void info(String infoMessage);
    public default void debug(String debugMessage) {}
    public void error(String errorMessage);
    public Answer question(String questionMessage);


    public PrintStream getPrintStream();

    public default PrintStream getDebugPrintStream(){
        return getPrintStream();
    }
}
