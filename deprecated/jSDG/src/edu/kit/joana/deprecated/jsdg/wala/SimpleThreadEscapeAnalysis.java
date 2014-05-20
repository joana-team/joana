/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.deprecated.jsdg.wala;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * <P>
 * A simple thread-level escape analysis: this code computes the set of classes of which some instance may be accessed
 * by some thread other than the one that created it.
 * </P>
 *
 * <P>
 * The algorithm is not very bright; it is based on the observation that there are only three ways for an object to pass
 * from one thread to another.
 * <UL>
 * <LI> The object is stored into a static variable.
 * <LI> The object is stored into an instance field of a Thread
 * <LI> The object is reachable from a field of another escaping object.
 * </UL>
 * </P>
 *
 * <P>
 * This observation is implemented in the obvious way:
 * <OL>
 * <LI> All static fields are collected
 * <LI> All Thread constructor parameters are collected
 * <LI> The points-to sets of these values represent the base set of escapees.
 * <LI> All object reachable from fields of these objects are added
 * <LI> This process continues until a fixpoint is reached
 * <LI> The abstract objects in the points-to sets are converted to types
 * <LI> This set of types is returned
 * </OL>
 * </P>
 *
 * @author Julian Dolby
 */
public class SimpleThreadEscapeAnalysis {

  /**
   * The heart of the analysis.
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public Set<IClass> gatherThreadEscapingClasses(CallGraph cg, PointerAnalysis<InstanceKey> pa, ClassHierarchy cha)
  throws ClassHierarchyException, IllegalArgumentException, CancelException {

    //
    // collect all places where objects can escape their creating thread:
    // 1) all static fields
    // 2) arguments to Thread constructors
    //
    Set<PointerKey> escapeAnalysisRoots = HashSetFactory.make();
    HeapModel heapModel = pa.getHeapModel();

    // 1) static fields
    for (IClass cls : cha) {
      Collection<IField> staticFields = cls.getDeclaredStaticFields();
      for (IField sf : staticFields) {
        if (sf.getFieldTypeReference().isReferenceType()) {
          escapeAnalysisRoots.add(heapModel.getPointerKeyForStaticField(sf));
        }
      }
    }

    // 2) instance fields of Threads
    // (we hack this by getting the 'this' parameter of all ctor calls;
    // this works because the next phase will add all objects transitively
    // reachable from fields of types in these pointer keys, and all
    // Thread objects must be constructed somewhere)
    Collection<IClass> threads = cha.computeSubClasses(TypeReference.JavaLangThread);
    for (IClass cls : threads) {
      for (IMethod m : cls.getDeclaredMethods()) {
        if (m.isInit()) {
          Set<CGNode> nodes = cg.getNodes(m.getReference());
          for (CGNode n : nodes) {
            escapeAnalysisRoots.add(heapModel.getPointerKeyForLocal(n, 1));
          }
        }
      }
    }

    //
    // compute escaping types: all types flowing to escaping roots and
    // all types transitively reachable through their fields.
    //
    Set<InstanceKey> escapingInstanceKeys = HashSetFactory.make();

    //
    // pass 1: get abstract objects (instance keys) for escaping locations
    //
    for (PointerKey root : escapeAnalysisRoots) {
      OrdinalSet<InstanceKey> objects = pa.getPointsToSet(root);
      for (InstanceKey obj : objects) {
        escapingInstanceKeys.add(obj);
      }
    }

    //
    // passes 2+: get fields of escaping keys, and add pointed-to keys
    //
    Set<InstanceKey> newKeys = HashSetFactory.make();
    do {
      newKeys.clear();
      for (InstanceKey key : escapingInstanceKeys) {
        IClass type = key.getConcreteType();
        if (type.isReferenceType()) {
          if (type.isArrayClass()) {
            if (((ArrayClass) type).getElementClass() != null) {
              PointerKey fk = heapModel.getPointerKeyForArrayContents(key);
              OrdinalSet<InstanceKey> fobjects = pa.getPointsToSet(fk);
              for (InstanceKey fobj : fobjects) {
                if (!escapingInstanceKeys.contains(fobj)) {
                  newKeys.add(fobj);
                }
              }
            }
          } else {
            Collection<IField> fields = type.getAllInstanceFields();
            for (IField f : fields) {
              if (f.getFieldTypeReference().isReferenceType()) {
                PointerKey fk = heapModel.getPointerKeyForInstanceField(key, f);
                OrdinalSet<InstanceKey> fobjects = pa.getPointsToSet(fk);
                for (InstanceKey fobj : fobjects) {
                  if (!escapingInstanceKeys.contains(fobj)) {
                    newKeys.add(fobj);
                  }
                }
              }
            }
          }
        }
      }
      escapingInstanceKeys.addAll(newKeys);
    } while (!newKeys.isEmpty());

    //
    // get set of types from set of instance keys
    //
    Set<IClass> escapingTypes = HashSetFactory.make();
    for (InstanceKey key : escapingInstanceKeys) {
      escapingTypes.add(key.getConcreteType());
    }

    return escapingTypes;
  }

  public static void printResults(Set<IClass> escapingTypes) throws ClassHierarchyException {
    for (IClass cls : escapingTypes) {
      if (!cls.isArrayClass()) {
        for (IField f : cls.getAllFields()) {
          if (!f.isVolatile() && !f.isFinal()) {
            System.err.println(f.getReference());
          }
        }
      }
    }
  }
}
