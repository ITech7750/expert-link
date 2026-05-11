package org.expert.link.app.shared.platform

import java.awt.Desktop
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.awt.print.PrinterJob
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlinx.serialization.json.Json
import org.expert.link.app.desktop.buildDesktopBarcode
import org.expert.link.app.desktop.DesktopWebRtcMediaEngineAdapter
import org.expert.link.app.desktop.buildDesktopQrCode
import org.expert.link.database.createPersistentRepositoryBundle
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.printing.PDFPageable
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** Desktop actual-реализация платформенных сервисов client-модуля. */
actual class AppPlatformServices actual constructor(
    private val args: List<String>,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val memoryMode: Boolean = args.any { it == "--memory" }
    private val mediaEngine = DesktopWebRtcMediaEngineAdapter()
    private val storageDirectory = File(System.getProperty("user.home"), ".expert-link").apply { mkdirs() }
    private val configFile = File(storageDirectory, "node-config.json")
    private val persistentRepositories by lazy {
        createPersistentRepositoryBundle(
            path = File(storageDirectory, "expert-link.db").absolutePath,
        )
    }

    actual val platformName: String = "Рабочий стол"
    actual val transportHint: String = if (memoryMode) "Локальная проверка" else "Полная сеть"
    actual val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = true,
        canShareFiles = false,
        canSaveFiles = true,
        canPrintFiles = !GraphicsEnvironment.isHeadless(),
        canOpenFiles = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN),
        canRenderQr = true,
        canRenderBarcode = true,
        canScanQr = true,
        canPickFile = true,
        prefersWideLayout = true,
    )

    actual fun defaultConfig(): MeshNodeConfig {
        val fallback = createDefaultNodeConfig(
            displayNamePrefix = "desktop",
            realTransport = true,
            realDiscovery = true,
        ).copy(
            fileTransfer = MeshFileTransferConfig(
                downloadDirectory = File(storageDirectory, "downloads").absolutePath,
            ),
        )
        val base = loadPersistedConfig()?.let { persisted ->
            persisted.copy(
                fileTransfer = persisted.fileTransfer.copy(
                    downloadDirectory = File(storageDirectory, "downloads").absolutePath,
                ),
            )
        } ?: fallback
        return args.fold(base) { config, arg ->
            when {
                arg.startsWith("--name=") -> config.copy(displayName = arg.substringAfter('='))
                arg.startsWith("--bind=") -> config.copy(bindHost = arg.substringAfter('='))
                arg.startsWith("--http=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(httpPort = it) } ?: config
                arg.startsWith("--discovery=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(discoveryPort = it) } ?: config
                arg.startsWith("--multicast=") -> config.copy(multicastGroup = arg.substringAfter('='))
                arg == "--memory" -> config.copy(
                    featureFlags = config.featureFlags.copy(
                        inMemoryTransport = true,
                        inMemoryDiscovery = true,
                    ),
                )
                else -> config
            }
        }
    }

    actual fun persistConfig(config: MeshNodeConfig) {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json.encodeToString(MeshNodeConfig.serializer(), config))
    }

    actual suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(
        configuration = config,
        mediaEngine = mediaEngine,
        persistentRepositories = persistentRepositories,
    )

    actual suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    actual suspend fun shareText(label: String, text: String): Result<Unit> = runCatching {
        val encodedSubject = URLEncoder.encode(label, StandardCharsets.UTF_8.toString())
        val encodedBody = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val uri = URI("mailto:?subject=$encodedSubject&body=$encodedBody")
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            Desktop.getDesktop().mail(uri)
        } else {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
        }
    }

    actual suspend fun shareFile(label: String, path: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("Системный share файла на desktop не поддерживается"),
    )

    actual suspend fun saveFileToDownloads(path: String, fileName: String): Result<String?> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val target = runOnAwtThread {
            val dialog = FileDialog(null as Frame?, "Сохранить файл", FileDialog.SAVE).apply {
                file = fileName
                directory = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }.absolutePath
            }
            dialog.isVisible = true
            val selected = requireNotNull(dialog.file) { "Сохранение отменено" }
            File(dialog.directory ?: File(System.getProperty("user.home"), "Downloads").absolutePath, selected)
        }
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        target.absolutePath
    }

    actual suspend fun openFile(path: String): Result<Unit> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        require(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            "Открытие файлов на этой платформе недоступно"
        }
        Desktop.getDesktop().open(source)
    }

    actual suspend fun cacheLocalArtifact(path: String, fileName: String): Result<String> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val artifactsDir = File(storageDirectory, "artifacts").apply { mkdirs() }
        val target = File(artifactsDir, fileName)
        source.copyTo(target, overwrite = true)
        target.absolutePath
    }

    actual suspend fun createInventoryExportArtifact(
        document: InventoryExportDocument,
        format: MeshInventoryExportFormat,
        suggestedFileName: String,
    ): Result<LocalArtifact> = runCatching {
        val exportsDir = File(storageDirectory, "exports").apply { mkdirs() }
        val sanitized = suggestedFileName
            .replace(Regex("[^A-Za-z0-9А-Яа-я _.-]+"), "-")
            .trim()
            .ifBlank { "inventory-export" }
        val extension = when (format) {
            MeshInventoryExportFormat.CSV -> "csv"
            MeshInventoryExportFormat.XLSX -> "xlsx"
            MeshInventoryExportFormat.PDF -> "pdf"
        }
        val target = uniqueTarget(exportsDir, "$sanitized.$extension")
        when (format) {
            MeshInventoryExportFormat.CSV -> target.writeText(buildCsv(document), Charsets.UTF_8)
            MeshInventoryExportFormat.XLSX -> writeXlsx(document, target)
            MeshInventoryExportFormat.PDF -> writePdf(document, target)
        }
        val descriptor = describeFile(target.absolutePath).getOrThrow()
        LocalArtifact(path = target.absolutePath, descriptor = descriptor)
    }

    actual suspend fun resolveLocalArtifactPath(fileName: String): Result<String?> = runCatching {
        val roots = listOf(
            storageDirectory,
            File(storageDirectory, "downloads"),
            File(storageDirectory, "labels"),
            File(storageDirectory, "artifacts"),
        ).distinct().filter { it.exists() }
        roots.asSequence()
            .flatMap { root -> root.walkTopDown().asSequence() }
            .firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) }
            ?.absolutePath
    }

    actual suspend fun printFile(path: String): Result<Unit> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        if (source.extension.equals("pdf", ignoreCase = true)) {
            PDDocument.load(source).use { document ->
                runOnAwtThread {
                    val printerJob = PrinterJob.getPrinterJob()
                    printerJob.jobName = source.name
                    printerJob.setPageable(PDFPageable(document))
                    check(printerJob.printDialog()) { "Печать отменена" }
                    printerJob.print()
                }
            }
            return@runCatching Unit
        }
        require(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
            "Печать на этой платформе недоступна"
        }
        Desktop.getDesktop().print(source)
    }

    actual suspend fun pickFile(): Result<String?> = runCatching {
        runOnAwtThread {
            val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
            dialog.isVisible = true
            val selected = dialog.file ?: return@runOnAwtThread null
            File(dialog.directory, selected).absolutePath
        }
    }

    actual suspend fun describeFile(path: String): Result<MeshFileDescriptor> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        MeshFileDescriptor(
            fileId = "file-${UUID.randomUUID()}",
            fileName = source.name,
            sizeBytes = source.length(),
            sha256 = source.inputStream().use { stream -> sha256Hex(stream) },
            contentType = java.net.URLConnection.guessContentTypeFromName(source.name)?.ifBlank { null },
        )
    }

    actual suspend fun scanQr(): Result<String?> = runCatching {
        val selectedFile = runOnAwtThread {
            val dialog = FileDialog(null as Frame?, "Выберите изображение с QR или штрихкодом", FileDialog.LOAD).apply {
                file = "*.png;*.jpg;*.jpeg;*.bmp"
            }
            dialog.isVisible = true
            val selected = dialog.file ?: return@runOnAwtThread null
            File(dialog.directory, selected)
        }
        val source = selectedFile ?: return@runCatching null
        require(source.exists()) { "Файл не найден" }
        val image = requireNotNull(ImageIO.read(source)) { "Не удалось прочитать изображение" }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels)))
        MultiFormatReader().decode(bitmap).text
    }

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildDesktopQrCode(text)

    actual fun buildBarcode(text: String): BarcodeMatrix? = buildDesktopBarcode(text)

    actual suspend fun shareQrImage(label: String, text: String): Result<Unit> = runCatching {
        val matrix = buildQrCode(text) ?: error("Не удалось сформировать QR")
        val png = writeQrPng(matrix, label)
        when {
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN) -> {
                Desktop.getDesktop().open(png)
            }

            else -> {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(png.absolutePath), null)
            }
        }
    }

    private fun loadPersistedConfig(): MeshNodeConfig? {
        if (!configFile.exists()) return null
        return try {
            json.decodeFromString(MeshNodeConfig.serializer(), configFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256Hex(stream: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun buildCsv(document: InventoryExportDocument): String {
        val lines = buildList {
            add(document.title)
            document.subtitle?.takeIf { it.isNotBlank() }?.let(::add)
            if (document.summary.isNotEmpty()) {
                add("")
                document.summary.forEach { (label, value) -> add("$label,$value") }
            }
            if (document.columns.isNotEmpty()) {
                add("")
                add(document.columns.joinToString(",") { csvEscape(it) })
                document.rows.forEach { row ->
                    add(row.joinToString(",") { csvEscape(it) })
                }
            }
        }
        return lines.joinToString(System.lineSeparator())
    }

    private fun writeXlsx(document: InventoryExportDocument, target: File) {
        ZipOutputStream(FileOutputStream(target)).use { zip ->
            writeZipEntry(
                zip,
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                </Types>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "docProps/app.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
                    xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                  <Application>Expert Link</Application>
                </Properties>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "docProps/core.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:dcterms="http://purl.org/dc/terms/"
                    xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <dc:title>${xmlEscape(document.title)}</dc:title>
                  <dc:creator>Expert Link</dc:creator>
                </cp:coreProperties>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "xl/workbook.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="Отчёт" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "xl/_rels/workbook.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            writeZipEntry(
                zip,
                "xl/styles.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border/></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
                </styleSheet>
                """.trimIndent(),
            )
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildWorksheetXml(document))
        }
    }

    private fun buildWorksheetXml(document: InventoryExportDocument): String {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf(document.title))
        document.subtitle?.takeIf { it.isNotBlank() }?.let { rows.add(listOf(it)) }
        if (document.summary.isNotEmpty()) {
            rows.add(emptyList())
            document.summary.forEach { (label, value) -> rows.add(listOf(label, value)) }
        }
        if (document.columns.isNotEmpty()) {
            rows.add(emptyList())
            rows.add(document.columns)
            rows.addAll(document.rows)
        }
        val sheetRows = rows.mapIndexed { index, row ->
            val cells = row.mapIndexed { cellIndex, value ->
                val ref = "${columnName(cellIndex)}${index + 1}"
                """<c r="$ref" t="inlineStr"><is><t>${xmlEscape(value)}</t></is></c>"""
            }.joinToString("")
            """<row r="${index + 1}">$cells</row>"""
        }.joinToString("")
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                $sheetRows
              </sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun writePdf(document: InventoryExportDocument, target: File) {
        PDDocument().use { pdf ->
            val regularFont = loadFont(pdf, REGULAR_FONT_CANDIDATES)
            val boldFont = loadFont(pdf, BOLD_FONT_CANDIDATES, regularFont)
            val pageRect = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
            var page: PDPage = PDPage(pageRect)
            pdf.addPage(page)
            var content: PDPageContentStream = PDPageContentStream(pdf, page)
            val margin = 36f
            var cursorY: Float = page.mediaBox.height - margin
            val lineHeight = 14f
            fun ensureSpace(requiredHeight: Float = lineHeight) {
                if (cursorY - requiredHeight > margin) return
                content.close()
                page = PDPage(pageRect)
                pdf.addPage(page)
                content = PDPageContentStream(pdf, page)
                cursorY = page.mediaBox.height - margin
            }
            fun writeLine(text: String, font: PDFont, size: Float = 10f) {
                ensureSpace(size + 4f)
                content.beginText()
                content.setFont(font, size)
                content.newLineAtOffset(margin, cursorY)
                content.showText(text)
                content.endText()
                cursorY -= (size + 4f)
            }
            writeLine(fitText(document.title, boldFont, 16f, page.mediaBox.width - margin * 2), boldFont, 16f)
            document.subtitle?.takeIf { it.isNotBlank() }?.let {
                writeLine(fitText(it, regularFont, 11f, page.mediaBox.width - margin * 2), regularFont, 11f)
            }
            if (document.summary.isNotEmpty()) {
                cursorY -= 4f
                document.summary.forEach { (label, value) ->
                    writeLine(
                        fitText("$label: $value", regularFont, 10f, page.mediaBox.width - margin * 2),
                        regularFont,
                        10f,
                    )
                }
            }
            if (document.columns.isNotEmpty()) {
                cursorY -= 6f
                val columnWidth = (page.mediaBox.width - margin * 2) / document.columns.size.coerceAtLeast(1)
                ensureSpace(18f)
                var offsetX = margin
                document.columns.forEach { column ->
                    content.beginText()
                    content.setFont(boldFont, 10f)
                    content.newLineAtOffset(offsetX, cursorY)
                    content.showText(fitText(column, boldFont, 10f, columnWidth - 4f))
                    content.endText()
                    offsetX += columnWidth
                }
                cursorY -= 16f
                document.rows.forEach { row ->
                    ensureSpace(16f)
                    offsetX = margin
                    document.columns.indices.forEach { columnIndex ->
                        val value = row.getOrNull(columnIndex).orEmpty()
                        content.beginText()
                        content.setFont(regularFont, 9f)
                        content.newLineAtOffset(offsetX, cursorY)
                        content.showText(fitText(value, regularFont, 9f, columnWidth - 4f))
                        content.endText()
                        offsetX += columnWidth
                    }
                    cursorY -= 14f
                }
            }
            content.close()
            pdf.save(target)
        }
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

    private fun fitText(text: String, font: PDFont, fontSize: Float, maxWidth: Float): String {
        if (text.isBlank()) return ""
        if ((font.getStringWidth(text) / 1000f) * fontSize <= maxWidth) return text
        var result = text
        while (result.length > 1 && ((font.getStringWidth("$result…") / 1000f) * fontSize > maxWidth)) {
            result = result.dropLast(1)
        }
        return "$result…"
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun <T> runOnAwtThread(block: () -> T): T {
        if (EventQueue.isDispatchThread()) {
            return block()
        }
        var result: Result<T>? = null
        EventQueue.invokeAndWait {
            result = runCatching(block)
        }
        return result!!.getOrThrow()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '\n' || it == '\r' || it == '"' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun columnName(index: Int): String {
        var current = index
        val builder = StringBuilder()
        do {
            builder.append(('A'.code + (current % 26)).toChar())
            current = current / 26 - 1
        } while (current >= 0)
        return builder.reverse().toString()
    }

    private fun writeQrPng(matrix: QrCodeMatrix, label: String): File {
        val module = maxOf(6, 1024 / (matrix.size + 8))
        val quiet = module * 4
        val imageSize = matrix.size * module + quiet * 2
        val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB)
        val white = 0xFFFFFF
        val black = 0x000000
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                image.setRGB(x, y, white)
            }
        }
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) {
                    val left = quiet + x * module
                    val top = quiet + y * module
                    for (py in top until (top + module)) {
                        for (px in left until (left + module)) {
                            image.setRGB(px, py, black)
                        }
                    }
                }
            }
        }
        val safeName = label.replace(Regex("[^A-Za-z0-9А-Яа-я_-]+"), "-")
        val target = File(storageDirectory, "qr-$safeName-${System.currentTimeMillis()}.png")
        ImageIO.write(image, "png", target)
        return target
    }

    private fun uniqueTarget(directory: File, fileName: String): File {
        return generateSequence(0) { it + 1 }
            .map { index ->
                if (index == 0) {
                    File(directory, fileName)
                } else {
                    val extension = fileName.substringAfterLast('.', "")
                    val baseName = if (extension.isBlank()) fileName else fileName.removeSuffix(".$extension")
                    val resolvedName = if (extension.isBlank()) "$baseName ($index)" else "$baseName ($index).$extension"
                    File(directory, resolvedName)
                }
            }
            .first { !it.exists() }
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
