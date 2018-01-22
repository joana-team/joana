/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.deprecated.jsdg.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;

/**
 * @author giffhorn
 *
 */
public class ModRefOutput {
    @SuppressWarnings("unchecked")
	public static Map<Integer, Set<Integer>>[] createModRefMaps(SDG sdg, IProgressMonitor progress,
            HashMap<AbstractPDGNode, edu.kit.joana.ifc.sdg.graph.SDGNode> nodeMap)
    throws CancelException {
        progress.beginTask("Creating Mod, Ref and Pto Maps", -1);

        /* FRICKEL */
        PointerKeyMap pkm = new PointerKeyMap();
        InstanceKeyMap ikm = new InstanceKeyMap();
        Map<Integer, Set<Integer>> mod = new HashMap<Integer, Set<Integer>>(); // SDGNode -> PointerKey
        Map<Integer, Set<Integer>> ref = new HashMap<Integer, Set<Integer>>(); // SDGNode -> PointerKey
        Map<Integer, Set<Integer>> pto = new HashMap<Integer, Set<Integer>>(); // PointerKey -> InstanceKey
        Map<Integer, Set<Integer>> use = new HashMap<Integer, Set<Integer>>(); // SDGNode -> local variables
        Map<Integer, Set<Integer>> def = new HashMap<Integer, Set<Integer>>(); // SDGNode -> local variables

        PointerAnalysis<InstanceKey> pta = sdg.getPointerAnalysis();
        AnalysisScope scope = sdg.getAnalysisScope();

        ModRef<InstanceKey> modRef = ModRef.make();
        ExtendedHeapModel eModel = new DelegatingExtendedHeapModel(pta.getHeapModel());
        HeapExclusions hExcl = (scope.getExclusions() == null ? null : new HeapExclusions(scope.getExclusions()));
        for (PDG pdg : sdg.getAllContainedPDGs()) {
            CGNode n = pdg.getCallGraphNode();
            for (AbstractPDGNode node : pdg) {
                edu.kit.joana.ifc.sdg.graph.SDGNode sdgNode = nodeMap.get(node);

                SSAInstruction instr = pdg.getInstructionForNode(node);
                if (instr != null) {
                    Set<PointerKey> mods = modRef.getMod(n, eModel, pta, instr, hExcl);
                    Set<Integer> imods = getInts(mods, pkm);
                    update(mod, sdgNode, imods);

                    Set<PointerKey> refs = modRef.getRef(n, eModel, pta, instr, hExcl);
                    Set<Integer> irefs = getInts(refs, pkm);
                    update(ref, sdgNode, irefs);

                    for (PointerKey pk : mods) {
                        if (pto.get(pkm.get(pk)) == null) {
                            pto.put(pkm.get(pk), getInts(pta.getPointsToSet(pk), ikm));
                        }
                    }

                    for (PointerKey pk : refs) {
                        if (pto.get(pkm.get(pk)) == null) {
                            pto.put(pkm.get(pk), getInts(pta.getPointsToSet(pk), ikm));
                        }
                    }

                    Set<Integer> uses = new HashSet<Integer>();
                    Set<Integer> defs = new HashSet<Integer>();

                    for (int i = 0; i < instr.getNumberOfDefs(); i++) {
                        defs.add(instr.getDef(i));
                    }

                    for (int i = 0; i < instr.getNumberOfUses(); i++) {
                        uses.add(instr.getUse(i));
                    }

                    update(use, sdgNode, uses);
                    update(def, sdgNode, defs);
                }
            }
        }

        progress.done();

        return new Map[] {mod, ref, pto, use, def};
    }

    private static void update(Map<Integer, Set<Integer>> map, edu.kit.joana.ifc.sdg.graph.SDGNode n, Set<Integer> ints) {
    	Set<Integer> old = map.get(n.getId());

    	if (old == null) {
    		old = ints;
    	} else {
    		old.addAll(ints);
    	}

    	map.put(n.getId(), old);
    }

    private static Set<Integer> getInts(Set<PointerKey> pks, PointerKeyMap pkm) {
        HashSet<Integer> set = new HashSet<Integer>();
        for (PointerKey pk : pks) {
            set.add(pkm.get(pk));
        }
        return set;
    }

    private static Set<Integer> getInts(OrdinalSet<InstanceKey> pks, InstanceKeyMap pkm) {
        HashSet<Integer> set = new HashSet<Integer>();
        for (InstanceKey pk : pks) {
            set.add(pkm.get(pk));
        }
        return set;
    }


    public static void saveMaps(String baseFile,
            Map<Integer, Set<Integer>> mod,
            Map<Integer, Set<Integer>> ref,
            Map<Integer, Set<Integer>> pto,
            Map<Integer, Set<Integer>> use,
            Map<Integer, Set<Integer>> def)
    throws IOException {
        File fMod = new File(baseFile+"_mod");
        File fRef = new File(baseFile+"_ref");
        File fPto = new File(baseFile+"_pto");
        File fUse = new File(baseFile+"_use");
        File fDef = new File(baseFile+"_def");
        write(mod, fMod);
        write(ref, fRef);
        write(pto, fPto);
        write(use, fUse);
        write(def, fDef);
    }

    private static void write(Map<Integer, Set<Integer>> map, File f) throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(map);
        oos.flush();
        oos.close();
    }

    @SuppressWarnings("unchecked")
	public static Map<Integer, Set<Integer>>[] loadMaps(String baseFile)
    throws IOException, ClassNotFoundException {
        Map<Integer, Set<Integer>>[] maps = new HashMap[5];
        File fMod = new File(baseFile+"_mod");
        File fRef = new File(baseFile+"_ref");
        File fPto = new File(baseFile+"_pto");
        File fUse = new File(baseFile+"_use");
        File fDef = new File(baseFile+"_def");

        maps[0] = load(fMod);
        maps[1] = load(fRef);
        maps[2] = load(fPto);
        maps[3] = load(fUse);
        maps[4] = load(fDef);

        return maps;
    }

    @SuppressWarnings("unchecked")
	private static Map<Integer, Set<Integer>> load(File f) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        Map<Integer, Set<Integer>> map = (Map<Integer, Set<Integer>>) ois.readObject();
        ois.close();
        return map;
    }

    static class PointerKeyMap {
        HashMap<PointerKey, Integer> map = new HashMap<PointerKey, Integer>();
        int i = 0;

        int get(PointerKey pk) {
            Integer x = map.get(pk);
            if (x == null) {
                i++;
                map.put(pk, i);
                return i;

            } else {
                return x.intValue();
            }
        }
    }

    static class InstanceKeyMap {
        HashMap<InstanceKey, Integer> map = new HashMap<InstanceKey, Integer>();
        int i = 0;

        int get(InstanceKey pk) {
            Integer x = map.get(pk);
            if (x == null) {
                i++;
                map.put(pk, i);
                return i;

            } else {
                return x.intValue();
            }
        }
    }
}
