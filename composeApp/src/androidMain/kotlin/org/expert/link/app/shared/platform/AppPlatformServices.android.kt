package org.expert.link.app.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.expert.link.app.android.buildAndroidBarcode
import org.expert.link.app.android.AndroidFilePicker
import org.expert.link.app.android.AndroidMulticastSupport
import org.expert.link.app.android.AndroidQrScanner
import org.expert.link.app.android.AndroidWebRtcMediaEngineAdapter
import org.expert.link.app.android.buildAndroidQrCode
import org.expert.link.database.createPersistentRepositoryBundle
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import kotlinx.serialization.json.Json

private object AndroidPlatformContextHolder {
    @Volatile
    private var context: Context? = null

    fun set(value: Context) {
        context = value.applicationContext
    }

    fun require(): Context {
        return requireNotNull(context) { "Android context is not initialized. Call initializeAppPlatformContext() before AppPlatformServices." }
    }
}

/** Инициализирует Android context для actual-сервисов платформы. */
fun initializeAppPlatformContext(context: Context) {
    AndroidPlatformContextHolder.set(context)
}

/** Android actual-реализация платформенных сервисов client-модуля. */
actual class AppPlatformServices actual constructor(
    @Suppress("UNUSED_PARAMETER")
    private val args: List<String>,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val context: Context
        get() = AndroidPlatformContextHolder.require()

    private val mediaEngine by lazy { AndroidWebRtcMediaEngineAdapter(context) }
    private val multicastSupport by lazy { AndroidMulticastSupport { context } }
    private val persistentRepositories by lazy {
        createPersistentRepositoryBundle(
            context = context,
            name = context.getDatabasePath("expert-link.db").absolutePath,
        )
    }

    actual val platformName: String = "Android"
    actual val transportHint: String = "Полная сеть"
    actual val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = true,
        canShareFiles = true,
        canSaveFiles = true,
        canPrintFiles = false,
        canOpenFiles = true,
        canRenderQr = true,
        canRenderBarcode = true,
        canScanQr = true,
        canPickFile = true,
        prefersWideLayout = false,
    )

    actual fun defaultConfig(): MeshNodeConfig {
        val base = createDefaultNodeConfig(
            displayNamePrefix = "android",
            realTransport = true,
            realDiscovery = true,
        )
        val persisted = context
            .getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getString(CONFIG_KEY, null)
            ?.let { encoded ->
                runCatching { json.decodeFromString(MeshNodeConfig.serializer(), encoded) }.getOrNull()
            }
        val resolved = persisted ?: base
        return resolved.copy(
            fileTransfer = resolved.fileTransfer.copy(
                downloadDirectory = context.filesDir.resolve("expert-link/downloads").absolutePath,
            ),
        )
    }

    actual fun persistConfig(config: MeshNodeConfig) {
        val encoded = json.encodeToString(MeshNodeConfig.serializer(), config)
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, encoded)
            .apply()
    }

    actual suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(
        configuration = config,
        mediaEngine = mediaEngine,
        multicastSupport = multicastSupport,
        persistentRepositories = persistentRepositories,
    )

    actual suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    actual suspend fun shareText(label: String, text: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    actual suspend fun shareFile(label: String, path: String): Result<Unit> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val uri = context.shareableUri(source)
        val mimeType = source.mimeType()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, source.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    actual suspend fun saveFileToDownloads(path: String, fileName: String): Result<String?> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val mimeType = source.mimeType()
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
            ) { "Не удалось создать запись в загрузках" }
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Не удалось открыть файл загрузки")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: error("Папка загрузок недоступна")
            downloadsDir.mkdirs()
            val target = uniqueTarget(downloadsDir, fileName)
            source.copyTo(target)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeType), null)
            target.absolutePath
        }
    }

    actual suspend fun openFile(path: String): Result<Unit> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val uri = context.shareableUri(source)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, source.mimeType())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    actual suspend fun cacheLocalArtifact(path: String, fileName: String): Result<String> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val artifactsDir = context.filesDir.resolve("expert-link/artifacts").apply { mkdirs() }
        val target = File(artifactsDir, fileName)
        source.copyTo(target, overwrite = true)
        target.absolutePath
    }

    actual suspend fun createInventoryExportArtifact(
        document: InventoryExportDocument,
        format: MeshInventoryExportFormat,
        suggestedFileName: String,
    ): Result<LocalArtifact> = runCatching {
        val exportsDir = context.filesDir.resolve("expert-link/exports").apply { mkdirs() }
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
        val candidates = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            context.cacheDir,
        )
        candidates
            .asSequence()
            .flatMap { root -> root.walkTopDown().asSequence() }
            .firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) }
            ?.absolutePath
    }

    actual suspend fun printFile(path: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("Системная печать на Android не поддерживается в этом клиенте"),
    )

    actual suspend fun pickFile(): Result<String?> = runCatching {
        AndroidFilePicker.pick(context)
    }

    actual suspend fun describeFile(path: String): Result<MeshFileDescriptor> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        MeshFileDescriptor(
            fileId = "file-${UUID.randomUUID()}",
            fileName = source.name,
            sizeBytes = source.length(),
            sha256 = source.inputStream().use { stream -> sha256Hex(stream) },
            contentType = source.mimeType(),
        )
    }

    actual suspend fun scanQr(): Result<String?> = runCatching {
        AndroidQrScanner.scan()
    }

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildAndroidQrCode(text)

    actual fun buildBarcode(text: String): BarcodeMatrix? = buildAndroidBarcode(text)

    actual suspend fun shareQrImage(label: String, text: String): Result<Unit> = runCatching {
        val matrix = buildQrCode(text) ?: error("Не удалось сформировать QR")
        val file = writeQrPng(matrix, label)
        shareFile(label, file.absolutePath).getOrThrow()
    }

    private fun Context.shareableUri(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun File.mimeType(): String =
        URLConnection.guessContentTypeFromName(name)?.ifBlank { null } ?: "application/octet-stream"

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
        val pdf = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
        }
        val width = 1123
        val height = 794
        val margin = 42f
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        var cursorY = margin + titlePaint.textSize

        fun startPage() {
            pdf.finishPage(page)
            pageNumber += 1
            pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            cursorY = margin + titlePaint.textSize
        }

        fun drawLine(text: String, paint: Paint) {
            val lineHeight = paint.textSize + 10f
            if (cursorY + lineHeight > height - margin) {
                startPage()
            }
            canvas.drawText(text, margin, cursorY, paint)
            cursorY += lineHeight
        }

        drawLine(document.title, titlePaint)
        document.subtitle?.takeIf { it.isNotBlank() }?.let { drawLine(it, bodyPaint) }
        if (document.summary.isNotEmpty()) {
            cursorY += 8f
            document.summary.forEach { (label, value) ->
                drawLine("$label: $value", bodyPaint)
            }
        }
        if (document.columns.isNotEmpty()) {
            cursorY += 12f
            drawLine(document.columns.joinToString(" | "), bodyPaint.apply { isFakeBoldText = true })
            bodyPaint.isFakeBoldText = false
            document.rows.forEach { row ->
                drawLine(row.joinToString(" | "), bodyPaint)
            }
        }

        pdf.finishPage(page)
        target.outputStream().use { output -> pdf.writeTo(output) }
        pdf.close()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
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
        val bitmapSize = matrix.size * module + quiet * 2
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) {
                    val left = quiet + x * module
                    val top = quiet + y * module
                    canvas.drawRect(
                        left.toFloat(),
                        top.toFloat(),
                        (left + module).toFloat(),
                        (top + module).toFloat(),
                        paint,
                    )
                }
            }
        }
        val safeName = label.replace(Regex("[^A-Za-z0-9А-Яа-я_-]+"), "-")
        val target = File(context.cacheDir, "qr-$safeName-${System.currentTimeMillis()}.png")
        target.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Не удалось сохранить изображение QR"
            }
        }
        return target
    }

    private companion object {
        const val CONFIG_PREFS = "expert-link-config"
        const val CONFIG_KEY = "mesh-node-config"
    }
}
