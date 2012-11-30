/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * This file is part of the Daisy distribution.  This software is
 * distributed 'as is' without any guarantees whatsoever. It may be
 * used freely for research but may not be used in any commercial
 * products.  The contents of this distribution should not be posted
 * on the web or distributed without the consent of the authors.
 *
 * Authors: Cormac Flanagan, Stephen N. Freund, Shaz Qadeer
 * Contact: Shaz Qadeer (qadeer@microsoft.com)
 */

package conc.daisy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;


/**
 * class providing a global debugger flag and other debugging tools.
 * this class reads information from debug.properties.
 */
public class Debug {

    /**
     * global flag to turn debug code on and off.
     */
    static public boolean debug = true;

    static {
        Properties p = new Properties();
        try {
            File f = Utility.findFileOnClasspath("debug.properties");

            if (f == null) {
                debug = false;

            } else {
                p.load(new FileInputStream(f));
                String s = p.getProperty("debug.debug", "false");
                debug = Boolean.valueOf(s).booleanValue();
            }

        } catch (Exception e) {
            Assert.notify(e);
        }
    }

    /**
     * return a string containing the current stack.
     */
    static public String getStackDump() {
        Writer w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        Throwable t = new Throwable();
        t.fillInStackTrace();
        t.printStackTrace(pw);
        String stack = w.toString();
        return stack;
    }

    /**
     * Enforce stdout/err logging.
     */
    static {
        try {
            if (Debug.debug) {
                PrintStream log = new PrintStream(new FileOutputStream("debug.log"), true);
                System.setOut(new SplitPrintStream(System.out, log));
                System.setErr(new SplitPrintStream(System.err, log));
            }

        } catch (Exception e) {
            Assert.fail(e);
        }
    }
}

