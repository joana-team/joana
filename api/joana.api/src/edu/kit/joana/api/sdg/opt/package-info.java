/**
 * There are multiple ways to preprocess byte code before the analysis.
 * This package contains different passes and a class that makes
 * it possible to combine them and apply them to byte code.
 *
 * Each pass has to retain the annotated methods, fields and
 * annotations.
 *
 * The idea is to first build an IFCAnalysis, add sinks, sources and declassifications.
 * Then the information stored in the analysis object is used to optimize the byte code
 * using different passes. At last the IFCAnalysis has to be rebuilt using the
 * optimized byte code. This analysis object can be used for the actual IFC analysis.
 *
 * There are currently 3 passes implemented:
 * {@link edu.kit.joana.api.sdg.opt.ProGuardPass} uses ProGuard to optimize the bytecode.
 * {@link edu.kit.joana.api.sdg.opt.SetValuePass} applies {@link edu.kit.joana.ui.annotations.SetValue} annotations.
 * {@link edu.kit.joana.api.sdg.opt.OpenApiPreProcessorPass} replaces potential OpenAPI client methods with dummies.
 */
package edu.kit.joana.api.sdg.opt;