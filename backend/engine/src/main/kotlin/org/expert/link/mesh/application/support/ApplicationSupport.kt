package org.expert.link.mesh.application.support

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/** Возвращает текущее время приложения. */
fun now(): Instant = Clock.System.now()

/** Добавляет миллисекунды к моменту времени. */
fun Instant.plusMillis(milliseconds: Long): Instant = Instant.fromEpochMilliseconds(toEpochMilliseconds() + milliseconds)

/** Добавляет секунды к моменту времени. */
fun Instant.plusSeconds(seconds: Long): Instant = plusMillis(seconds * 1_000)

/** Генерирует идентификатор с префиксом. */
fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
