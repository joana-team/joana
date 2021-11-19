package edu.kit.joana.api.sdg.opt.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM8

/** creates new names that only contain [a-z_0-9A-Z] are therefore usable in most Java related places */
class NameCreator(private val existingNames: MutableSet<String> = HashSet()) {
    fun create(name: String): String {
        var candidate = name.replace("[^a-z_0-9A-Z]".toRegex(), "")
        while (existingNames.contains(candidate)) {
            candidate += existingNames.size
        }
        existingNames.add(candidate)
        return candidate
    }

    companion object {

        @JvmStatic
        fun forClass(cr: ClassReader): NameCreator {
            val existingNames = HashSet<String>()
            cr.accept(
                object : ClassVisitor(ASM8) {
                    override fun visitField(
                        access: Int,
                        name: String,
                        descriptor: String?,
                        signature: String?,
                        value: Any?
                    ): FieldVisitor? {
                        existingNames.add(name)
                        return null
                    }

                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String?,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor? {
                        existingNames.add(name)
                        return null
                    }
                },
                ClassReader.EXPAND_FRAMES
            )
            return NameCreator(existingNames)
        }
    }
}
