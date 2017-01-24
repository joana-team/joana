package edu.kit.joana.wala.jodroid;

import java.io.IOException;
import java.util.List;

import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;

public class JoDroidTestRuns {
	public static void main(String[] args) throws SDGConstructionException, IOException, ClassHierarchyException {
		String androidLib = "lib/android.jar"; // please adapt!
		String appName = "com.madgag.agit_130400912"; // please adapt (apk file name without '.apk')!
		String appFile = appName + ".apk";
		int i = 0;
		AndroidEntryPointManager manager = new AndroidEntryPointManager();
		List<MethodReference> entryPoints = ListEntryPoints.collectEntryPoints(manager, appFile, androidLib);
		for (MethodReference m : entryPoints) {
			i++;
			System.out.println(String.format("Analyzing with entry point %d of %d: %s", i, entryPoints.size(), m.getSignature()));
			String sdgFileName = makeNeatFileName(appName, m);;
			try {
				JoDroidConstruction.buildAndroidSDGAndSave(manager, appFile, androidLib, m.getSignature(), sdgFileName);
			} catch (Throwable t) {
				System.out.println("An error occurred: " + t.getMessage());
				// Log to somewhere
			} finally {
				System.out.println();
			}
		}
	}
	
	private static String makeNeatFileName(String appName, MethodReference mRef) {
		return appName + "-" + mRef.getSignature().replaceAll("/", ".") + ".pdg";
	}

}
