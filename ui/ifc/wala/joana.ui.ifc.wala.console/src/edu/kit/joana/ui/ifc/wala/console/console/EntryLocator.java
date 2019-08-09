/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.EntryPoints;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.util.NullPrintStream;
import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

import static edu.kit.joana.util.Pair.pair;

/**
 * Performs a search for possible main methods and maintains the list of search
 * results.
 *
 * @author Martin Mohr, Johannes Bechberger
 *
 */
public class EntryLocator {
	
	/** the list of search results */
	private final List<JavaMethodSignature> possibleEntries = new ArrayList<JavaMethodSignature>();

	/** the entry method which has been selected */
	private JavaMethodSignature activeEntry;

	/**
	 * Returns whether the last search for an entry method yielded any results.
	 *
	 * @return whether the last search for an entry method yielded any results
	 */
	public boolean foundPossibleEntries() {
		return !possibleEntries.isEmpty();
	}

	/**
	 * Returns the entry method which has been selected.
	 *
	 * @return the entry method which has been selected
	 */
	public JavaMethodSignature getActiveEntry() {
		return activeEntry;
	}

	/**
	 * Performs the search and updates the result list. After a successful
	 * search the result list contains all found entry methods and no entry
	 * method is selected. If no entry method is found, result list and selected
	 * entry method remain untouched.
	 *
	 * @param classPath
	 *            location of the classes in which the entry methods are to be
	 *            searched. Can be a directory or a jar.
	 * @return whether the search was successful
	 */
	public boolean doSearch(String classPath, IFCConsoleOutput out) {
		final SDGBuildPreparation.Config cfg = new SDGBuildPreparation.Config("Search main <unused>", "<unused>",
				classPath, true, FieldPropagation.FLAT);
		List<JavaMethodSignature> newEntries = new ArrayList<JavaMethodSignature>();
		try {
			List<String> res = SDGBuildPreparation.searchMainMethods(out.getPrintStream(), cfg);
			for (String sig : res) {
				newEntries.add(JavaMethodSignature.fromString(sig));
			}
		} catch (ClassHierarchyException e) {
			out.error("Error while analyzing class structure!");
			return false;
		} catch (IOException e) {
			out.error("I/O error while searching entry methods!");
			return false;
		}

		if (newEntries.isEmpty()) {
			return false;
		}

		possibleEntries.clear();
		possibleEntries.addAll(newEntries);
		Collections.sort(possibleEntries, new Comparator<JavaMethodSignature>() {

			@Override
			public int compare(JavaMethodSignature arg0,
					JavaMethodSignature arg1) {
				return arg0.toHRString().compareTo(arg1.toHRString());
			}

		});
		unselectEntry();
		return true;
	}

	/**
	 * Performs the search and updates the result list. After a successful
	 * search the result list contains all found entry methods and no entry
	 * method is selected. If no entry method is found, result list and selected
	 * entry method remain untouched.
	 * 
	 * Searches for static methods that have the {@link EntryPoint} annotation
	 * 
	 * @param classPath
	 *            location of the classes in which the entry methods are to be
	 *            searched. Can be a directory or a jar.
	 * @param pattern pattern
	 * @return empty or a list of annotations with their methods, methods can appear only once in the list
	 */
	public Optional<List<Pair<IMethod, Annotation>>> doSearchForEntryPointAnnotated(String classPath, IFCConsoleOutput out, Pattern pattern){
		final SDGBuildPreparation.Config cfg = new SDGBuildPreparation.Config("Search entry <unused>", "<unused>",
				classPath, true, FieldPropagation.FLAT);
		List<JavaMethodSignature> newEntries = new ArrayList<JavaMethodSignature>();
		List<Pair<IMethod, Annotation>> entries = new ArrayList<>();
		try {
			out.info("Searching for main methods in '" + cfg.classpath + "'...");
			ClassHierarchy cha = SDGBuildPreparation.computeClassHierarchy(new NullPrintStream(), cfg);	
			String entryPointName = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(EntryPoint.class.getCanonicalName()).toBCString(false)).toString();
			String entryPointsName = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(EntryPoints.class.getCanonicalName()).toBCString(false)).toString();
			for (final IClass cls : cha) {
				if (!cls.isInterface() && !cls.isAbstract() && cls.getClassLoader().getName().equals(AnalysisScope.APPLICATION)) {
					for (final IMethod m : cls.getDeclaredMethods()) {
						//if (m.isStatic()) {
						out.info("Look at method '" + m.getSignature() + "'");
						// TODO: use @EntryPoints
						List<Annotation> anns = new ArrayList<>();
						m.getAnnotations().stream()
							.filter(a -> a.getType().getName()
									.toString().equals(entryPointName))
							.forEach(anns::add);
						m.getAnnotations().stream()
							.filter(a -> a.getType().getName().toString()
									.equals(entryPointsName))
							.forEach(a -> {
								if (a.getNamedArguments().containsKey("value")){
									Object[] entryAnnotations = ((ArrayElementValue)a.getNamedArguments().get("value")).vals;
									Arrays.stream(entryAnnotations).forEach(ann -> {
										AnnotationAttribute elem = (AnnotationAttribute)ann;
										anns.add(Annotation.makeWithNamed(TypeReference.findOrCreate(ClassLoaderReference.Application, elem.type),
												elem.elementValues));
									});
								}
							});
						if (anns.size() > 0) {
							for (Annotation ann : anns){
								if (pattern.matchEntryPoint(m, ann)) {
									out.info("\tfound '" + m.getSignature() + "': " + ann);
									entries.add(pair(m, ann));
									newEntries.add(JavaMethodSignature.fromString(m.getSignature()));
								}
							}
						}
					}
				}
			}
		} catch (ClassHierarchyException e) {
			out.error("Error while analyzing class structure!");
			return Optional.empty();
		} catch (IOException e) {
			out.error("I/O error while searching entry methods!");
			return Optional.empty();
		}

		if (newEntries.isEmpty()) {
			return Optional.empty();
		}

		possibleEntries.clear();
		possibleEntries.addAll(newEntries);
		Collections.sort(possibleEntries, new Comparator<JavaMethodSignature>() {

			@Override
			public int compare(JavaMethodSignature arg0,
					JavaMethodSignature arg1) {
				return arg0.toHRString().compareTo(arg1.toHRString());
			}

		});
		unselectEntry();
		return Optional.of(entries);
	}
	
	/**
	 * Prints the results of the last entry search to the given print stream
	 *
	 * @param out
	 *            print stream into which the results of the last entry search
	 *            are to be printed
	 */
	public void displayLastEntrySearchResults(IFCConsoleOutput out) {
		if (!foundPossibleEntries()) {
			for (int i = 0; i < possibleEntries.size(); i++) {
				out.logln("[" + i + "] "
						+ possibleEntries.get(i).toHRString());
			}
		} else {
			out.logln("No search results.");
		}
	}

	/**
	 * Returns whether the given index is valid, i.e. corresponds to a possible
	 * entry method.
	 *
	 * @param i
	 *            index to be checked
	 * @return whether the given index is within the bounds of the list of
	 *         possible entry methods
	 */
	public boolean entryIndexValid(int i) {
		return i >= 0 && i < getNumberOfFoundEntries();
	}

	/**
	 * Selects the entry method with index i. If i is not valid, does nothing.
	 *
	 * @param i
	 *            index of entry method to be selected
	 */
	public void selectEntry(int i) {
		if (entryIndexValid(i)) {
			activeEntry = possibleEntries.get(i);
		}
	}

	public void selectEntry(JavaMethodSignature entry) {
		if (possibleEntries.contains(entry))
			selectEntry(possibleEntries.indexOf(entry));
	}

	/**
	 * Unselects the active entry.
	 */
	public void unselectEntry() {
		activeEntry = null;
	}

	/**
	 * Returns whether an entry method has been selected.
	 *
	 * @return whether an entry method has been selected
	 */
	public boolean entrySelected() {
		return activeEntry != null;
	}

	/**
	 * Returns the number of search results of the last successful search.
	 *
	 * @return the number of search results of the last successful search
	 */
	public int getNumberOfFoundEntries() {
		return possibleEntries.size();
	}

	public List<JavaMethodSignature> getLastSearchResults() {
		List<JavaMethodSignature> ret = new ArrayList<JavaMethodSignature>();
		ret.addAll(possibleEntries);
		return ret;
	}
	
	public JavaMethodSignature getEntry(int arg0) {
		return possibleEntries.get(arg0);
	}

	public int getIndex(JavaMethodSignature entry) {
		for (int i = 0; i < getNumberOfFoundEntries(); i++) {
			if (getEntry(i).equals(entry)) {
				return i;
			}
		}

		return -1;
	}

	public static Optional<String> getEntryPointIdAttribute(Annotation entryPoint){
		String id = ((ConstantElementValue)entryPoint.getNamedArguments().getOrDefault("tag", new ConstantElementValue(""))).val.toString();
		if (id.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(id);
	}
}
