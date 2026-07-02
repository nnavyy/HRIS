package com.ptniger.hris.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ptniger.hris.data.model.Payroll
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * Professional B&W Payroll Slip PDF Generator.
 * Generates A4 monochrome PDF with:
 *   - Company header from OfficeLocation profile
 *   - Structured payroll table
 *   - 3-column signature block (Karyawan / Manager / HRD)
 */
object PdfSlipGenerator {

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    data class CompanyInfo(
        val name: String = "PT NIGER",
        val address: String = "",
        val phone: String = "",
        val email: String = "",
        val npwp: String = ""
    )

    data class SignatureInfo(
        val employeeName: String = "",
        val managerName: String = "",
        val hrName: String = ""
    )

    fun generateAndOpen(
        context: Context,
        payroll: Payroll,
        employeeName: String,
        nik: String = "",
        position: String = "",
        department: String = "",
        company: CompanyInfo = CompanyInfo(),
        signatures: SignatureInfo = SignatureInfo(employeeName = employeeName)
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawSlipContent(canvas, payroll, employeeName, nik, position, department, company, signatures)

            document.finishPage(page)

            val fileName = "SlipGaji_${employeeName.replace(" ", "_")}_${payroll.month}_${payroll.year}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    document.close()
                    Toast.makeText(context, "Slip gaji berhasil diunduh!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {
                        Toast.makeText(context, "File tersimpan di folder Downloads", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { document.writeTo(it) }
                document.close()
                Toast.makeText(context, "Slip gaji berhasil diunduh!", Toast.LENGTH_SHORT).show()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(intent) } catch (_: Exception) {
                    Toast.makeText(context, "File tersimpan di folder Downloads", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawSlipContent(
        canvas: Canvas, p: Payroll, employeeName: String,
        nik: String, position: String, department: String,
        company: CompanyInfo, signatures: SignatureInfo
    ) {
        val pw = 595f
        val ml = 40f   // margin left
        val mr = 555f   // margin right
        var y = 0f

        // ── Paints ──
        val black = Color.BLACK
        val gray = Color.parseColor("#666666")
        val lightGray = Color.parseColor("#E0E0E0")

        val titlePaint = paint(black, 16f, bold = true)
        val companyPaint = paint(black, 14f, bold = true)
        val subtitlePaint = paint(gray, 9f)
        val sectionPaint = paint(black, 10f, bold = true)
        val normalPaint = paint(black, 9.5f)
        val normalGray = paint(gray, 9f)
        val boldPaint = paint(black, 9.5f, bold = true)
        val valueRight = paint(black, 9.5f, align = Paint.Align.RIGHT)
        val boldRight = paint(black, 10f, bold = true, align = Paint.Align.RIGHT)
        val linePaint = Paint().apply { color = black; strokeWidth = 0.8f }
        val thinLine = Paint().apply { color = lightGray; strokeWidth = 0.5f }
        val thickLine = Paint().apply { color = black; strokeWidth = 2f }

        // ═══════════════════════════════════════════════════════════
        //  HEADER — Company Info
        // ═══════════════════════════════════════════════════════════
        y = 30f
        canvas.drawLine(ml, y, mr, y, thickLine)
        y += 22f

        val cName = company.name.ifEmpty { "PT NIGER" }
        canvas.drawText(cName, ml, y, companyPaint)
        y += 14f

        if (company.address.isNotEmpty()) {
            canvas.drawText(company.address, ml, y, subtitlePaint)
            y += 12f
        }
        val contactParts = mutableListOf<String>()
        if (company.phone.isNotEmpty()) contactParts.add("Telp: ${company.phone}")
        if (company.email.isNotEmpty()) contactParts.add("Email: ${company.email}")
        if (contactParts.isNotEmpty()) {
            canvas.drawText(contactParts.joinToString("  |  "), ml, y, subtitlePaint)
            y += 12f
        }
        if (company.npwp.isNotEmpty()) {
            canvas.drawText("NPWP: ${company.npwp}", ml, y, subtitlePaint)
            y += 12f
        }

        y += 4f
        canvas.drawLine(ml, y, mr, y, thickLine)
        y += 20f

        // ═══════════════════════════════════════════════════════════
        //  TITLE
        // ═══════════════════════════════════════════════════════════
        val titleCenter = paint(black, 14f, bold = true, align = Paint.Align.CENTER)
        canvas.drawText("SLIP GAJI KARYAWAN", pw / 2, y, titleCenter)
        y += 16f
        val periodCenter = paint(gray, 10f, align = Paint.Align.CENTER)
        canvas.drawText("Periode: ${DateUtils.formatMonthYear(p.month, p.year)}", pw / 2, y, periodCenter)
        y += 6f

        val statusText = when (p.status) {
            "paid" -> "LUNAS"
            "finalized" -> "FINAL"
            "approved" -> "DISETUJUI"
            "pending_approval" -> "MENUNGGU APPROVAL"
            else -> p.status.uppercase()
        }
        val statusCenter = paint(gray, 8f, align = Paint.Align.CENTER)
        canvas.drawText("Status: $statusText", pw / 2, y + 12f, statusCenter)
        y += 24f

        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 16f

        // ═══════════════════════════════════════════════════════════
        //  EMPLOYEE INFO (2-column table)
        // ═══════════════════════════════════════════════════════════
        canvas.drawText("INFORMASI KARYAWAN", ml, y, sectionPaint)
        y += 16f

        val labelX = ml + 4f
        val valX = ml + 120f
        val labelX2 = pw / 2 + 10f
        val valX2 = pw / 2 + 120f

        // Row 1
        canvas.drawText("Nama Lengkap", labelX, y, normalGray)
        canvas.drawText(": ${employeeName.ifEmpty { p.employeeName }}", valX, y, boldPaint)
        canvas.drawText("Departemen", labelX2, y, normalGray)
        canvas.drawText(": ${department.ifEmpty { p.departmentId.ifEmpty { "-" } }}", valX2, y, boldPaint)
        y += 15f

        // Row 2
        canvas.drawText("NIK", labelX, y, normalGray)
        canvas.drawText(": ${nik.ifEmpty { "-" }}", valX, y, boldPaint)
        canvas.drawText("Jabatan", labelX2, y, normalGray)
        canvas.drawText(": ${position.ifEmpty { "-" }}", valX2, y, boldPaint)
        y += 15f

        // Row 3
        canvas.drawText("PTKP", labelX, y, normalGray)
        canvas.drawText(": ${p.ptkpStatus}", valX, y, boldPaint)
        canvas.drawText("Kontrak", labelX2, y, normalGray)
        canvas.drawText(": ${p.contractId.ifEmpty { "-" }}", valX2, y, boldPaint)
        y += 20f

        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 16f

        // ═══════════════════════════════════════════════════════════
        //  TABLE HEADER: Keterangan | Pendapatan | Potongan
        // ═══════════════════════════════════════════════════════════
        val colDesc = ml + 4f
        val colIncome = mr - 140f
        val colDeduct = mr

        // Table header background
        val headerBg = Paint().apply { color = Color.parseColor("#F0F0F0") }
        canvas.drawRect(ml, y - 4f, mr, y + 14f, headerBg)
        canvas.drawRect(ml, y - 4f, mr, y + 14f, Paint().apply { color = black; style = Paint.Style.STROKE; strokeWidth = 0.8f })

        canvas.drawText("Keterangan", colDesc, y + 10f, sectionPaint)
        val headerRight1 = paint(black, 10f, bold = true, align = Paint.Align.RIGHT)
        canvas.drawText("Pendapatan", colIncome, y + 10f, headerRight1)
        canvas.drawText("Potongan", colDeduct, y + 10f, headerRight1)
        y += 18f

        // ═══════════════════════════════════════════════════════════
        //  PENDAPATAN ROWS
        // ═══════════════════════════════════════════════════════════
        y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "Gaji Pokok", currencyFmt.format(p.baseSalary), "", normalPaint, valueRight, thinLine, ml, mr)

        val totalAllowance = p.allowanceMeal + p.allowanceTransport + p.allowancePosition
        if (p.allowanceMeal > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "  Tunjangan Makan", currencyFmt.format(p.allowanceMeal), "", normalPaint, valueRight, thinLine, ml, mr)
        if (p.allowanceTransport > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "  Tunjangan Transport", currencyFmt.format(p.allowanceTransport), "", normalPaint, valueRight, thinLine, ml, mr)
        if (p.allowancePosition > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "  Tunjangan Jabatan", currencyFmt.format(p.allowancePosition), "", normalPaint, valueRight, thinLine, ml, mr)

        if (p.overtimePay > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "Lembur (${p.overtimeHours} jam)", currencyFmt.format(p.overtimePay), "", normalPaint, valueRight, thinLine, ml, mr)
        if (p.kpiBonus > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "Bonus KPI (Skor: ${String.format("%.1f", p.kpiScore)})", currencyFmt.format(p.kpiBonus), "", normalPaint, valueRight, thinLine, ml, mr)

        // ═══════════════════════════════════════════════════════════
        //  POTONGAN ROWS
        // ═══════════════════════════════════════════════════════════
        if (p.bpjsKesehatan > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "BPJS Kesehatan (1%)", "", currencyFmt.format(p.bpjsKesehatan), normalPaint, valueRight, thinLine, ml, mr)
        if (p.bpjsJht > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "BPJS JHT (2%)", "", currencyFmt.format(p.bpjsJht), normalPaint, valueRight, thinLine, ml, mr)
        if (p.bpjsJp > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "BPJS JP (1%)", "", currencyFmt.format(p.bpjsJp), normalPaint, valueRight, thinLine, ml, mr)
        if (p.pph21 > 0) y = tableRow(canvas, colDesc, colIncome, colDeduct, y, "PPh 21", "", currencyFmt.format(p.pph21), normalPaint, valueRight, thinLine, ml, mr)

        // ═══════════════════════════════════════════════════════════
        //  INFO TANGGUNGAN PERUSAHAAN (non-deduction)
        // ═══════════════════════════════════════════════════════════
        if (p.bpjsJkk > 0 || p.bpjsJkm > 0) {
            y += 4f
            canvas.drawLine(ml, y, mr, y, thinLine)
            y += 14f
            canvas.drawText("Iuran Ditanggung Perusahaan (informasi):", colDesc, y, normalGray)
            y += 14f
            if (p.bpjsJkk > 0) { canvas.drawText("  BPJS JKK (0.24%)", colDesc, y, normalGray); canvas.drawText(currencyFmt.format(p.bpjsJkk), colIncome, y, paint(gray, 9f, align = Paint.Align.RIGHT)); y += 14f }
            if (p.bpjsJkm > 0) { canvas.drawText("  BPJS JKM (0.3%)", colDesc, y, normalGray); canvas.drawText(currencyFmt.format(p.bpjsJkm), colIncome, y, paint(gray, 9f, align = Paint.Align.RIGHT)); y += 14f }
        }

        // ═══════════════════════════════════════════════════════════
        //  TOTAL ROW
        // ═══════════════════════════════════════════════════════════
        y += 4f
        canvas.drawLine(ml, y, mr, y, thickLine)
        y += 2f
        canvas.drawLine(ml, y, mr, y, thickLine)
        y += 16f

        val totalPendapatan = p.baseSalary + totalAllowance + p.overtimePay + p.kpiBonus
        val totalPotongan = p.bpjsKesehatan + p.bpjsJht + p.bpjsJp + p.pph21

        canvas.drawText("TOTAL", colDesc, y, sectionPaint)
        canvas.drawText(currencyFmt.format(totalPendapatan), colIncome, y, boldRight)
        canvas.drawText(currencyFmt.format(totalPotongan), colDeduct, y, boldRight)
        y += 20f

        // NET SALARY BOX
        val boxPaint = Paint().apply { color = black; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRect(ml, y - 4f, mr, y + 22f, boxPaint)
        y += 14f
        val netLabel = paint(black, 11f, bold = true)
        canvas.drawText("GAJI BERSIH (TAKE HOME PAY)", ml + 10f, y, netLabel)
        canvas.drawText(currencyFmt.format(p.netSalary), mr - 10f, y, paint(black, 12f, bold = true, align = Paint.Align.RIGHT))
        y += 30f

        // ═══════════════════════════════════════════════════════════
        //  SIGNATURE BLOCK (3 columns)
        // ═══════════════════════════════════════════════════════════
        y += 10f
        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 16f

        val datePaint = paint(gray, 8f)
        canvas.drawText("Semarang, ${DateUtils.formatDate(DateUtils.today())}", ml, y, datePaint)
        y += 20f

        // 3 columns
        val col1Center = ml + (mr - ml) / 6f
        val col2Center = pw / 2f
        val col3Center = mr - (mr - ml) / 6f

        val sigTitlePaint = paint(black, 9f, bold = true, align = Paint.Align.CENTER)
        val sigNamePaint = paint(black, 9f, bold = true, align = Paint.Align.CENTER)
        val sigLinePaint = Paint().apply { color = black; strokeWidth = 0.5f }

        canvas.drawText("Diterima oleh,", col1Center, y, paint(gray, 8f, align = Paint.Align.CENTER))
        canvas.drawText("Disetujui oleh,", col2Center, y, paint(gray, 8f, align = Paint.Align.CENTER))
        canvas.drawText("Diproses oleh,", col3Center, y, paint(gray, 8f, align = Paint.Align.CENTER))
        y += 50f // space for signature

        // Signature lines
        val lineHalf = 60f
        canvas.drawLine(col1Center - lineHalf, y, col1Center + lineHalf, y, sigLinePaint)
        canvas.drawLine(col2Center - lineHalf, y, col2Center + lineHalf, y, sigLinePaint)
        canvas.drawLine(col3Center - lineHalf, y, col3Center + lineHalf, y, sigLinePaint)
        y += 14f

        // Names
        val empSigName = signatures.employeeName.ifEmpty { employeeName.ifEmpty { p.employeeName } }
        val mgrSigName = signatures.managerName.ifEmpty { "Manager" }
        val hrSigName = signatures.hrName.ifEmpty { "HRD" }

        canvas.drawText(empSigName, col1Center, y, sigNamePaint)
        canvas.drawText(mgrSigName, col2Center, y, sigNamePaint)
        canvas.drawText(hrSigName, col3Center, y, sigNamePaint)
        y += 14f

        canvas.drawText("Karyawan", col1Center, y, paint(gray, 8f, align = Paint.Align.CENTER))
        canvas.drawText("Manager", col2Center, y, paint(gray, 8f, align = Paint.Align.CENTER))
        canvas.drawText("HRD", col3Center, y, paint(gray, 8f, align = Paint.Align.CENTER))

        // ═══════════════════════════════════════════════════════════
        //  FOOTER
        // ═══════════════════════════════════════════════════════════
        val footY = 820f
        canvas.drawLine(ml, footY - 16f, mr, footY - 16f, thinLine)
        canvas.drawText("Dokumen ini dihasilkan oleh sistem HRIS ${cName} dan sah tanpa tanda tangan basah.", ml, footY - 4f, paint(gray, 7f))
        canvas.drawText("Dicetak: ${DateUtils.formatDate(DateUtils.today())} ${DateUtils.nowTime()}", mr, footY - 4f, paint(gray, 7f, align = Paint.Align.RIGHT))
    }

    // ── Helper: draw a table row ─────────────────────────────────────────
    private fun tableRow(
        canvas: Canvas, colDesc: Float, colIncome: Float, colDeduct: Float,
        y: Float, label: String, income: String, deduct: String,
        labelPaint: Paint, valuePaint: Paint, linePaint: Paint,
        ml: Float, mr: Float
    ): Float {
        canvas.drawText(label, colDesc, y, labelPaint)
        if (income.isNotEmpty()) canvas.drawText(income, colIncome, y, valuePaint)
        if (deduct.isNotEmpty()) canvas.drawText(deduct, colDeduct, y, valuePaint)
        val ny = y + 16f
        canvas.drawLine(ml, ny - 2f, mr, ny - 2f, linePaint)
        return ny
    }

    // ── Helper: create Paint ─────────────────────────────────────────────
    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT): Paint {
        return Paint().apply {
            this.color = color
            this.textSize = size
            this.isFakeBoldText = bold
            this.isAntiAlias = true
            this.textAlign = align
        }
    }
}
