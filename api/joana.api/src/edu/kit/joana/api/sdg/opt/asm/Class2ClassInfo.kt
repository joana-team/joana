package edu.kit.joana.api.sdg.opt.asm

import edu.kit.joana.util.TypeNameUtils
import io.github.classgraph.ClassInfo
import org.objectweb.asm.Type

/* it's far less boilerplate in kotlin than in Java */

class Class2ClassInfo(private val map: MutableMap<String, ClassInfo>) : Map<String, ClassInfo> by map {
    fun get(key: Type): ClassInfo? = get(TypeNameUtils.toJavaClassName(key))
}