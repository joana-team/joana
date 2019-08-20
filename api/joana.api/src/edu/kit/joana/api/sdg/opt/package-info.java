/**
 * Their are multiple ways to pre process byte code before the analysis.
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
 */
package edu.kit.joana.api.sdg.opt;