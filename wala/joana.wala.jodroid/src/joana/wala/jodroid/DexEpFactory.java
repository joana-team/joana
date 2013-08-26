package joana.wala.jodroid;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import edu.kit.joana.wala.core.EntryPointFactory;

/**
 * Entry point factory for Dex-Entry point. Used for the analysis of Android bytecode.
 * @author Martin Mohr
 *
 */
public class DexEpFactory implements EntryPointFactory {
	
	public static final EntryPointFactory INSTANCE = new DexEpFactory();
	
	/** Avoid instantiation from outside */
	private DexEpFactory() {};
	
	@Override
	public Entrypoint make(IMethod m, IClassHierarchy cha) {
		return new DexEntryPoint(m, cha);
	}

}
