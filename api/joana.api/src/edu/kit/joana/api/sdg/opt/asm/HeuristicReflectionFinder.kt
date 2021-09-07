package edu.kit.joana.api.sdg.opt.asm

import edu.kit.joana.wala.core.SDGBuilder
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import nonapi.io.github.classgraph.classpath.SystemJarFinder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.Type
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Find classes that are used in reflective calls using heuristics.
 *
 * Currently, collects classes from the following places:
 * - type signatures (used by GSON)
 * - X.class
 *
 * Additionally, finds constructors per class
 */
class HeuristicReflectionFinder @JvmOverloads constructor(
    /**
     * directory full of class files to analyze
     */
    private val path: Path,
    private val cgr: SDGBuilder.CGResult,
    /**
     * java class name → info,
     * keeps us from computing the information ourselves
     */
    private val classInfoMap: Map<String, ClassInfo> = ClassGraph().enableMethodInfo()
        .overrideClasspath(SystemJarFinder.getJreRtJarPath() + ":" + path)
        .enableSystemJarsAndModules().enableInterClassDependencies().scan().allClassesAsMap
) {

    val foundTypes: MutableSet<Type> = HashSet()

    fun run() {
        processClassInfos()
        processFiles()
    }

    /** gather information directly from the classInfoMap (generated via classgraph)  */
    private fun processClassInfos() {
        foundTypes.addAll(classInfoMap.values.flatMap { processClassInfo(it) })
    }

    private fun processClassInfo(info: ClassInfo): Set<Type> {
        val deps = info.classDependencies
        val params = info.typeSignature?.typeParameters
        return emptySet()
    }

    /** use asm to process the class files  */
    private fun processFiles() {
        try {
            path.toFile().walkTopDown().forEach {
                if (it.toString().endsWith(".class")) {
                    processFile(it.toPath())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun processFile(path: Path) {
        val cr = ClassReader(Files.newInputStream(path))
        val vis = AnalysisVisitor()
        cr.accept(vis, ClassReader.EXPAND_FRAMES)
        foundTypes += vis.foundTypes
    }

    internal inner class AnalysisVisitor : org.objectweb.asm.ClassVisitor(ASM8) {

        val foundTypes: HashSet<Type> = HashSet()
        private val methodVisitor = object : MethodVisitor(ASM8) {
            override fun visitLdcInsn(value: Any?) {
                super.visitLdcInsn(value)
                when (value) {
                    is Type -> {
                        val type = if (value.sort == Type.ARRAY) value.elementType else value
                        if (type.sort == Type.OBJECT) {
                            foundTypes.add(type)
                        }
                    }
                }
            }
        }
        private val signatureVisitor = object : org.objectweb.asm.signature.SignatureVisitor(ASM8) {
            override fun visitClassType(name: String) {
                super.visitClassType(name)
                foundTypes.add(Type.getObjectType(name))
            }
        }

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String?>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            signature?.let {
                org.objectweb.asm.signature.SignatureReader(signature).accept(signatureVisitor)
            }
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            return methodVisitor
        }

    }
}