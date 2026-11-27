package com.xywy.signalmap.data.importer

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipFile

data class ParsedSheet(
    val name: String,
    val rows: List<Map<String, String>>,
)

class XlsxSheetParser {
    fun readRows(inputStream: InputStream): List<Map<String, String>> =
        readSheets(inputStream).firstOrNull()?.rows.orEmpty()

    fun readSheets(inputStream: InputStream): List<ParsedSheet> {
        val rowsBySheet = linkedMapOf<String, MutableList<Map<String, String>>>()
        forEachSheetRow(
            inputStream = inputStream,
            onHeaders = { sheetName, _ ->
                rowsBySheet.putIfAbsent(sheetName, mutableListOf())
            },
            onRow = { sheetName, row ->
                rowsBySheet.getOrPut(sheetName) { mutableListOf() } += row
            },
        )
        return rowsBySheet.map { (sheetName, rows) ->
            ParsedSheet(name = sheetName, rows = rows)
        }
    }

    fun readSheetPreviews(inputStream: InputStream): List<ParsedSheetPreview> {
        val previews = mutableListOf<ParsedSheetPreview>()
        withWorkbookArchive(inputStream) { archive ->
            archive.sheets.forEach { sheet ->
                archive.openSheet(sheet.path)?.use { sheetStream ->
                    previews += ParsedSheetPreview(
                        name = sheet.name,
                        headers = parseSheetHeaders(sheetStream, archive.sharedStrings),
                    )
                }
            }
        }
        return previews
    }

    fun readSheet(inputStream: InputStream, sheetName: String): ParsedSheet? {
        val rows = mutableListOf<Map<String, String>>()
        val found = readSheetRows(
            inputStream = inputStream,
            sheetName = sheetName,
            onRow = { rows += it },
        )
        return if (found) ParsedSheet(name = sheetName, rows = rows) else null
    }

    fun readSheetRows(
        inputStream: InputStream,
        sheetName: String,
        onHeaders: (List<String>) -> Unit = {},
        onRow: (Map<String, String>) -> Unit,
    ): Boolean = withWorkbookArchive(inputStream) { archive ->
        val targetSheet = archive.sheets.firstOrNull { it.name == sheetName } ?: return@withWorkbookArchive false
        val sheetStream = archive.openSheet(targetSheet.path) ?: return@withWorkbookArchive false
        sheetStream.use {
            parseSheetXml(
                inputStream = sheetStream,
                sharedStrings = archive.sharedStrings,
                stopAfterHeaderRow = false,
                onHeaders = onHeaders,
                onRow = onRow,
            )
        }
        true
    }

    fun forEachSheetRow(
        inputStream: InputStream,
        selectedSheetName: String? = null,
        onHeaders: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onRow: (sheetName: String, row: Map<String, String>) -> Unit,
    ): List<String> = withWorkbookArchive(inputStream) { archive ->
        val targetSheets = if (selectedSheetName.isNullOrBlank()) {
            archive.sheets
        } else {
            archive.sheets.filter { it.name == selectedSheetName }
        }
        targetSheets.mapNotNull { sheet ->
            archive.openSheet(sheet.path)?.use { sheetStream ->
                parseSheetXml(
                    inputStream = sheetStream,
                    sharedStrings = archive.sharedStrings,
                    stopAfterHeaderRow = false,
                    onHeaders = { headers -> onHeaders(sheet.name, headers) },
                    onRow = { row -> onRow(sheet.name, row) },
                )
            } ?: return@mapNotNull null
            sheet.name
        }
    }

    private fun parseSheetHeaders(
        inputStream: InputStream,
        sharedStrings: List<String>,
    ): List<String> {
        var headers = emptyList<String>()
        parseSheetXml(
            inputStream = inputStream,
            sharedStrings = sharedStrings,
            stopAfterHeaderRow = true,
            onHeaders = { headers = it },
            onRow = {},
        )
        return headers
    }

    private fun parseSheetRows(
        inputStream: InputStream,
        sharedStrings: List<String>,
    ): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        parseSheetXml(
            inputStream = inputStream,
            sharedStrings = sharedStrings,
            stopAfterHeaderRow = false,
            onHeaders = {},
            onRow = { rows += it },
        )
        return rows
    }

    private fun parseSheetXml(
        inputStream: InputStream,
        sharedStrings: List<String>,
        stopAfterHeaderRow: Boolean,
        onHeaders: (List<String>) -> Unit,
        onRow: (Map<String, String>) -> Unit,
    ) {
        val handler = SheetXmlHandler(
            sharedStrings = sharedStrings,
            stopAfterHeaderRow = stopAfterHeaderRow,
            onHeaders = onHeaders,
            onRow = onRow,
        )
        parseXml(inputStream, handler)
    }

    private fun readSharedStrings(zipFile: ZipFile): List<String> {
        val entry = zipFile.getEntry(SHARED_STRINGS_PATH) ?: return emptyList()
        return zipFile.getInputStream(entry).use { input ->
            val handler = SharedStringsHandler()
            parseXml(input, handler)
            handler.values
        }
    }

    private fun resolveSheetEntries(zipFile: ZipFile): List<WorkbookSheetEntry> {
        val workbookEntry = zipFile.getEntry(WORKBOOK_PATH)
        val relationsEntry = zipFile.getEntry(WORKBOOK_RELS_PATH)
        if (workbookEntry == null || relationsEntry == null) {
            return listOf(WorkbookSheetEntry(name = "sheet1", path = DEFAULT_SHEET_PATH))
        }

        val workbookDocument = zipFile.getInputStream(workbookEntry).use(::parseSmallXml)
        val relationDocument = zipFile.getInputStream(relationsEntry).use(::parseSmallXml)
        val relationTargets = relationDocument.elementsByLocalName("Relationship")
            .mapNotNull { relation ->
                relation.getAttribute("Id")
                    .takeIf(String::isNotBlank)
                    ?.let { it to relation.getAttribute("Target") }
            }
            .toMap()

        val sheets = workbookDocument.elementsByLocalName("sheet").mapNotNullIndexed { index, sheet ->
            val relationId = sequenceOf(
                sheet.getAttributeNS(OFFICE_REL_NS, "id"),
                sheet.getAttribute("r:id"),
                sheet.getAttribute("id"),
            ).firstOrNull { it.isNotBlank() } ?: return@mapNotNullIndexed null
            val target = relationTargets[relationId] ?: return@mapNotNullIndexed null
            WorkbookSheetEntry(
                name = sheet.getAttribute("name").ifBlank { "sheet${index + 1}" },
                path = normalizeSheetPath(target),
            )
        }
        return sheets.ifEmpty { listOf(WorkbookSheetEntry(name = "sheet1", path = DEFAULT_SHEET_PATH)) }
    }

    private fun normalizeSheetPath(target: String): String = when {
        target.startsWith("xl/") -> target
        target.startsWith("/") -> target.removePrefix("/")
        target.startsWith("../") -> "xl/${target.removePrefix("../")}".replace("//", "/")
        else -> "xl/$target"
    }

    private fun parseXml(inputStream: InputStream, handler: DefaultHandler) {
        val parser = ImportXmlSecurity.newXmlReader()
        parser.contentHandler = handler
        try {
            parser.parse(InputSource(inputStream))
        } catch (stop: StopParsingException) {
            // preview mode intentionally aborts after reading the header row
        }
    }

    private fun parseSmallXml(inputStream: InputStream): Document =
        ImportXmlSecurity.newDocumentBuilder().parse(inputStream)

    private fun <T> withWorkbookArchive(inputStream: InputStream, block: (WorkbookArchive) -> T): T {
        val tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
        try {
            inputStream.use { source ->
                Files.newOutputStream(tempFile).use(source::copyTo)
            }
            ZipFile(tempFile.toFile()).use { zipFile ->
                val archive = WorkbookArchive(
                    zipFile = zipFile,
                    sheets = resolveSheetEntries(zipFile),
                    sharedStrings = readSharedStrings(zipFile),
                )
                return block(archive)
            }
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    private data class WorkbookArchive(
        val zipFile: ZipFile,
        val sheets: List<WorkbookSheetEntry>,
        val sharedStrings: List<String>,
    ) {
        fun openSheet(path: String): InputStream? = zipFile.getEntry(path)?.let(zipFile::getInputStream)
    }

    private data class WorkbookSheetEntry(
        val name: String,
        val path: String,
    )

    private class SharedStringsHandler : DefaultHandler() {
        val values = mutableListOf<String>()

        private var currentText: StringBuilder? = null
        private var insideTextNode = false

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            when (elementName(localName, qName)) {
                "si" -> currentText = StringBuilder()
                "t" -> insideTextNode = currentText != null
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (insideTextNode) currentText?.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (elementName(localName, qName)) {
                "t" -> insideTextNode = false
                "si" -> {
                    values += currentText?.toString().orEmpty()
                    currentText = null
                }
            }
        }
    }

    private class SheetXmlHandler(
        private val sharedStrings: List<String>,
        private val stopAfterHeaderRow: Boolean,
        private val onHeaders: (List<String>) -> Unit,
        private val onRow: (Map<String, String>) -> Unit,
    ) : DefaultHandler() {
        private val headersByColumn = mutableMapOf<Int, String>()
        private var currentRowValues = linkedMapOf<Int, String>()
        private var currentColumn = 0
        private var nextFallbackColumn = 0
        private var currentType = ""
        private var currentValue = StringBuilder()
        private var readingValueNode = false

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            when (elementName(localName, qName)) {
                "row" -> {
                    currentRowValues = linkedMapOf()
                    nextFallbackColumn = 0
                }

                "c" -> {
                    val reference = attributes?.getValue("r").orEmpty()
                    currentColumn = if (reference.isNotBlank()) columnIndex(reference) else nextFallbackColumn
                    nextFallbackColumn = currentColumn + 1
                    currentType = attributes?.getValue("t").orEmpty()
                    currentValue = StringBuilder()
                    readingValueNode = false
                }

                "v" -> readingValueNode = true
                "t" -> if (currentType == "inlineStr") readingValueNode = true
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (readingValueNode) currentValue.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (elementName(localName, qName)) {
                "v" -> readingValueNode = false
                "t" -> if (currentType == "inlineStr") readingValueNode = false
                "c" -> currentRowValues[currentColumn] = resolveCellValue(currentType, currentValue.toString()).trim()
                "row" -> flushRow()
            }
        }

        private fun flushRow() {
            if (currentRowValues.isEmpty()) return
            if (headersByColumn.isEmpty()) {
                val headers = buildList {
                    currentRowValues.toSortedMap().forEach { (column, value) ->
                        headersByColumn[column] = value
                        if (value.isNotBlank()) add(value)
                    }
                }
                onHeaders(headers)
                if (stopAfterHeaderRow) throw StopParsingException()
                return
            }
            val row = buildMap {
                currentRowValues.toSortedMap().forEach { (column, value) ->
                    val header = headersByColumn[column]?.takeIf { it.isNotBlank() } ?: return@forEach
                    put(header, value)
                }
            }
            if (row.isNotEmpty()) onRow(row)
        }

        private fun resolveCellValue(type: String, value: String): String = when (type) {
            "s" -> value.toIntOrNull()?.let(sharedStrings::getOrNull).orEmpty()
            else -> value
        }
    }

    private class StopParsingException : SAXException()

    companion object {
        private const val WORKBOOK_PATH = "xl/workbook.xml"
        private const val WORKBOOK_RELS_PATH = "xl/_rels/workbook.xml.rels"
        private const val SHARED_STRINGS_PATH = "xl/sharedStrings.xml"
        private const val DEFAULT_SHEET_PATH = "xl/worksheets/sheet1.xml"
        private const val OFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        private const val TEMP_FILE_PREFIX = "signalmap-xlsx-"
        private const val TEMP_FILE_SUFFIX = ".xlsx"

        private fun Document.elementsByLocalName(localName: String): List<Element> {
            val namespaced = getElementsByTagNameNS("*", localName)
            val source = if (namespaced.length > 0) namespaced else getElementsByTagName(localName)
            return buildList(source.length) {
                for (index in 0 until source.length) {
                    val element = source.item(index) as? Element ?: continue
                    add(element)
                }
            }
        }

        private inline fun <T, R : Any> Iterable<T>.mapNotNullIndexed(transform: (Int, T) -> R?): List<R> {
            val output = mutableListOf<R>()
            forEachIndexed { index, item ->
                transform(index, item)?.let(output::add)
            }
            return output
        }

        private fun elementName(localName: String?, qName: String?): String = localName?.takeIf { it.isNotBlank() } ?: qName.orEmpty()

        private fun columnIndex(reference: String): Int {
            val letters = reference.takeWhile { it.isLetter() }
            var value = 0
            letters.forEach { char ->
                value = (value * 26) + (char.uppercaseChar().code - 'A'.code + 1)
            }
            return (value - 1).coerceAtLeast(0)
        }
    }
}
