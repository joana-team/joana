package edu.kit.joana.wala.summary.test;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder;

import java.util.function.BiFunction;

public enum SDGConfigs {

  SMALL((classPath, entryMethod) -> new SDGConfig(classPath, true, entryMethod, Stubs.JRE_15,
      SDGBuilder.ExceptionAnalysis.IGNORE_ALL,
      SDGBuilder.FieldPropagation.NONE, SDGBuilder.PointsToPrecision.OBJECT_SENSITIVE, false, false, MHPType.NONE)),

  DEFAULT((classPath, entryMethod) -> new SDGConfig(classPath, true, entryMethod, Stubs.JRE_15,
      SDGBuilder.ExceptionAnalysis.INTERPROC,
      SDGBuilder.FieldPropagation.OBJ_GRAPH, SDGBuilder.PointsToPrecision.INSTANCE_BASED, false, false, MHPType.NONE)),

  DEFAULT_17((classPath, entryMethod) -> new SDGConfig(classPath, true, entryMethod, Stubs.JRE_17,
      SDGBuilder.ExceptionAnalysis.IGNORE_ALL,
      SDGBuilder.FieldPropagation.OBJ_GRAPH, SDGBuilder.PointsToPrecision.INSTANCE_BASED, false, false, MHPType.NONE)),

  SLOW((classPath, entryMethod) -> new SDGConfig(classPath, true, entryMethod, Stubs.JRE_15,
      SDGBuilder.ExceptionAnalysis.INTERPROC,
      SDGBuilder.FieldPropagation.OBJ_GRAPH, SDGBuilder.PointsToPrecision.OBJECT_SENSITIVE, false, false, MHPType.NONE)),

  SLOW_17((classPath, entryMethod) -> new SDGConfig(classPath, true, entryMethod, Stubs.JRE_17,
      SDGBuilder.ExceptionAnalysis.INTERPROC,
      SDGBuilder.FieldPropagation.OBJ_GRAPH, SDGBuilder.PointsToPrecision.OBJECT_SENSITIVE, false, false, MHPType.NONE));

  private final BiFunction<String, String, SDGConfig> creator;

  SDGConfigs(BiFunction<String, String, SDGConfig> creator){
    this.creator = creator;
  }

  public SDGConfig instantiate(String classPath, String entryMethod){
    return creator.apply(classPath, entryMethod);
  }

  public SDGConfig instantiate() {
    return instantiate("", null);
  }
}
