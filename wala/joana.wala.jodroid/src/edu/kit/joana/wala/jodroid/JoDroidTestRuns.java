package edu.kit.joana.wala.jodroid;

import java.io.IOException;
import java.util.List;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;

public class JoDroidTestRuns {
	
	public static void main(String[] args) throws SDGConstructionException, IOException, ClassHierarchyException {
		String androidLib = "lib/android.jar";
		String appFile = "k9-4.001-release.apk";
		int i = 0;
		List<MethodReference> entryPoints = ListEntryPoints.collectEntryPoints(appFile, androidLib);
		for (MethodReference m : entryPoints) {
			i++;
			System.out.println(String.format("Analyzing with entry point %d of %d: %s", i, entryPoints.size(), m.getSignature()));
			String sdgFileName = "k9-"+m.getDeclaringClass().getName().toString().replace('/','.')+"-"+m.getName()+".pdg";
			JoDroidConstruction.buildAndroidSDGAndSave("k9-4.001-release.apk", androidLib, "com.fsck.k9.activity.Accounts.onCreate(Landroid/os/Bundle;)V", sdgFileName);
		}
		//JoDroidConstruction.buildAndroidSDGAndSave("BarcodeScanner4.31.apk", androidLib, "com.google.zxing.client.android.CaptureActivity.onCreate(Landroid/os/Bundle;)V", "barcode.pdg");
		
		
//		for (MethodReference m : ListEntryPoints.collectEntryPoints("Facebook_3.5.apk", androidLib)) {
//			System.out.println(m.getSignature());
//		}
		//JoDroidConstruction.buildAndroidSDGAndSave("Facebook_3.5.apk", androidLib, "com.facebook.nodex.failuremessage.NodexFailureActivity.onCreate(Landroid/os/Bundle;)V", "facebook.pdg");
		//JoDroidConstruction.buildAndroidSDGAndSave("Facebook_3.5.apk", androidLib, "android.support.v4.app.FragmentActivity.onStart()V", "facebook.pdg");
		//JoDroidConstruction.buildAndroidSDGAndSave("Facebook_3.5.apk", androidLib, "android.support.v4.app.FragmentActivity.onCreate(Landroid/os/Bundle;)V", "facebook.pdg");
		//JoDroidConstruction.buildAndroidSDGAndSave("Facebook_3.5.apk", androidLib, "com.facebook.base.app.DelegatingApplication.onCreate()V", "facebook.pdg");
		//JoDroidConstruction.buildAndroidSDGAndSave("Facebook_3.5.apk", androidLib, "com.facebook.katana.app.FacebookApplication.onCreate()V", "facebook.pdg");
		
	}

}
