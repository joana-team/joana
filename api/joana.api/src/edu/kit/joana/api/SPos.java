/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SPos implements Comparable<SPos> {
	private final String sourceFile;
	private final String simpleSource;
	private final int startChar;
	private final int endChar;
	private final int startLine;
	private final int endLine;
	
	public SPos(final String sourceFile, final int startLine, final int endLine, final int startChar,
			final int endChar) {
		this.sourceFile = sourceFile;
		if (sourceFile.contains("/")) {
			this.simpleSource = sourceFile.substring(sourceFile.lastIndexOf("/") + 1);
		} else if (sourceFile.contains("\\")) {
			this.simpleSource = sourceFile.substring(sourceFile.lastIndexOf("\\") + 1);
		} else {
			this.simpleSource = sourceFile;
		}
		this.startLine = startLine;
		this.endLine = endLine;
		this.startChar = startChar;
		this.endChar = endChar;
	}
	
	public int hashCode() {
		return sourceFile.hashCode() + 13 * startLine;
	}
	
	public boolean isAllZero() {
		return startLine == 0 && endLine == 0 && startChar == 0 && endChar == 0;
	}
	
	public boolean hasCharPos() {
		return !(startChar == 0 && startChar == endChar);
	}
	
	public boolean isMultipleLines() {
		return startLine != endLine;
	}
	
	public boolean equals(Object o) {
		if (o instanceof SPos) {
			final SPos spos = (SPos) o;
			return sourceFile.equals(spos.sourceFile) && startLine == spos.startLine && endLine == spos.endLine
					&& startChar == spos.startChar && endChar == spos.endChar;
		}
		
		return false;
	}

	@Override
	public int compareTo(SPos o) {
		if (sourceFile.compareTo(o.sourceFile) != 0) {
			return sourceFile.compareTo(o.sourceFile);
		}
		
		if (startLine != o.startLine) {
			return startLine - o.startLine;
		}
		
		if (endLine != o.endLine) {
			return endLine - o.endLine;
		}
		
		if (startChar != o.startChar) {
			return startChar - o.startChar;
		}
		
		if (endChar != o.endChar) {
			return endChar - o.endChar;
		}
		
		return 0;
	}
	
	public String toString() {
		if (hasCharPos() && isMultipleLines()) {
			return simpleSource + ":(" + startLine + "," + startChar + ")-(" + endLine + "," + endChar +")"; 
		} else if (hasCharPos()) {
			return simpleSource + ":(" + startLine + "," + startChar + "-" + endChar +")"; 
		} else if (isMultipleLines()) {
			return simpleSource + ":" + startLine + "-" + endLine; 
		} else {
			return simpleSource + ":" + startLine; 
		}
	}
	
	public String getSourceCode(final File sourceFile) {
		final File f = sourceFile;
		try {
			String code = "";
			final BufferedReader read = new BufferedReader(new FileReader(f));
			for (int i = 0; i < startLine-1; i++) {
				read.readLine();
			}

			if (!isMultipleLines()) {
				final String line = read.readLine();
				if (hasCharPos()) {
					code = line.substring(startChar, endChar);
				} else {
					code = line;
				}
			} else {
				{
					final String line = read.readLine();
					if (hasCharPos()) {
						code = line.substring(startChar);
					} else {
						code = line;
					}
				}
				
				for (int i = startLine; i < endLine-1; i++) {
					code += read.readLine();
				}
				
				{
					final String line = read.readLine();
					if (hasCharPos()) {
						code += line.substring(0, endChar);
					} else {
						code += line;
					}
				}
			}

			read.close();
			
			return code;
		} catch (IOException e) {}
		
		return  "error getting source";
	}

	public String toStringFullFile() {
		if (hasCharPos() && isMultipleLines()) {
			return sourceFile + ":(" + startLine + "," + startChar + ")-(" + endLine + "," + endChar +")"; 
		} else if (hasCharPos()) {
			return sourceFile + ":(" + startLine + "," + startChar + "-" + endChar +")"; 
		} else if (isMultipleLines()) {
			return sourceFile + ":" + startLine + "-" + endLine; 
		} else {
			return sourceFile + ":" + startLine; 
		}
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public int getStartChar() {
		return startChar;
	}

	public int getEndChar() {
		return endChar;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getEndLine() {
		return endLine;
	}
	

}