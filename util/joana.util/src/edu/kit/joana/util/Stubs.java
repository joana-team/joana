/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import exceptionalist.MethodInfo;
import joana.contrib.lib.Contrib;
import com.ibm.wala.classLoader.IMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

;

/**
 * This class encapsulates the functionality to locate stubs. It is hopefully
 * the only place to change, when something in connection with stubs is changed.
 * 
 * @author Martin Mohr
 */

public enum Stubs {
	NO_STUBS         ("NONE",              null,                               JoanaConstants.nativesWala,  Contrib.class.getClassLoader()),
	JRE_14_INCOMPLETE("JRE_14_INCOMPLETE", "stubs/jSDG-stubs-jre1.4.jar",      JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	JRE_15_INCOMPLETE("JRE_15_INCOMPLETE", "stubs/jSDG-stubs-jre1.5.jar",      JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	JRE_15           ("JRE_15",            "stubs/jdk-1.5-with-stubs.jar",     JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	JRE_16           ("JRE_16",            "stubs/jdk-1.6-with-stubs.jar",     JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	JRE_17           ("JRE_17",            "stubs/jdk-1.7-with-stubs.jar",     JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	JAVACARD         ("JAVACARD",          "stubs/jSDG-stubs-javacard.jar"
	                                    + ":stubs/api.jar",                    JoanaConstants.nativesEmpty, Contrib.class.getClassLoader()),
	J2ME             ("J2ME",              "stubs/jSDG-stubs-j2me2.0.jar"
	                                    + ":stubs/jsr184.jar"
			                            + ":stubs/mmapi.jar",                  JoanaConstants.nativesEmpty, Contrib.class.getClassLoader());

	private final String name;
	private final String[] files;
	private final String nativeSpecFile;
	private final ClassLoader nativeSpecClassLoader;
	private final String exceptionalistMethodInfoFile = "jdk7_wo_exceptions.json";
	private Map<String, Map<String, MethodInfo>> exceptionalistMethodInfoCached = null;

	public static class ExceptionalistConfig {
		public final boolean enable;

		public static final ExceptionalistConfig DISABLE = new ExceptionalistConfig(false);
		public static final ExceptionalistConfig ENABLE = new ExceptionalistConfig(true);

		public ExceptionalistConfig(boolean enable) {
			this.enable = enable;
		}
	}

	private Stubs(final String name, final String fileName, final String nativeSpecFile, final ClassLoader nativeSpecClassLoader) {
		this.name = name;
		this.files = (fileName == null ? new String[0] : fileName.split(":"));
		this.nativeSpecFile = nativeSpecFile;
		this.nativeSpecClassLoader = nativeSpecClassLoader;
	}

	public String getName() {
		return name;
	}
	
	public String getNativeSpecFile() {
		return nativeSpecFile;
	}

	public ClassLoader getNativeSpecClassLoader() {
		return nativeSpecClassLoader;
	}

	public String toString() {
		return name;
	}
	
	public String[] getPaths() {
		String stubsBaseDir = null;
		final String[] paths = new String[files.length];
		
		for (int i = 0; i < files.length; i++) {
			final String name = files[i];
		
			if (locateableByClassLoader(name)) {
				paths[i] = name;
			} else {
				if (stubsBaseDir == null) { stubsBaseDir = determineStubsBasePath(); }
				paths[i] = stubsBaseDir + "/" + name;
			}
		}
		return paths;
	}

	private String determineStubsBasePath() {
		String joanaBaseDir;
		joanaBaseDir = System.getProperty("joana.base.dir");
		if (joanaBaseDir == null) {
			try {
				InputStream propertyStream = new FileInputStream(JoanaConstants.PROPERTIES);
				Properties p = new Properties();
				p.load(propertyStream);
				joanaBaseDir = p.getProperty("joana.base.dir");
			} catch (Throwable t) {
			}
		}

		if (joanaBaseDir == null) {
			throw new Error("Cannot locate property 'joana.base.dir'! Please provide a property 'joana.base.dir'"
				+ " to the jvm or a project.properties in the base dir of your eclipse project!");
		}

		File fBase = new File(joanaBaseDir + "/contrib/lib/");
		if (!fBase.exists()) {
			throw new Error("Invalid location " + joanaBaseDir + " for joana.base.dir!");
		}

		return fBase.getAbsolutePath();
	}

	private boolean locateableByClassLoader(final String name) {
		final URL urlStubsLocation = Contrib.class.getClassLoader().getResource(name);
		return (urlStubsLocation != null);
	}

	private InputStream getExceptionalistFile() throws IOException {
		String path = "stubs/" + exceptionalistMethodInfoFile;
		if (locateableByClassLoader(path)) {
			URL url = Contrib.class.getClassLoader().getResource(path);
			assert url != null;
			final URLConnection con = url.openConnection();
			return con.getInputStream();
		} else {
			return Files.newInputStream(Paths.get(determineStubsBasePath() + "/" + path));
		}
	}

	/**
	 * Loads a file that contains methods that should not throw an error (besides null related errors if a parameter or the
	 * owning object is null).
	 * @return class name → methods
	 */
	public Map<String, Map<String, MethodInfo>> getExceptionalistMethodInfo() throws IOException {
		return exceptionalist.MainKt.loadMethodInfos(getExceptionalistFile());
	}

	/**
	 * Loads a file that contains methods that should not throw an error (besides null related errors if a parameter or the
	 * owning object is null).
	 *
	 * This method caches the method infos.
	 *
	 * @return class name → methods
	 */
	public Map<String, Map<String, MethodInfo>> getExceptionalistMethodInfoCached() throws IOException {
		if (exceptionalistMethodInfoCached == null) {
			exceptionalistMethodInfoCached = getExceptionalistMethodInfo();
		}
		return exceptionalistMethodInfoCached;
	}

	public Optional<MethodInfo> getExceptionalistInfo(IMethod method) {
		try {
			return Optional.ofNullable(getExceptionalistMethodInfoCached()
					.getOrDefault(TypeNameUtils.toJavaClassName(method.getDeclaringClass()),
							Collections.emptyMap()).getOrDefault(TypeNameUtils.toJavaSignatureWOReturn(method), null));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * Given a string, returns the stubs object whose name equals the string. If
	 * none of the available stubs objects matches, {@code null} is returned.
	 * 
	 * @param strStubs
	 *            string to parse stubs object from
	 * @return the stubs object whose name matches the given string, if there is
	 *         any, {@code null} otherwise
	 */
	public static Stubs fromString(String strStubs) {
		for (Stubs stubs : values()) {
			if (stubs.getName().equals(strStubs)) {
				return stubs;
			}
		}

		return null;
	}

	public static Stubs getStubForStr(String name) {
		for (Stubs s : Stubs.values()) {
			if (s.name.equals(name)) {
				return s;
			}
			
		}
		
		throw new IllegalArgumentException("Stub with name '" + name + "' not found.");
	}
}
