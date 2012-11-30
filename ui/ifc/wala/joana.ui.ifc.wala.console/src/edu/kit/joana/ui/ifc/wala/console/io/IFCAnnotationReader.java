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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.SDGMethodSelector;

public class IFCAnnotationReader {

	private BufferedReader in;
	private static final Pattern annPat = Pattern
			.compile("(.*?)\\((.*?)\\) - (.*)");
	private static final Pattern instrPat = Pattern
			.compile("instr \\((.*?):(\\d+)\\)");
	private static final Pattern paramPat = Pattern
			.compile("param (\\d+) of method (.*)");
	private static final Pattern exitPat = Pattern
			.compile("exit node of method (.*)");
	private SDGMethodSelector sel;

	public IFCAnnotationReader(SDGMethodSelector sel, InputStream in) {
		this.in = new BufferedReader(new InputStreamReader(in));
		this.sel = sel;
	}

	public Set<IFCAnnotation> readAnnotations() throws IOException,
			InvalidAnnotationFormatException, MethodNotFoundException {
		Set<IFCAnnotation> ret = new HashSet<IFCAnnotation>();
		IFCAnnotation next = readAnnotation();
		while (next != null) {
			ret.add(next);
			next = readAnnotation();
		}
		return ret;
	}

	public IFCAnnotation readAnnotation() throws IOException,
			InvalidAnnotationFormatException, MethodNotFoundException {
		String line = in.readLine();
		if (line == null) {
			return null;
		} else {
			Matcher m = annPat.matcher(line);
			if (!m.matches()) {
				throw new InvalidAnnotationFormatException(line);
			} else {
				Type type = Type.fromString(m.group(1));
				String[] levels = m.group(2).split("->");
				SDGProgramPart methodPart;
				try {
					methodPart = parseMethodPart(m.group(3));
				} catch (MethodNotFoundException mnfe) {
					mnfe.setAnnotation(line);
					throw mnfe;
				}
				assert levels.length == 1 || levels.length == 2;
				if (levels.length == 1)
					return new IFCAnnotation(type, levels[0], methodPart);
				else {
					return new IFCAnnotation(levels[0], levels[1], methodPart);
				}
			}
		}
	}

	private SDGProgramPart parseMethodPart(String methodPart)
			throws InvalidAnnotationFormatException, MethodNotFoundException {
		Matcher mInstr = instrPat.matcher(methodPart);
		Matcher mParam = paramPat.matcher(methodPart);
		Matcher mExit = exitPat.matcher(methodPart);

		if (mInstr.matches()) {
			String methodName = mInstr.group(1);
			int index = Integer.parseInt(mInstr.group(2));
			boolean found = sel.searchMethod(methodName);
			assert !found || sel.numberOfSearchResults() == 1;
			if (!found)
				throw new MethodNotFoundException(methodName);

			SDGInstruction instr = sel.getMethod(0).getInstructionWithBCIndex(
					index);
			assert instr != null;
			return instr;
		} else if (mParam.matches()) {
			int index = Integer.parseInt(mParam.group(1));
			String methodName = mParam.group(2);
			boolean found = sel.searchMethod(methodName);
			assert !found || sel.numberOfSearchResults() == 1;
			if (!found)
				throw new MethodNotFoundException(methodName);
			return sel.getMethod(0).getParameter(index);
		} else if (mExit.matches()) {
			String methodName = mExit.group(1);
			boolean found = sel.searchMethod(methodName);
			assert found && sel.numberOfSearchResults() == 1;
			return sel.getMethod(0).getExit();
		} else {
			// maybe it is just a method signature without any extras
			JavaMethodSignature methSig = JavaMethodSignature
					.fromString(methodPart);
			if (methSig != null) {
				boolean found = sel.searchMethod(methodPart);
				assert !found || sel.numberOfSearchResults() == 1;
				if (!found)
					throw new MethodNotFoundException(methodPart);
				return sel.getMethod(0);
			} else {
				throw new InvalidAnnotationFormatException(methodPart);
			}
		}
	}
}
