package edu.kit.joana.api.sdg.opt.asm

import java.util.HashSet

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
}