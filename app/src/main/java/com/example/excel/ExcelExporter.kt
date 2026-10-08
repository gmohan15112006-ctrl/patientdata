package com.example.excel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.PatientEntity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    val REQUIRED_HEADERS = listOf(
        "S NO",
        "IP NO",
        "NAME",
        "AGE",
        "SEX",
        "ADMISSION DATE & TIME",
        "PATIENT RECEIVED TIME",
        "BROAD SPECIALITY CATEGORY",
        "DIAGNOSIS",
        "AGE INTERVAL",
        "TAEI PILLAR/TAEI NON PILLAR",
        "MEDICOLEGAL CATEGORY",
        "TRANSFERRED OUT",
        "TRANSFERRED OUT TIME",
        "EMERGENCY RESPONSE TIME"
    )

    data class ExportSheet(
        val sheetName: String,
        val patients: List<PatientEntity>
    )

    /**
     * Generates a genuine .xlsx OpenXML spreadsheet file.
     * Supports single sheet or multiple sheets grouped by date.
     */
    fun generateExcelFile(
        context: Context,
        fileName: String,
        sheets: List<ExportSheet>
    ): File {
        val safeName = if (fileName.endsWith(".xlsx", ignoreCase = true)) fileName else "$fileName.xlsx"
        val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val targetFile = File(exportDir, safeName)

        val fos = FileOutputStream(targetFile)
        val zos = ZipOutputStream(fos)

        val safeSheets = if (sheets.isEmpty()) {
            listOf(ExportSheet("Patients", emptyList()))
        } else {
            sheets.map { sheet ->
                // Clean sheet name (max 31 chars, no invalid chars like : \ / ? * [ ])
                val cleanedName = sheet.sheetName
                    .replace(Regex("[:\\\\/?*\\[\\]]"), "-")
                    .take(30)
                    .ifBlank { "Sheet1" }
                ExportSheet(cleanedName, sheet.patients)
            }
        }

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypesXml = buildContentTypesXml(safeSheets.size)
        zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
        zos.write(relsXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        val workbookRelsXml = buildWorkbookRelsXml(safeSheets.size)
        zos.write(workbookRelsXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 4. xl/styles.xml
        zos.putNextEntry(ZipEntry("xl/styles.xml"))
        val stylesXml = buildStylesXml()
        zos.write(stylesXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 5. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        val workbookXml = buildWorkbookXml(safeSheets)
        zos.write(workbookXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 6. Each worksheet: xl/worksheets/sheetN.xml
        safeSheets.forEachIndexed { index, sheet ->
            val sheetNumber = index + 1
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet$sheetNumber.xml"))
            val sheetXml = buildWorksheetXml(sheet.patients)
            zos.write(sheetXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        zos.flush()
        zos.close()
        fos.close()

        return targetFile
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sheetOverrides = (1..sheetCount).joinToString("\n") { index ->
            """<Override PartName="/xl/worksheets/sheet$index.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    $sheetOverrides
</Types>"""
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sheetRels = (1..sheetCount).joinToString("\n") { index ->
            """<Relationship Id="rId$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$index.xml"/>"""
        }
        val stylesRelId = "rId${sheetCount + 1}"
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    $sheetRels
    <Relationship Id="$stylesRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookXml(sheets: List<ExportSheet>): String {
        val sheetTags = sheets.mapIndexed { index, sheet ->
            """<sheet name="${escapeXml(sheet.sheetName)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        $sheetTags
    </sheets>
</workbook>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font>
            <sz val="10"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="10"/>
            <color rgb="FFFFFFFF"/>
            <name val="Calibri"/>
        </font>
    </fonts>
    <fills count="3">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill>
            <patternFill patternType="solid">
                <fgColor rgb="FF006876"/>
            </patternFill>
        </fill>
    </fills>
    <borders count="2">
        <border>
            <left/><right/><top/><bottom/><diagonal/>
        </border>
        <border>
            <left style="thin"><color rgb="FFCBD5E1"/></left>
            <right style="thin"><color rgb="FFCBD5E1"/></right>
            <top style="thin"><color rgb="FFCBD5E1"/></top>
            <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
        </border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="4">
        <!-- 0: Default Normal -->
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1"/>
        <!-- 1: Header (Bold, Teal fill, white text, centered) -->
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center" wrapText="1"/>
        </xf>
        <!-- 2: Data text (left aligned) -->
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="left" vertical="center"/>
        </xf>
        <!-- 3: Data centered (S.No, Age, Sex, Codes) -->
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
    </cellXfs>
</styleSheet>"""
    }

    private fun columnLetter(colIdx: Int): String {
        val sb = StringBuilder()
        var num = colIdx
        while (num > 0) {
            val rem = (num - 1) % 26
            sb.append(('A'.code + rem).toChar())
            num = (num - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun buildWorksheetXml(patients: List<PatientEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        
        // Frozen top header row
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0">""")
        sb.append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
        sb.append("""</sheetView></sheetViews>""")

        // Column widths
        sb.append("<cols>")
        val colWidths = listOf(8, 14, 22, 8, 10, 22, 22, 26, 30, 15, 26, 22, 18, 20, 24)
        colWidths.forEachIndexed { i, w ->
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Row 1: Header
        sb.append("""<row r="1" ht="28" customHeight="1">""")
        REQUIRED_HEADERS.forEachIndexed { index, header ->
            val cellRef = "${columnLetter(index + 1)}1"
            sb.append("""<c r="$cellRef" t="inlineStr" s="1"><is><t>${escapeXml(header)}</t></is></c>""")
        }
        sb.append("</row>")

        // Patient rows
        patients.forEachIndexed { pIndex, p ->
            val rowNum = pIndex + 2
            sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")

            val values = listOf(
                Pair(p.serial_no.toString(), 3), // S NO (centered)
                Pair(p.ip_no, 3), // IP NO
                Pair(p.name, 2), // NAME
                Pair(p.age.toString(), 3), // AGE
                Pair(p.sex, 3), // SEX
                Pair(p.admission_datetime, 3), // ADMISSION DATE & TIME
                Pair(p.patient_received_time, 3), // PATIENT RECEIVED TIME
                Pair(p.broad_speciality_category, 2), // BROAD SPECIALITY CATEGORY
                Pair(p.diagnosis, 2), // DIAGNOSIS
                Pair(p.age_interval, 3), // AGE INTERVAL
                Pair(p.taei_category, 2), // TAEI PILLAR/TAEI NON PILLAR
                Pair(p.medicolegal_category, 3), // MEDICOLEGAL CATEGORY
                Pair(p.transferred_out, 3), // TRANSFERRED OUT
                Pair(p.transferred_out_time, 3), // TRANSFERRED OUT TIME
                Pair(p.emergency_response_time, 3) // EMERGENCY RESPONSE TIME
            )

            values.forEachIndexed { cIndex, pair ->
                val (text, styleId) = pair
                val cellRef = "${columnLetter(cIndex + 1)}$rowNum"
                sb.append("""<c r="$cellRef" t="inlineStr" s="$styleId"><is><t>${escapeXml(text)}</t></is></c>""")
            }

            sb.append("</row>")
        }

        sb.append("</sheetData>")

        // Auto-filter
        val lastRow = maxOf(patients.size + 1, 1)
        sb.append("""<autoFilter ref="A1:O$lastRow"/>""")

        sb.append("</worksheet>")
        return sb.toString()
    }

    /**
     * Creates an Intent to share or open the generated Excel file in apps like
     * Microsoft Excel, Google Sheets, Drive, or Gmail.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
