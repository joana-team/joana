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

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class provides basic logging utilities.
 * Logging is done using log keys.  You turn messaging
 * on certain keys on/off.
 * Example:
 *     <p><pre><blockquote>
 *     logToStdout("test", LL_HIGH);
 *     Log.log("test", "hello");
 *     logOff("test);
 *     </blockquote></pre>
 * <p>This takes initial sets of log keys from debug.properies.
 * There are six entires in that file:
 * log.toFileLow, log.toFileMed, log.toFileHigh, log.low, log.med, log.high
 * which control the initial sets of log keys to stdout and to files
 * for each log level.  The values are sequences of keys with ; separating them.
 */
public class Log {

    /** log just message */
    static public final int LL_LOW = 0;

    /** log stamp, key, message */
    static public final int LL_MED = 1;

    /** log stap, key, loc, message */
    static public final int LL_HIGH = 2;

    /** log whole stack with evertyhing else */
    static public final int LL_EXT = 3;

    static private int stamp = 0;
    static private Hashtable<String, LogInfo> logKeys;

    static private String[] listToArray(String s) {
        StringTokenizer st = new StringTokenizer(s, ";", false);
        int len = st.countTokens();
        String ss[] = new String[len];

        for (int i = 0; i < len; i++) {
            ss[i] = st.nextToken();
        }

        return ss;
    }

    static {
        logKeys = new Hashtable<String, LogInfo>();
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("debug.properties"));
            String[] ss = listToArray(p.getProperty("log.toFileLow", ""));

            for (int i = 0; i < ss.length; i++) {
                logToFile(ss[i], LL_LOW);
            }

            ss = listToArray(p.getProperty("log.toFileMed", ""));
            for (int i = 0; i < ss.length; i++) {
                logToFile(ss[i], LL_MED);
            }

            ss = listToArray(p.getProperty("log.toFileHigh", ""));
            for (int i = 0; i < ss.length; i++) {
                logToFile(ss[i], LL_HIGH);
            }

            ss = listToArray(p.getProperty("log.low", ""));
            for (int i = 0; i < ss.length; i++) {
                logToStdout(ss[i], LL_LOW);
            }
            ss = listToArray(p.getProperty("log.med", ""));
            for (int i = 0; i < ss.length; i++) {
                logToStdout(ss[i], LL_MED);
            }

            ss = listToArray(p.getProperty("log.high", ""));
            for (int i = 0; i < ss.length; i++) {
                logToStdout(ss[i], LL_HIGH);
            }

        } catch(IOException ioe) { }
    }

    /**
     * helper class to keep info on a single key.
     */
    private static class LogInfo {
        public String key;
        PrintWriter ps;
        public int logLevel;

        public LogInfo(String key, PrintWriter ps, int logLevel) {
            this.key = key;
            this.ps = ps;
            this.logLevel = logLevel;
        }
    }


    /**
     * Start loggin on key, at level logLevel, and printing to ps.
     */
    static public void logOn(String key, PrintWriter ps, int logLevel) {
        System.out.println("[logging " + key + "]");
        logKeys.put(key, new LogInfo(key, ps, logLevel));
    }

    /**
     * turn off logging on key.
     */
    static public void logOff(String key) {
        logKeys.remove(key);
    }

    /**
     * Start logging key, putting output in file log-key.log.
     */
    static public void logToFile(String key, int logLevel) {
        try {
            FileWriter fw = new FileWriter("log-" + key + ".log");
            Assert.notFalse(fw != null);
            PrintWriter pw = new PrintWriter(fw, true);
            Assert.notFalse(pw != null);
            logOn(key, pw, logLevel);

        } catch (Throwable e) {
            Assert.fail(e);
        }
    }

    /**
     * log on key, and level logLevel, to standard out.
     */
    static public void logToStdout(String key, int logLevel) {
        logOn(key, new PrintWriter(System.out), logLevel);
    }

    static private String pad = "     ";
    private static String getStamp() {
        String s = Integer.toString(stamp++);
        if (s.length() > pad.length()) return s;
        return pad.substring(s.length()) + s;
    }

    /**
     * produce a log message and logging on key is enabled.
     */
    synchronized static public void log(String key, String s) {
        LogInfo li = (LogInfo)logKeys.get(key);
        String msg;
        if (li == null) return;
        switch (li.logLevel) {
        case LL_LOW: msg = s; break;
        case LL_MED: msg = getStamp() + "[" + key + "] " + s; break;
        case LL_HIGH: {
            // this is somewhat specific on the stack dump format.
            String stack = Debug.getStackDump();
            int startLoc = stack.indexOf("\n") + 1;
            startLoc = stack.indexOf("\n", startLoc) + 1;
            startLoc = stack.indexOf("\n", startLoc) + 1;
            String thirdLine =
                stack.substring(stack.indexOf("(", startLoc),
                        stack.indexOf(")", startLoc) + 1);
            msg = getStamp() + "[" + key + "]"+ thirdLine + ": " + s;
            break;
        }
        case LL_EXT: {
            String stack = Debug.getStackDump();
            msg = getStamp() + "[" + key + "] " + stack + s;
            break;
        }
        default: msg = "unkown log level: " + li.logLevel;
        }
        li.ps.println(msg);
        li.ps.flush();
    }
}
