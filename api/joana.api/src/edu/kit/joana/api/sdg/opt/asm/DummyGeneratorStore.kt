package edu.kit.joana.api.sdg.opt.asm

import edu.kit.joana.api.sdg.opt.OpenApiPreProcessorPass
import edu.kit.joana.util.Pair
import org.objectweb.asm.Type
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


/** Store DummyGenerator and only create them if really needed */
class DummyGeneratorStore(val basePath: Path, val packageName: String, val classNames: NameCreator,
                          val config: OpenApiPreProcessorPass.Config) {

    private val generators: MutableMap<Key, Result> = HashMap()

    data class Key(val config: OpenApiPreProcessorPass.Config,
                           val possibleConstructors: PossibleConstructors,
                           val mainType: Type, val subTypes: Set<Type>)
    data class Result(val method: BaseMethodDescriptor)

    fun get(key: Key) = generators.computeIfAbsent(key) {
        Result(DummyGenerator(key.config, key.possibleConstructors)
            .createClassForTypes(basePath, packageName, classNames, key.mainType, key.subTypes))
    }

    fun createClassForTypes(possibleConstructors: PossibleConstructors, mainType: Type, subTypes: Set<Type>
    ) = get(Key(config, possibleConstructors, mainType, subTypes)).method
}