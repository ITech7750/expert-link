package org.expert.link.mesh.bootstrap

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.config.MeshNodeConfig
import java.io.File

/** Тонкая CLI-обёртка над публичным API backend-модуля. */
fun main(args: Array<String>): Unit = runBlocking {
    require(args.isNotEmpty()) { "Usage: bootstrap <config.json>" }
    val configFile = File(args.first())
    require(configFile.exists()) { "Configuration file does not exist: ${configFile.absolutePath}" }
    val configuration = Json { ignoreUnknownKeys = true }.decodeFromString(MeshNodeConfig.serializer(), configFile.readText())
    MeshBackend.launch(configuration)
    awaitCancellation()
}
