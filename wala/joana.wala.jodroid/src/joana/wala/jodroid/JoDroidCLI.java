package joana.wala.jodroid;

import java.io.IOException;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

public class JoDroidCLI {

	private static final String JODROID_INVOCATION_NAME = "<PROGRAM>";
	private static final String PARAMETERS = "<classpath> <android lib> <entry method> <sdgfile>";

	private static void printUsage() {
		System.out.println("Usage: " + JODROID_INVOCATION_NAME + " " + PARAMETERS + " where ");
		System.out.println("<classpath>:    specifies .apk file to analyze.");
		System.out
				.println("<android lib>:  specifies the path to the .jar or .dex which contains the android library.");
		System.out
				.println("<entry method>: specifies the method at which the environment enters the app under analysis. Note that the name of the method must be fully qualified");
		System.out.println("                and its signature has to be given in bytecode notation.");
		System.out.println("                Examples: ");
		System.out.println("                \tcom.foo.bar.AClass.main([Ljava/lang/String;)V");
		System.out.println("                \tcom.foo.bar.AClass.addTwoInts(II)I");
		System.out.println("                \tcom.foo.bar.AClass$AnInnerClass.isTroodles()Z");
		System.out
				.println("<sdgfile>:      specifies the path to the file in which the resulting SDG is to be written");

	}

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException,
			CancelException, UnsoundGraphException {
		if (args.length != 4) {
			printUsage();
		} else {
			String classPath = args[0];
			String androidLib = args[1];
			String entryMethod = args[2];
			String sdgFile = args[3];
			try {
				JoDroidConstruction.buildAndroidSDGAndSave(classPath, androidLib, entryMethod, sdgFile);
			} catch (SDGConstructionException m) {
				System.out.println("Entry method not found: " + m.getCause());
				return;
			}
		}
	}
	


}
