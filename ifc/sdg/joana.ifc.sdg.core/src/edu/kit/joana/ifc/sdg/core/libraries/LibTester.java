/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.libraries;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;


public class LibTester {
    public static void main(String[] args) throws Exception {
        SDG libSDG = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.LibraryTest.pdg", new SecurityNodeFactory());

        /* TEST THE STUFF */
        // create some declassification nodes
        SecurityNode s88 = (SecurityNode) libSDG.getNode(88);
        SecurityNode s89 = (SecurityNode) libSDG.getNode(89);

        s88.setProvided(SecurityNode.ABSTRACT);
        s88.setRequired(SecurityNode.ABSTRACT);
        s89.setProvided(SecurityNode.ABSTRACT);
        s89.setRequired(SecurityNode.ABSTRACT);

//        SecurityNode s156 = (SecurityNode) libSDG.getNode(156);
//        SecurityNode s155 = (SecurityNode) libSDG.getNode(155);
//        s156.setProvided(SecurityNode.ABSTRACT);
//        s156.setRequired(SecurityNode.ABSTRACT);
//        s155.setProvided(SecurityNode.ABSTRACT);
//        s155.setRequired(SecurityNode.ABSTRACT);


        // compute the IFC conditions
        LibraryIFC libIFC = new LibraryIFC(libSDG);
        SecurityNode libEntry = (SecurityNode) libSDG.getNode(87);
        LibraryPropagationRules pre = libIFC.computeConditions(libEntry);
        System.out.println(pre);


        /* USE MODULAR IFC */
        SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.LibraryTest.pdg", new SecurityNodeFactory());

        // annotate source and sink nodes
        SecurityNode s79 = (SecurityNode) g.getNode(79);
        s79.setProvided("restricted");
        SecurityNode s26 = (SecurityNode) g.getNode(26);
        s26.setProvided("confidential");

        // provide concrete levels for the declassifications
//        s156.setProvided("public");
//        s156.setRequired("secure");
//        s155.setProvided("public");
//        s155.setRequired("secure");
        s88.setProvided("public");
        s88.setRequired("secure");
        s89.setProvided("public");
        s89.setRequired("secure");

        // initialize IFC algorithm
        IFC ifc = new IFC(g, lattice());
        ifc.addPrecondition(libEntry.getProc(), pre);

        // run analysis
        Collection<ClassifiedViolation> vios = ifc.check();
        System.out.println(vios);

    }

    private static IStaticLattice<String> lattice() throws Exception {
        File latticeFile = new File("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/diamond.lat");
        IEditableLattice<String> el = LatticeUtil.loadLattice(new FileInputStream(latticeFile));
        IStaticLattice<String> sl = LatticeUtil.compileBitsetLattice(el);
        return sl;
    }
}
