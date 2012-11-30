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
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;

public class Line {
    private int rowStart = Integer.MAX_VALUE;
    private int columnStart = Integer.MAX_VALUE;
    private int columnEnd = -1;
    private String filename;
    private int type = -1;

    public Line() { }

    public int getRowStart() {
        return rowStart;
    }

    public int getColumnStart() {
        return columnStart;
    }

    public int getColumnEnd() {
        return columnEnd;
    }

    public String getFileName() {
        return filename;
    }

    public int getType() {
        return type;
    }

    public void setFileName(String name) {
        filename = name;
    }

    public void setType(int t) {
        type = t;
    }

    public void setRowStart(int s) {
        if (s < rowStart) rowStart = s;
    }

    public void setColumnStart(int s) {
        if (s < columnStart) columnStart = s;
    }

    public void setColumnEnd(int e) {
        if (e > columnEnd) columnEnd = e;
    }

    public String toString() {
        String str = "row " + rowStart +"\n";
        str += "column "+ columnStart + " - " + columnEnd +"\n";
        str += "file " + filename +"\n";
        str += "type "+ type + "\n";
        return str;
    }
}
