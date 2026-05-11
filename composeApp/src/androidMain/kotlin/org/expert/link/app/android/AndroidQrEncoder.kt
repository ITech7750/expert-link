package org.expert.link.app.android

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.expert.link.app.shared.platform.QrCodeMatrix

/** Генерирует QR-матрицу для Android host. */
internal fun buildAndroidQrCode(text: String): QrCodeMatrix? = runCatching {
    val matrix = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        192,
        192,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val size = matrix.width
    QrCodeMatrix(
        size = size,
        darkModules = buildList(size * size) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    add(matrix.get(x, y))
                }
            }
        },
    )
}.getOrNull()
