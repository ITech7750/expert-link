package org.expert.link.mesh.domain.support

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Кодирует данные в URL-safe Base64 без padding. */
@OptIn(ExperimentalEncodingApi::class)
fun encodeBase64Url(data: ByteArray): String = Base64.UrlSafe.encode(data).trimEnd('=')

/** Декодирует URL-safe Base64, допускающий отсутствие padding. */
@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64Url(encoded: String): ByteArray {
    val missingPadding = (4 - encoded.length % 4) % 4
    return Base64.UrlSafe.decode(encoded + "=".repeat(missingPadding))
}

/** Кодирует данные в стандартный Base64. */
@OptIn(ExperimentalEncodingApi::class)
fun encodeBase64(data: ByteArray): String = Base64.Default.encode(data)

/** Декодирует стандартный Base64. */
@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64(encoded: String): ByteArray = Base64.Default.decode(encoded)
