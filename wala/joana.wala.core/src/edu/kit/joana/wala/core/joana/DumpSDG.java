/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.joana;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.ibm.wala.util.CancelException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;

/**
 * Writes current SDGBuilder status to a Joana SDG file.
 * 
 * @author Juergen Graf <juergen.graf@gmail.co>
 */
public final class DumpSDG {

	private DumpSDG() {}
	
	public static void dumpIfEnabled(final SDGBuilder builder, final String loggerId) throws CancelException {
		final Logger log = Log.getLogger(loggerId);
		if (log.isEnabled()) {
			final SDG sdg = JoanaConverter.convert(builder, NullProgressMonitor.INSTANCE);
			final String fileName = sdg.getName() + loggerId + ".pdg";
			try {
				log.outln("saving " + fileName);
				SDGSerializer.toPDGFormat(sdg, new FileOutputStream(fileName));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
}
