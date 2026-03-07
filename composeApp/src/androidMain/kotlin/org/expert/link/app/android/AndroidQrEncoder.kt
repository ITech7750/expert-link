package org.expert.link.app.android

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.expert.link.app.shared.platform.QrCodeMatrix

/** Генерирует QR-матрицу для Android host. */
internal fun buildAndroidQrCode(text: String): QrCodeMatrix? = runCatching {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0)
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
