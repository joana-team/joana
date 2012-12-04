/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * This class encapsulates the functionality to locate stubs. It is hopefully
 * the only place to change, when something in connection with stubs is changed.
 * 
 * @author Martin Mohr
 */
public class Stubs {

	private static class NoStubs extends Stubs {

		NoStubs() {
			super("NONE", null);
		}

		public String getPath() {
			return null;
		}

	}

	public static final Stubs NO_STUBS = new NoStubs();
	public static final Stubs JRE_14 = new Stubs("JRE_14", "jSDG-stubs-jre1.4.jar");
	public static final Stubs JRE_15 = new Stubs("JRE_15", "jSDG-stubs-jre1.5.jar");

	private static final String PROPERTIES = "project.properties";


	private String name;
	private String fileName;

	private Stubs(String name, String fileName) {
		this.name = name;
		this.fileName = fileName;
	}

	public String getName() {
		return name;
	}
	
	public String toString() {
		return name;
	}

	public String getPath() {
		if (locateableByClassLoader()) {
			return this.fileName;
		} else {
			String stubsBaseDir = determineStubsBasePath();
			return stubsBaseDir + "/" + this.fileName;
		}
	}
	
	private String determineStubsBasePath() {
		String joanaBaseDir;
		joanaBaseDir = System.getProperty("joana.base.dir");
		if (joanaBaseDir == null) {
			try {
				InputStream propertyStream = new FileInputStream(PROPERTIES);
				Properties p = new Properties();
				p.load(propertyStream);
				joanaBaseDir = p.getProperty("joana.base.dir");
			} catch (Throwable t) {
			}
		}

		if (joanaBaseDir == null) {
			throw new Error("Cannot locate property 'joana.base.dir'! Please provide a property 'joana.base.dir' to the jvm or a project.properties in the base dir of your eclipse project!");
		} else {
			File fBase = new File(joanaBaseDir + "/contrib/lib/stubs/");
			if (!fBase.exists()) {
				throw new Error("Invalid location " + joanaBaseDir + " for joana.base.dir!");
			} else {
				return fBase.getAbsolutePath();
//				System.out.println("stubs base dir: " + STUBS_BASEDIR);
//				System.out.println("found stubs: ");
//				for (File file : fBase.listFiles()) {
//					System.out.println(file);
//				}
			}
		}
	}
	
	private boolean locateableByClassLoader() {
		URL urlStubsLocation = getClass().getClassLoader().getResource(fileName);
		if (urlStubsLocation == null) {
			return false;
		} else {
			return true;
		}
	}

	public static Stubs[] getAvailableStubs() {
		return new Stubs[] { JRE_14, JRE_15, NO_STUBS };
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
		for (Stubs stubs : getAvailableStubs()) {
			if (stubs.getName().equals(strStubs)) {
				return stubs;
			}
		}

		return null;
	}
}
