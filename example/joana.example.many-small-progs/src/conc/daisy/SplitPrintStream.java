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

import java.io.IOException;
import java.io.PrintStream;


/**
 * An output stream that splits into two.
 */
public class SplitPrintStream extends PrintStream {
    private PrintStream os2;

    /**
     * Pass in the two print streams to be fed from
     * the same source.  Use the new stream, and it will
     * forward all output to both os1 and os2.
     */
    public SplitPrintStream(PrintStream os1, PrintStream os2) {
        super(os1);
        this.os2 = os2;
    }


    public void flush()  {
        super.flush();
        this.os2.flush();
    }

    public void write (int b) {
        super.write(b);
        os2.write(b);
    }

    public void write (byte[] b)
    throws IOException {
        super.write(b);
        os2.write(b);
    }

    public void write (byte[] b, int off, int len) {
        super.write(b, off, len);
        os2.write(b, off, len);
    }
}
