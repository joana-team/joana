/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Leveling.java
 *
 * Created on 29. August 2005, 17:52
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;


/**
 * This class is the super class for leveling strategies.
 * @author Siegfried Weber
 */
public abstract class LevelingStrategy extends LayoutProgress {

    /**
     * Starts the leveling.
     * @param graph a PDG
     */
    public abstract void level(PDG graph);
}
