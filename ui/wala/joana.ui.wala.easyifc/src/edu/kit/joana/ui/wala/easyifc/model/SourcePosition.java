/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

public class SourcePosition implements Comparable<SourcePosition> {

    final int firstLine;
    final int lastLine;
    final int firstCol;
    final int lastCol;

    public SourcePosition(int firstLine, int lastLine, int firstCol, int lastCol) {
      this.firstLine = firstLine;
      this.lastLine = lastLine;
      this.firstCol = firstCol;
      this.lastCol = lastCol;
    }


    public int getFirstCol() {
      return firstCol;
    }

    public int getFirstLine() {
      return firstLine;
    }

    public int getFirstOffset() {
      return 0;
    }

    public int getLastCol() {
      return lastCol;
    }

    public int getLastLine() {
      return lastLine;
    }

    public int getLastOffset() {
      return 0;
    }

    public int hashCode() {
    	return firstLine + (13 * firstCol);
    }

    public boolean equals(final Object obj) {
    	if (obj instanceof SourcePosition) {
    		final SourcePosition s = (SourcePosition) obj;

    		return compareTo(s) == 0;
    	}

    	return false;
    }

    public int compareTo(final SourcePosition p) {
    	if (p == null) {
    		return -1;
    	} if (firstLine != p.getFirstLine()) {
	      return firstLine - p.getFirstLine();
	    } else if (firstCol != p.getFirstCol()) {
	      return firstCol - p.getFirstCol();
	    } else if (lastLine != p.getLastLine()) {
	      return lastLine - p.getLastLine();
	    } else if (lastCol != p.getLastCol()) {
	      return lastCol - p.getLastCol();
	    } else {
	      return 0;
	    }
    }

    @Override
    public String toString() {
      return "(" + firstLine + "," + firstCol + "-" + lastLine + "," + lastCol + ")";
    }

}
