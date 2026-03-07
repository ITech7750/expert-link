package org.expert.link.database

import kotlinx.serialization.json.Json

internal val storageJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

internal inline fun <reified T> encodePayload(value: T): String = storageJson.encodeToString(value)

internal inline fun <reified T> decodePayload(payload: String): T = storageJson.decodeFromString(payload)

internal fun participantKey(participantPeerIds: Set<String>): String =
    participantPeerIds.toList().sorted().joinToString("|")
