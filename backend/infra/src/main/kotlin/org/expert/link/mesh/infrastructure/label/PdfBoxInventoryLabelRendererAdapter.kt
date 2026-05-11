package org.expert.link.mesh.infrastructure.label

import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.inventory.InventoryBarcodeFormat
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelField
import org.expert.link.mesh.domain.port.external.InventoryLabelArtifact
import org.expert.link.mesh.domain.port.external.InventoryLabelRendererPort
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

class PdfBoxInventoryLabelRendererAdapter(
    baseDirectory: String,
) : InventoryLabelRendererPort {
    private val outputDirectory = File(baseDirectory, "labels").apply { mkdirs() }

    override suspend fun renderPdf(label: InventoryLabel, fileName: String): InventoryLabelArtifact {
        return renderPdfBatch(listOf(label), fileName)
    }

    override suspend fun renderPdfBatch(labels: List<InventoryLabel>, fileName: String): InventoryLabelArtifact {
        require(labels.isNotEmpty()) { "Labels list is empty" }
        val resolvedName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
        val target = uniqueTarget(resolvedName)
        PDDocument().use { document ->
            labels.forEach { label ->
                document.addPage(buildPage(document, label))
            }
            document.save(target)
        }
        val descriptor = FileDescriptor(
            fileId = "label-${target.nameWithoutExtension}-${System.currentTimeMillis()}",
            fileName = target.name,
            sizeBytes = target.length(),
            sha256 = target.inputStream().use { sha256Hex(it) },
            contentType = "application/pdf",
        )
        return InventoryLabelArtifact(descriptor = descriptor, localPath = target.absolutePath)
    }

    private fun buildPage(document: PDDocument, label: InventoryLabel): PDPage {
        val page = PDPage(PDRectangle(280f, 170f))
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        val margin = 12f
        val qrSize = 72f
        val barcodeHeight = 36f
        val barcodeWidth = pageWidth - margin * 2 - qrSize - 8f
        val textStartY = pageHeight - margin - 12f
        val regularFont = loadFont(document, REGULAR_FONT_CANDIDATES)
        val boldFont = loadFont(document, BOLD_FONT_CANDIDATES, regularFont)

        PDPageContentStream(document, page).use { content ->
            content.beginText()
            content.setFont(boldFont, 10f)
            content.newLineAtOffset(margin, textStartY)
            content.showText(label.templateName)
            content.endText()

            drawFields(content, label.fields, margin, textStartY - 14f, regularFont)

            label.barcodeValue?.takeIf { it.isNotBlank() }?.let { value ->
                val image = buildBarcodeImage(value, barcodeWidth.toInt(), barcodeHeight.toInt())
                val x = margin
                val y = margin
                val pdImage = LosslessFactory.createFromImage(document, image)
                content.drawImage(pdImage, x, y, barcodeWidth, barcodeHeight)
            }

            label.qrValue?.takeIf { it.isNotBlank() }?.let { value ->
                val image = buildQrImage(value, qrSize.toInt(), qrSize.toInt())
                val x = pageWidth - margin - qrSize
                val y = margin
                val pdImage = LosslessFactory.createFromImage(document, image)
                content.drawImage(pdImage, x, y, qrSize, qrSize)
            }
        }
        return page
    }

    private fun drawFields(
        content: PDPageContentStream,
        fields: List<InventoryLabelField>,
        x: Float,
        startY: Float,
        font: PDFont,
    ) {
        var cursorY = startY
        fields.forEach { field ->
            content.beginText()
            content.setFont(font, 9f)
            content.newLineAtOffset(x, cursorY)
            content.showText("${field.label}: ${field.value}")
            content.endText()
            cursorY -= 12f
        }
    }

    private fun buildBarcodeImage(value: String, width: Int, height: Int): BufferedImage {
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.CODE_128,
            width,
            height,
            mapOf(EncodeHintType.MARGIN to 1),
        )
        return matrix.toBufferedImage()
    }

    private fun buildQrImage(value: String, width: Int, height: Int): BufferedImage {
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.QR_CODE,
            width,
            height,
            mapOf(EncodeHintType.MARGIN to 1),
        )
        return matrix.toBufferedImage()
    }

    private fun uniqueTarget(fileName: String): File {
        return generateSequence(0) { it + 1 }
            .map { index ->
                if (index == 0) {
                    File(outputDirectory, fileName)
                } else {
                    val extension = fileName.substringAfterLast('.', "")
                    val baseName = if (extension.isBlank()) fileName else fileName.removeSuffix(".$extension")
                    val resolvedName = if (extension.isBlank()) "$baseName ($index)" else "$baseName ($index).$extension"
                    File(outputDirectory, resolvedName)
                }
            }
            .first { !it.exists() }
    }

    private fun sha256Hex(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun loadFont(
        document: PDDocument,
        candidates: List<String>,
        fallback: PDFont = PDType1Font.HELVETICA,
    ): PDFont {
        candidates.asSequence()
            .map(::File)
            .firstOrNull { it.exists() && it.isFile }
            ?.inputStream()
            ?.use { stream ->
                return PDType0Font.load(document, stream, true)
            }
        return fallback
    }

    private companion object {
        val REGULAR_FONT_CANDIDATES = listOf(
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
        )

        val BOLD_FONT_CANDIDATES = listOf(
            "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "C:/Windows/Fonts/arialbd.ttf",
            "C:/Windows/Fonts/segoeuib.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        )
    }
}

private fun com.google.zxing.common.BitMatrix.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until width) {
        for (y in 0 until height) {
            image.setRGB(x, y, if (get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return image
}
