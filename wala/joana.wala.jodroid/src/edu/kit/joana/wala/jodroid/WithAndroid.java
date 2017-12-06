package edu.kit.joana.wala.jodroid;

import java.io.IOException;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilderConfigurator;
import edu.kit.joana.wala.jodroid.AndroidAnalysis;
import edu.kit.joana.wala.jodroid.io.AppSpec;

public class WithAndroid extends SDGBuilderConfigurator<WithAndroid> {

	@Override
	public WithAndroid thisActually() {
		return this;
	}
	public WithAndroid configureForAndroidApp(AppSpec appSpec) throws ClassHierarchyException, IOException, CancelException {
		SDGBuilder.SDGBuilderConfig scfg2 = new AndroidAnalysis().makeSDGBuilderConfig(appSpec, scope, cha, null, true, false);
		rwd.set("entry", scfg2.entry);
		rwd.set("cache", scfg2.cache);
		rwd.set("additionalContextSelector", scfg2.additionalContextSelector);
		rwd.set("additionalContextInterpreter", scfg2.additionalContextInterpreter);
		return thisActually();
	}
}
