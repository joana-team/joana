/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.create;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.gui.Activator;
import edu.kit.joana.deprecated.jsdg.util.Log;

public class SDGConfigBuilder {

	private final ICompilationUnit javafile;

	private SDGConfigBuilder(ICompilationUnit javafile) {
		this.javafile = javafile;
	}

	public enum Stubs { JOANA("lib/stubs.jar"), JRE_14("lib/jSDG-stubs-jre1.4.jar"),
		JRE_15_BETA("lib/jSDG-stubs-jre1.5.jar"), J2ME_20("lib/jSDG-stubs-j2me2.0.jar"),
		ANDROID_21("lib/jSDG-stubs-android-2.1-r2.jar");

		private final String file;

		private Stubs(String file) {
			this.file = file;
		}

		public String getFile() {
			return file;
		}

	};

	public static IPath getStubPath(ICompilationUnit javaFile, Stubs stub) {
		SDGConfigBuilder cb = new SDGConfigBuilder(javaFile);

		switch (stub) {
		case J2ME_20:
			return cb.getStubsJ2ME20();
		case JOANA:
			return cb.getStubsJoana();
		case JRE_14:
			return cb.getStubsJRE14();
		case JRE_15_BETA:
			return cb.getStubsJRE15();
		case ANDROID_21:
			return cb.getStubsAndroid21();
		}

		throw new IllegalStateException("" + stub);
	}

	public static SDGFactory.Config createConfig(ICompilationUnit javaFile) {
		SDGConfigBuilder cb = new SDGConfigBuilder(javaFile);

		SDGFactory.Config cfg = new SDGFactory.Config();

		cfg.mainClass = cb.getMainClassBCName();
		IPath out = cb.getOutputDir();
		if (!cb.checkExists(out)) {
			out.toFile().mkdir();
		}

		cfg.outputDir = cb.getOutputDir().toOSString();
		cfg.logFile = cb.getLogFile().toOSString();
		cfg.outputSDGfile = cb.getSDGFile().toOSString();
		try {
			if (cfg.useJoanaCompiler) {
				try {
					IProject project = javaFile.getPrimaryElement().getJavaProject().getProject();
					String newBuildPath = project.getWorkingLocation("joana.ifc.compiler") + File.separator + "build";
					cfg.classpath = newBuildPath;
				} catch (Exception e) {
					cfg.classpath = cb.getProjectClassPath().toOSString();
					Log.warn("Reading extended bytecode attributes went wrong...");
					Log.warn(e);
				}
			} else {
				cfg.classpath = cb.getProjectClassPath().toOSString();
			}
		} catch (JavaModelException e) {
			Activator.getDefault().showError(e, "Could not classpath of selected java project (JavaModelException)");
		}

		cb.checkAndCopy(cb.getNativesEmpty(), "lib/natives_empty.xml");
		cb.checkAndCopy(cb.getNativesWala(), "lib/natives_orig_wala.xml");
		cb.checkAndCopy(cb.getPrimordialModel(), "lib/primordial.jar.model");
		cb.checkAndCopy(cb.getStubsJoana(), Stubs.JOANA.getFile());
		cb.checkAndCopy(cb.getStubsJRE14(), Stubs.JRE_14.getFile());
		cb.checkAndCopy(cb.getStubsJRE15(), Stubs.JRE_15_BETA.getFile());
		cb.checkAndCopy(cb.getStubsJ2ME20(), Stubs.J2ME_20.getFile());
		cb.checkAndCopy(cb.getStubsAndroid21(), Stubs.ANDROID_21.getFile());

		cfg.nativesXML = cb.getNativesEmpty().toOSString();
		cfg.scopeData = new ArrayList<String>();
//		cfg.scopeData.add("Primordial,Java,stdlib,none");
		cfg.scopeData.add("Primordial,Java,jarFile," + cb.getStubsJRE14().toOSString());
		cfg.scopeData.add("Primordial,Java,jarFile," + cb.getPrimordialModel().toOSString());
		cfg.logLevel = Log.LogLevel.INFO;

		return cfg;
	}

	/**
	 * Tries to adjust the absolute paths in the configuration file to the current
	 * project location.
	 * @param cfg This configuration object is adjusted.
	 */
	public static void adjustConfigToLocalPaths(ICompilationUnit cu, Config cfg) {
		Config defaultCfg = createConfig(cu);
		if (!cfg.outputDir.equals(defaultCfg.outputDir)) {
			// We assume that there is nothing to adjust if the outputDir is equal.
			// This holds as long there has been no manual edit in the config file.
			cfg.outputSDGfile = defaultCfg.outputSDGfile;
			cfg.outputDir = defaultCfg.outputDir;
			cfg.scopeData = adjustStringList(cfg.scopeData, defaultCfg.scopeData);
			cfg.classpath = defaultCfg.classpath;
			cfg.exclusions = adjustStringList(cfg.exclusions, defaultCfg.exclusions);
		}
	}

	private static List<String> adjustStringList(List<String> origValues, List<String> defaultValues) {
		if (origValues == null) {
			return null;
		} else if (defaultValues == null) {
			return origValues;
		}

		List<String> adjusted = new LinkedList<String>();
		for (String orig : origValues) {
			String bestMatch = "";
			String bestMatchValue = null;

			for (String def : defaultValues) {
				String match = findMatchingEnding(orig, def);
				if (match.length() > bestMatch.length()) {
					bestMatch = match;
					bestMatchValue = def;
				}
			}

			if (bestMatch.length() > 0) {
				adjusted.add(bestMatchValue);
			} else {
				adjusted.add(orig);
			}
		}

		return adjusted;
	}

	private static String findMatchingEnding(String s1, String s2) {
		int match = 0;
		while (s1.regionMatches(s1.length() - (match + 1), s2, s2.length() - (match + 1), match + 1)) {
			match++;
		}

		return (match > 0 ? s1.substring(s1.length() - match) : "");
	}

	public static IPath getDefaultCfg(ICompilationUnit javaFile) {
		SDGConfigBuilder cb = new SDGConfigBuilder(javaFile);
		String name = cb.getMainClassName();
		IPath defaultCfg = cb.getOutputDir().append(name);
		defaultCfg = defaultCfg.addFileExtension("cfg");

		return defaultCfg;
	}

	/**
	 * Creates the bytecode name of the class corresponding to javafile
	 *
	 * @return
	 */
	private String getMainClassBCName() {
		String mainClass = getMainClassName();

		mainClass = mainClass.replace('.', '/');

		return 'L' + mainClass;
	}

	private String getMainClassName() {
		return getMainClassName(javafile);
	}

	public static String getMainClassName(ICompilationUnit javafile) {
        String path = "";
        IJavaElement elem = javafile;

        while (elem.getParent() != null && elem.getParent().getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
            elem = elem.getParent();
            path = elem.getElementName() + path;
        }

        String mainFile = javafile.getElementName().replaceAll(".java", "");

        if (path.equals("")) {
            return mainFile;
        } else {
            return path + "." + mainFile;
        }
	}

	private IPath getProjectClassPath() throws JavaModelException {
		IJavaProject project = javafile.getJavaProject();
//		IClasspathEntry[] entries = project.getResolvedClasspath(true);
		IPath path = project.getOutputLocation();
		IPath fullWorkspacePath = getFullWorkspacePath(project);

		IPath absolutePath = fullWorkspacePath.append(path);

		return absolutePath;
	}

	private final static IPath getFullWorkspacePath(IJavaProject project) {
		IPath fullWorkspacePath = null;

		try {
			IPath fullProjectPath = project.getCorrespondingResource().getLocation();
			fullWorkspacePath = fullProjectPath.removeLastSegments(1);
		} catch (JavaModelException jme) {
			Activator.getDefault().showError(jme, "Could not retrieve absolute path for project " + project);
		}

		return fullWorkspacePath;
	}

	private IPath getOutputDir() {
		IJavaProject project = javafile.getJavaProject();
		IPath workspacePath = getFullWorkspacePath(project);
		IPath path = project.getPath();
		IPath output = workspacePath.append(path);
		output = output.append("jSDG");

		return output;
	}

	private IPath getSDGFile()  {
		String mainClass = getMainClassName();
		/*
		IJavaProject project = javafile.getJavaProject();
		IPath path2 = getFullWorkspacePath(project);
		IPath path = project.getPath();
		IPath sdgFile = path2.append(path);
		*/
		IPath sdgFile = getOutputDir();
		sdgFile = sdgFile.append(mainClass);
		sdgFile = sdgFile.addFileExtension("pdg");

		return sdgFile;
	}

	private IPath getLogFile() {
		String mainClass = getMainClassName();
		IPath out = getOutputDir();
		out = out.append(mainClass);
		out = out.addFileExtension("log");

		return out;
	}

	private void checkAndCopy(IPath path, String name) {
		if (!checkExists(path)) {
			path.toFile().getParentFile().mkdirs();
			try {
				copyFromBundle(name, path);
			} catch (IOException e) {
				Activator.getDefault().showError(e, "Failed to copy " + name + " from plug-in bundle to " + path.toOSString());
			}
		}
	}

	private boolean checkExists(IPath path) {
		return path.toFile().exists();
	}

	private void copyFromBundle(String name, IPath toPath) throws IOException {
		Bundle bundle = edu.kit.joana.deprecated.jsdg.Activator.getDefault().getBundle();
		URL url = bundle.getEntry(name);
		BufferedInputStream bIn = new BufferedInputStream(url.openStream());
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(toPath.toFile()));
		byte[] buf = new byte[1024 * 10];
		while (bIn.available() > 0) {
			int read = bIn.read(buf);
			if (read > 0) {
				bOut.write(buf, 0, read);
			}
		}
		bIn.close();
		bOut.close();
	}

	private IPath getNativesEmpty() {
		IPath path = getOutputDir().append("lib");
		path = path.append("natives_empty");
		path = path.addFileExtension("xml");

		return path;
	}

	private IPath getNativesWala() {
		IPath path = getOutputDir().append("lib");
		path = path.append("natives_orig_wala");
		path = path.addFileExtension("xml");

		return path;
	}

	private IPath getPrimordialModel() {
		IPath path = getOutputDir().append("lib");
		path = path.append("primordial.jar");
		path = path.addFileExtension("model");

		return path;
	}

	private IPath getStubsJoana() {
		IPath path = getOutputDir().append("lib");
		path = path.append("stubs");
		path = path.addFileExtension("jar");

		return path;
	}

	private IPath getStubsJRE14() {
		IPath path = getOutputDir().append("lib");
		path = path.append("jSDG-stubs-jre1.4");
		path = path.addFileExtension("jar");

		return path;
	}

	private IPath getStubsJRE15() {
		IPath path = getOutputDir().append("lib");
		path = path.append("jSDG-stubs-jre1.5");
		path = path.addFileExtension("jar");

		return path;
	}

	private IPath getStubsJ2ME20() {
		IPath path = getOutputDir().append("lib");
		path = path.append("jSDG-stubs-j2me2.0");
		path = path.addFileExtension("jar");

		return path;
	}

	private IPath getStubsAndroid21() {
		IPath path = getOutputDir().append("lib");
		path = path.append("jSDG-stubs-android-2.1-r2");
		path = path.addFileExtension("jar");

		return path;
	}


}
