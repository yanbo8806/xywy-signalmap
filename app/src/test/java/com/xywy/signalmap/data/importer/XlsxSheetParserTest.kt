package com.xywy.signalmap.data.importer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.ParserConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import org.xml.sax.SAXException

class XlsxSheetParserTest {
    @Test
    fun `failed workbook stream copy removes the partial temporary file`() {
        val directory = File(requireNotNull(System.getProperty("java.io.tmpdir")))
        fun temporaryFiles() = directory.listFiles().orEmpty()
            .filter { it.name.startsWith("signalmap-xlsx-") }.map { it.name }.toSet()
        val before = temporaryFiles()
        var closed = false
        val failedInput = object : InputStream() {
            override fun read(): Int = throw IOException("provider disconnected")
            override fun close() { closed = true }
        }
        assertFailsWith<IOException> { XlsxSheetParser().readSheetPreviews(failedInput) }
        assertTrue(closed)
        assertEquals(before, temporaryFiles())
    }

    @Test
    fun `missing sheet xml is reported as absent`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { it.writeEntry("unrelated.xml", "<root/>") }
        assertFalse(XlsxSheetParser().readSheetRows(output.toByteArray().inputStream(), "sheet1", onRow = {}))
    }

    @Test
    fun `worksheet rejects external XML entities`() {
        val bytes = buildWorkbook(WorkbookSheetSpec("unsafe", """
            <!DOCTYPE worksheet [<!ENTITY external SYSTEM "file:///nonexistent-private-file">]>
            <worksheet><sheetData><row><c t="inlineStr"><is><t>&external;</t></is></c></row></sheetData></worksheet>
        """.trimIndent()))
        assertFailsWith<SAXException> { XlsxSheetParser().readSheets(bytes.inputStream()) }
    }

    @Test
    fun `safe feature setter ignores unsupported parser feature and continues`() {
        val applied = mutableListOf<String>()

        ImportXmlSecurity.applyFeatures(
            "broken" to true,
            "supported" to false,
        ) { feature, enabled ->
            if (feature == "broken") throw ParserConfigurationException(feature)
            applied += "$feature=$enabled"
        }

        assertEquals(listOf("supported=false"), applied)
    }

    @Test
    fun `optional parser actions ignore unsupported operations and continue`() {
        val applied = mutableListOf<String>()

        ImportXmlSecurity.applyOptionalActions(
            { throw UnsupportedOperationException("xinclude") },
            { applied += "expand=false" },
        )

        assertEquals(listOf("expand=false"), applied)
    }

    @Test
    fun `read sheet previews only needs the header row`() {
        val workbookBytes = buildWorkbook(
            WorkbookSheetSpec(
                name = "LTE",
                xml = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="inlineStr"><is><t>经度</t></is></c>
                          <c r="B1" t="inlineStr"><is><t>纬度</t></is></c>
                          <c r="C1" t="inlineStr"><is><t>方位角</t></is></c>
                        </row>
                        <row r="2"><c r="A2"><v>120.8</v></c></row>
                      </sheetData>
                    </worksheet>
                """.trimIndent(),
            ),
        )

        val previews = XlsxSheetParser().readSheetPreviews(ByteArrayInputStream(workbookBytes))

        assertEquals(1, previews.size)
        assertEquals("LTE", previews.single().name)
        assertEquals(listOf("经度", "纬度", "方位角"), previews.single().headers)
    }

    @Test
    fun `read sheet rows streams only selected sheet`() {
        val workbookBytes = buildWorkbook(
            WorkbookSheetSpec(
                name = "LTE",
                xml = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="inlineStr"><is><t>经度</t></is></c>
                          <c r="B1" t="inlineStr"><is><t>纬度</t></is></c>
                        </row>
                        <row r="2">
                          <c r="A2"><v>120.8</v></c>
                          <c r="B2"><v>34.4</v></c>
                        </row>
                      </sheetData>
                    </worksheet>
                """.trimIndent(),
            ),
            WorkbookSheetSpec(
                name = "NR",
                xml = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="inlineStr"><is><t>经度</t></is></c>
                          <c r="B1" t="inlineStr"><is><t>纬度</t></is></c>
                        </row>
                        <row r="2">
                          <c r="A2"><v>121.1</v></c>
                          <c r="B2"><v>35.1</v></c>
                        </row>
                      </sheetData>
                    </worksheet>
                """.trimIndent(),
            ),
        )

        val headers = mutableListOf<List<String>>()
        val rows = mutableListOf<Map<String, String>>()

        val found = XlsxSheetParser().readSheetRows(
            inputStream = ByteArrayInputStream(workbookBytes),
            sheetName = "NR",
            onHeaders = { headers += it },
            onRow = { rows += it },
        )

        assertTrue(found)
        assertEquals(listOf(listOf("经度", "纬度")), headers)
        assertEquals(
            listOf(
                mapOf(
                    "经度" to "121.1",
                    "纬度" to "35.1",
                ),
            ),
            rows,
        )
    }

    private fun buildWorkbook(vararg sheets: WorkbookSheetSpec): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.writeEntry(
                "[Content_Types].xml",
                buildString {
                    appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    appendLine("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                    appendLine("""  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                    appendLine("""  <Default Extension="xml" ContentType="application/xml"/>""")
                    appendLine("""  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                    sheets.forEachIndexed { index, _ ->
                        appendLine("""  <Override PartName="/xl/worksheets/sheet${index + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                    }
                    append("""</Types>""")
                },
            )
            zip.writeEntry(
                "xl/workbook.xml",
                buildString {
                    appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    appendLine("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"""")
                    appendLine("""          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
                    appendLine("""  <sheets>""")
                    sheets.forEachIndexed { index, sheet ->
                        appendLine("""    <sheet name="${sheet.name}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
                    }
                    appendLine("""  </sheets>""")
                    append("""</workbook>""")
                },
            )
            zip.writeEntry(
                "xl/_rels/workbook.xml.rels",
                buildString {
                    appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    appendLine("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                    sheets.forEachIndexed { index, _ ->
                        appendLine("""  <Relationship Id="rId${index + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${index + 1}.xml"/>""")
                    }
                    append("""</Relationships>""")
                },
            )
            sheets.forEachIndexed { index, sheet ->
                zip.writeEntry("xl/worksheets/sheet${index + 1}.xml", sheet.xml)
            }
        }
        return out.toByteArray()
    }

    private data class WorkbookSheetSpec(
        val name: String,
        val xml: String,
    )

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
