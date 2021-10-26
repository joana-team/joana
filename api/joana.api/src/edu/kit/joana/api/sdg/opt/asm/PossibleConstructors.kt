package edu.kit.joana.api.sdg.opt.asm

import edu.kit.joana.api.sdg.opt.OpenApiPreProcessorPass
import edu.kit.joana.util.TypeNameUtils
import io.github.classgraph.ClassInfo
import io.github.classgraph.ClassInfoList
import io.github.classgraph.MethodInfo
import org.objectweb.asm.Type
import java.lang.reflect.Modifier
import java.util.* // ktlint-disable no-wildcard-imports
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun ClassInfo.toType(): Type {
    return Type.getType(TypeNameUtils.toInternalName(name))
}

val ClassInfo.children: ClassInfoList
    get() {
        return if (isInterface) classesImplementing else subclasses
    }

/**
 * Possible constructors per class, has support for dealing with types obtained via reflection heuristics
 */
class PossibleConstructors @JvmOverloads constructor(
    private val classInfoMap: Class2ClassInfo,
    private val config: OpenApiPreProcessorPass.Config,
    private val map: Map<Type, Set<String>> = HashMap(),
    private val typesUsedInReflection: Set<ClassInfo> = HashSet()
) : Map<Type, Set<String>> by map {
    fun forType(type: Type): PossibleConstructors {
        val out: MutableMap<Type, Set<String>> = HashMap()
        val classInfo = classInfoMap[TypeNameUtils.toJavaClassName(type)]
            ?: return PossibleConstructors(classInfoMap, config, HashMap(), typesUsedInReflection)
        Stream.concat(
            (if (classInfo.isInterface) classInfo.classesImplementing else classInfo.subclasses).stream(),
            if (classInfo.isStandardClass && !classInfo.isAbstract) Stream.of(classInfo) else Stream.empty()
        )
            .map { c: ClassInfo -> Type.getType(TypeNameUtils.javaClassNameToInternalName(c.name)) }
            .forEach { t: Type ->
                val cons = getConstructorDescriptorsForClass(t)
                if (cons.isNotEmpty()) {
                    out[t] = cons
                }
            }
        if (classInfo.isStandardClass && !classInfo.isAbstract && (
            map.containsKey(type) || typesUsedInReflection.contains(
                    classInfoMap[TypeNameUtils.toJavaClassName(type)]
                )
            )
        ) {
            out[type] = getConstructorDescriptorsForClass(type)
        }
        return PossibleConstructors(classInfoMap, config, out, typesUsedInReflection)
    }

    private fun getConstructorDescriptorsForClass(type: Type): Set<String> {
        val klass = classInfoMap[TypeNameUtils.toJavaClassName(type)]
        val cons = Stream.concat(
            map.getOrDefault(type, emptySet()).stream(),
            if (typesUsedInReflection.contains(klass)) getOwnConstructors(klass!!).stream() else Stream.empty()
        ).collect(Collectors.toSet())
        return if (config.useSingleConstructorPerType) {
            cons.stream().sorted(Comparator.comparingInt { obj: String -> obj.length }).limit(1).collect(Collectors.toSet())
        } else cons
    }

    private fun getOwnConstructors(klass: ClassInfo) = klass.constructorInfo.stream()
        .filter { x: MethodInfo -> !x.isNative && (!Modifier.isPrivate(x.modifiers) || !config.ignorePrivateConstructors) }
        .map { obj: MethodInfo -> obj.typeDescriptorStr }.collect(Collectors.toSet())

    fun methodDescriptorStream(): Stream<BaseMethodDescriptor> {
        return map.entries.stream()
            .flatMap { (type, constructorDescriptors) ->
                constructorsToMethodDescriptors(type, constructorDescriptors)
            }
    }

    private fun constructorsToMethodDescriptors(
        type: Type,
        constructorDescriptors: Set<String>
    ) = constructorDescriptors.stream().map { d: String? ->
        BaseMethodDescriptor(
            type.toString(), "<init>", d
        )
    }

    override fun toString(): String {
        return map.toString()
    }

    fun hasConstructors(type: Type) = !get(type).isNullOrEmpty()

    /** important: assumes class info map and config are equal */
    fun combine(other: PossibleConstructors): PossibleConstructors {
        return PossibleConstructors(classInfoMap, config, map + other.map, typesUsedInReflection + other.typesUsedInReflection)
    }

    /**
     * Like a type hierarchy, but contains only classes with possible constructors
     */
    class TypeTree private constructor(private var type2node: Map<Type, Node>) {
        private class Node(val type: Type, val children: MutableSet<Node> = HashSet())

        private fun get(type: Type) = type2node[type]

        fun hasType(type: Type) = type2node.containsKey(type)

        /** a type without subtypes in this tree  */
        fun isTrivialType(type: Type): Boolean = get(type)?.children?.isEmpty() ?: true

        fun getSubTypes(type: Type): List<Type> = get(type)?.let { n -> n.children.map { it.type } } ?: listOf()

        companion object {

            fun topLevelTypes(possibleConstructors: PossibleConstructors): Collection<Type> {
                with(possibleConstructors) {
                    return classInfoMap.values.filter { it.isInterface && it.interfaces.isEmpty() }.map { it.toType() } +
                        Type.getType(Object::class.java)
                }
            }

            fun create(possibleConstructors: PossibleConstructors): TypeTree {
                val queue: Queue<Node> = ArrayDeque()
                val type2node: MutableMap<Type, Node> = HashMap()

                fun push(node: Node) {
                    if (!type2node.containsValue(node)) {
                        type2node[node.type] = node
                        queue.add(node)
                    }
                }
                fun push(nodes: Collection<Node>) = nodes.forEach { push(it) }

                push(topLevelTypes(possibleConstructors).map { Node(it) })

                while (queue.isNotEmpty()) {
                    val cur = queue.poll()
                    possibleConstructors.classInfoMap.get(cur.type)?.also { it: ClassInfo ->
                        val children = it.children.directOnly().map { Node(it.toType()) }
                        push(children)
                        cur.children.addAll(children)
                    }
                }
                return TypeTree(type2node)
            }
        }
    }

    fun createTypeTree(): TypeTree {
        return TypeTree.create(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PossibleConstructors) return false

        if (classInfoMap != other.classInfoMap) return false
        if (config != other.config) return false
        if (map != other.map) return false
        if (typesUsedInReflection != other.typesUsedInReflection) return false

        return true
    }

    override fun hashCode() = Objects.hash(classInfoMap, config, map, typesUsedInReflection)

    fun findOwnConstructors(type: Type): Set<String> {
        return classInfoMap.get(type)?.let {
            if (it.isStandardClass && !it.isAbstract) {
                getOwnConstructors(it)
            } else {
                emptySet()
            }
        } ?: emptySet()
    }

    fun findOwnConstructorDescriptors(type: Type) = constructorsToMethodDescriptors(type, findOwnConstructors(type))
}
