package org.expert.link.mesh.application.support

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/** Возвращает текущее время приложения. */
fun now(): Instant = Clock.System.now()

/** Добавляет миллисекунды к моменту времени. */
fun Instant.plusMillis(milliseconds: Long): Instant = Instant.fromEpochMilliseconds(toEpochMilliseconds() + milliseconds)

/** Добавляет секунды к моменту времени. */
fun Instant.plusSeconds(seconds: Long): Instant = plusMillis(seconds * 1_000)

/** Генерирует идентификатор с префиксом. */
fun newId(prefix: String): String = "$prefix-${randomHex(16)}"

private fun randomHex(sizeBytes: Int): String {
    val bytes = Random.Default.nextBytes(sizeBytes)
    val buffer = StringBuilder(bytes.size * 2)
    bytes.forEach { byte ->
        val value = byte.toInt() and 0xff
        buffer.append(HEX_DIGITS[value ushr 4])
        buffer.append(HEX_DIGITS[value and 0x0f])
    }
    return buffer.toString()
}

private const val HEX_DIGITS = "0123456789abcdef"
