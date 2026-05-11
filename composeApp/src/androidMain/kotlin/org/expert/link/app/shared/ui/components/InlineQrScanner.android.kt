package org.expert.link.app.shared.ui.components

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@Composable
actual fun InlineQrScanner(
    modifier: Modifier,
    enabled: Boolean,
    onCodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val latestOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val scannerView = remember(context) {
        DecoratedBarcodeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            statusView?.text = ""
            barcodeView.decoderFactory = DefaultDecoderFactory(
                listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                ),
            )
            barcodeView.cameraSettings.isAutoFocusEnabled = true
        }
    }
    val callback = remember(scannerView) {
        object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val text = result?.text?.trim().orEmpty()
                if (text.isEmpty()) return
                latestOnCodeScanned(text)
                scannerView.pause()
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) = Unit
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            scannerView.decodeContinuous(callback)
            scannerView
        },
        update = { view ->
            if (enabled) {
                view.decodeContinuous(callback)
                view.resume()
            } else {
                view.pause()
            }
        },
    )

    DisposableEffect(scannerView, enabled) {
        if (enabled) {
            scannerView.decodeContinuous(callback)
            scannerView.resume()
        } else {
            scannerView.pause()
        }
        onDispose {
            scannerView.pause()
        }
    }
}
