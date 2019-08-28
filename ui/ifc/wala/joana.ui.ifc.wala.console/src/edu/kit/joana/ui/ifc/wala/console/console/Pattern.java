/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.annotations.Annotation;

import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ui.annotations.Declassification;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.ProgramPartToString;

public class Pattern {
	
	public static enum PatternType {
		SIGNATURE, ID
	}
	
	final Set<Pattern.PatternType> type;
	final String pattern;
	final boolean isRegexp;
	final boolean matchAll;

	public Pattern(String pattern, boolean isRegexp, Pattern.PatternType... type) {
		this.type = new HashSet<>(Arrays.asList(type));
		this.pattern = pattern;
		this.isRegexp = isRegexp;
		this.matchAll = false;
	}
	
	public Pattern() {
		this.pattern = "";
		this.isRegexp = false;
		this.type = Collections.emptySet();
		this.matchAll = true;
	}
	
	boolean match(String str) {
		if (matchAll) {
			return true;
		}
		if (isRegexp) {
			return str.matches(pattern);
		}
		return pattern.equals(str);
	}
	
	
	public boolean matchEntryPoint(IMethod method, Annotation entryPointAnnotation) {
		return matchAll || (type.contains(PatternType.SIGNATURE) && match(method.getSignature())) ||
				(type.contains(PatternType.ID) && match(EntryLocator.getEntryPointIdAttribute(entryPointAnnotation).orElse("")));
	}
	
	private boolean matchProgramAnnotation(SDGProgramPart part, String[] entries) {
		return matchAll || (type.contains(PatternType.SIGNATURE) && match(part.acceptVisitor(ProgramPartToString.getStandard(), null)))
				|| (type.contains(PatternType.ID) && (Arrays.stream(entries).anyMatch(this::match) || (entries.length == 0 && pattern.isEmpty())));
	}
	
	public boolean matchSink(SDGProgramPart part, Sink sink) {
		return matchProgramAnnotation(part, sink.tags());
	}
	
	public boolean matchSource(SDGProgramPart part, Source source) {
		return matchProgramAnnotation(part, source.tags());
	}
	
	public boolean matchDeclassification(SDGProgramPart part, Declassification declass) {
		return matchProgramAnnotation(part, declass.tags());
	}
}