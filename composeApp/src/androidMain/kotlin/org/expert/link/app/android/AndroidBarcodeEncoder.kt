package org.expert.link.app.android

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.expert.link.app.shared.platform.BarcodeMatrix

internal fun buildAndroidBarcode(text: String): BarcodeMatrix? = runCatching {
    val width = 320
    val height = 120
    val matrix = MultiFormatWriter().encode(
        text,
        BarcodeFormat.CODE_128,
        width,
        height,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    BarcodeMatrix(
        width = matrix.width,
        height = matrix.height,
        darkModules = buildList(matrix.width * matrix.height) {
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    add(matrix.get(x, y))
                }
            }
        },
    )
}.getOrNull()
