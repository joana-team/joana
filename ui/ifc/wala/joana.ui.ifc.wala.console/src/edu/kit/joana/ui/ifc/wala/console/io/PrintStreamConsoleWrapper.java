/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput.Answer;


class StringToAnswer {
    public boolean canParseString(String str) {
	return ("yes".equals(str.toLowerCase()) || "no".equals(str.toLowerCase()));
    }

    public String errorMessage() {
	return "Please enter 'yes' or 'no.";
    }

    public Answer convertStringtoAnswer(String str) {
	if ("yes".equals(str.toLowerCase())) {
	    return Answer.YES;
	} else if ("no".equals(str.toLowerCase())){
	    return Answer.NO;
	} else {
	    return null;
	}
    }
}

public class PrintStreamConsoleWrapper implements IFCConsoleOutput {

    private final PrintStream logOut;
    private final PrintStream errOut;
    private final BufferedReader questionIn;
    private final PrintStream questionOut;
    private final PrintStream infoOut;
    private StringToAnswer strToAns = new StringToAnswer();

    public PrintStreamConsoleWrapper(PrintStream logOut, PrintStream errOut, BufferedReader questionIn,
	    PrintStream questionOut, PrintStream infoOut) {
	super();
	this.logOut = logOut;
	this.errOut = errOut;
	this.questionIn = questionIn;
	this.questionOut = questionOut;
	this.infoOut = infoOut;
    }

    @Override
    public void log(String logMessage) {
	logOut.print(logMessage);
    }

    @Override
    public void logln(String logMessage) {
	logOut.println(logMessage);
    }

    @Override
    public void info(String infoMessage) {
	infoOut.println(infoMessage);
    }

    @Override
    public void error(String errorMessage) {
	errOut.println(errorMessage);
    }

    @Override
    public Answer question(String questionMessage) {
	String answer = null;
	while (true) {
	    questionOut.print(questionMessage + " ");
	    try {
		answer = questionIn.readLine();
	    } catch (IOException e) {
		error("\nI/O error while reading answer!");
	    }
	    if (!strToAns.canParseString(answer)) {
		questionOut.println(strToAns.errorMessage());
	    } else {
		return strToAns.convertStringtoAnswer(answer);
	    }
	}


    }

	@Override
	public PrintStream getPrintStream() {
		return errOut;
	}
}
