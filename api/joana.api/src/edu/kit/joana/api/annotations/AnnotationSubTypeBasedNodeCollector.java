/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 * <p>
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Selector;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collect the corresponding overloading methods (and their parameters), but not the attributes
 */
public class AnnotationSubTypeBasedNodeCollector extends AnnotationTypeBasedNodeCollector {

  private SDGProgram program;

  private final AnnotationTypeBasedNodeCollector collector;

  private final Map<SDGMethod, Set<SDGMethod>> overloadingMethodsAndSelf;

  public AnnotationSubTypeBasedNodeCollector(SDG sdg, SDGProgram program) {
    this(sdg, program, new SDGClassComputation(sdg));
  }

  public AnnotationSubTypeBasedNodeCollector(SDG sdg, SDGProgram program, SDGClassComputation pp2NodeTrans) {
    super(sdg, pp2NodeTrans);
    this.program = program;
    this.collector = new AnnotationTypeBasedNodeCollector(sdg, pp2NodeTrans);
    this.overloadingMethodsAndSelf = new HashMap<>();
  }

  @Override public void init(SDGProgram program) {
    super.init(program);
    overloadingMethodsAndSelf.clear();
    this.program = program;
    collector.init(program);
  }

  private Set<IClass> getSubClassesAndSelf(SDGClass klass) {
    IClass iClass = getCha().lookupClass(klass.getTypeName().toBCString());
    return Stream.concat(Stream.of(iClass), getCha().computeSubClasses(iClass.getReference()).stream()).collect(Collectors.toSet());
  }

  private IClassHierarchy getCha() {
    return program.getClassHierarchy();
  }

  private IClass lookupClass(JavaType type) {
    return getCha().lookupClass(type.toBCString());
  }

  private Set<SDGMethod> getOverloadingMethodsAndSelfCached(SDGMethod method) {
    return overloadingMethodsAndSelf.computeIfAbsent(method, this::getOverloadingMethodsAndSelf);
  }

  private Set<SDGMethod> getOverloadingMethodsAndSelf(SDGMethod method) {
    IClass owningClass = lookupClass(method.getSignature().getDeclaringType());
    IMethod iMethod = owningClass.getMethod(Selector.make(method.getSignature().getSelector()));
    Set<IMethod> possibleTargets = getCha().getPossibleTargets(iMethod.getReference());
    return possibleTargets.stream().map(program::getMethod).collect(Collectors.toSet());
  }

  @Override protected Set<SDGNode> visitParameter(SDGFormalParameter param, AnnotationType type) {
    return getOverloadingMethodsAndSelf(param.getOwningMethod()).stream().map(m -> {
      if (m == null){
        System.err.println(String.format("No methods for %s", param.getOwningMethod()));
        return null;
      } else {
        System.out.println(type + " " + m);
      }
      return m.getParameter(param.getIndex());
    }).filter(Objects::nonNull)
        .flatMap(p -> collector.visitParameter(p, type).stream()).collect(Collectors.toSet());
  }

  @Override protected Set<SDGNode> visitMethod(SDGMethod method, AnnotationType type) {
    return getOverloadingMethodsAndSelfCached(method).stream().map(m -> {
      if (m == null){
        System.err.println(String.format("No methods for %s", method));
        return null;
      }
      return method;
    }).filter(Objects::nonNull).flatMap(m -> collector.visitMethod(m, type).stream())
        .collect(Collectors.toSet());
  }

}
