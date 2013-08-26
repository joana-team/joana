package joana.wala.jodroid;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

public class ListEntryPoints {

	public static final TypeReference TR_ACTIVITY = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
			"Landroid/app/Activity");
	public static final TypeReference TR_APPLICATION = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
			"Landroid/app/Application");


	public static void main(String[] args) throws ClassHierarchyException, IOException {
		String androidLib = "lib/android-2.3.6_r1.jar"; // change this to the
														// android lib of your
														// choice!
		String classPath = "Facebook_3.5.apk"; // change this to the app of
													// your choice!
		
		
		List<IMethod> promisingMethods = collectEntryPoints(classPath, androidLib);
		System.out.println("You may want to choose one of the following methods as Entry points: ");
		for (IMethod prom : promisingMethods) {
			System.out.println(prom.getSignature());
		}
	}
	
	public static List<IMethod> collectEntryPoints(String classPath, String androidLib) throws ClassHierarchyException, IOException {
		IClassHierarchy cha = JoDroidConstruction.computeCH(classPath, androidLib);
		IClass cApp = resolve(cha, TR_APPLICATION);
		IClass cAct = resolve(cha, TR_ACTIVITY);
		if (cApp == null) {
			throw new IllegalArgumentException("Could not find class 'android.app.Application'! Check path of Android lib!");
		}
		if (cAct == null) {
			throw new IllegalArgumentException("Could not find class 'android.app.Activity'! Check path of Android lib!");
		}
		List<IClass> entryClasses = collectAllSubclasses(cha, resolve(cha, TR_APPLICATION));
		entryClasses.addAll(collectAllSubclasses(cha, resolve(cha, TR_ACTIVITY)));

		List<IMethod> promisingMethods = collectPromisingMethods(entryClasses, cha);
		
		return promisingMethods;
	}

	private static IClass resolve(IClassHierarchy cha, TypeReference tr) {
		return cha.lookupClass(tr);
	}

	public static List<IClass> collectAllSubclasses(IClassHierarchy cha, IClass baseClass) throws ClassHierarchyException,
			IOException {
		List<IClass> ret = new LinkedList<IClass>();
		for (IClass cl : cha) {
			if (cl.getClassLoader().getReference().equals(ClassLoaderReference.Application)
					&& cha.isSubclassOf(cl, baseClass)) {
				ret.add(cl);
			}
		}
		return ret;
	}
	
	private static List<IMethod> collectPromisingMethods(Collection<IClass> classes, IClassHierarchy cha) {
		List<IMethod> ret = new LinkedList<IMethod>();
		for (IClass eClass : classes) {
			ret.addAll(collectPromisingMethods(eClass, cha));
		}

		return ret;
	}
	
	public static List<IMethod> collectPromisingMethods(IClass clazz, IClassHierarchy cha) {
		List<IMethod> ret = new LinkedList<IMethod>();
		MethodReference onStartRef = MethodReference.findOrCreate(clazz.getReference(), Selector.make("onStart()V"));
		MethodReference onActCreateRef = MethodReference.findOrCreate(clazz.getReference(), Selector.make("onCreate(Landroid/os/Bundle;)V"));
		MethodReference onAppCreateRef = MethodReference.findOrCreate(clazz.getReference(), Selector.make("onCreate()V"));
		
		IMethod onStart = cha.resolveMethod(onStartRef);
		IMethod onActCreate = cha.resolveMethod(onActCreateRef);
		IMethod onAppCreate = cha.resolveMethod(onAppCreateRef);
		
		if (clazz.getDeclaredMethods().contains(onStart)) {
			ret.add(onStart);
		}
		
		if (clazz.getDeclaredMethods().contains(onActCreate)) {
			ret.add(onActCreate);
		}
		
		if (clazz.getDeclaredMethods().contains(onAppCreate)) {
			ret.add(onAppCreate);
		}
		
		return ret;
	}
}
