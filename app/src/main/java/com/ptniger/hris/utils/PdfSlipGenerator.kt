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

object PdfSlipGenerator {

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    fun generateAndOpen(context: Context, payroll: Payroll, employeeName: String, nik: String = "", position: String = "", department: String = "") {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawSlipContent(canvas, payroll, employeeName, nik, position, department)

            document.finishPage(page)

            // Save to Downloads
            val fileName = "SlipGaji_${employeeName.replace(" ", "_")}_${payroll.month}_${payroll.year}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ : use MediaStore
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

                    // Open the PDF
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
                // Legacy: save to external storage
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

    private fun drawSlipContent(canvas: Canvas, p: Payroll, employeeName: String, nik: String, position: String, department: String) {
        val pageWidth = 595f
        val marginLeft = 40f
        val marginRight = 555f
        var y = 40f

        // --- Paints ---
        val titlePaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val subtitlePaint = Paint().apply { color = Color.DKGRAY; textSize = 11f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 13f; isFakeBoldText = true; isAntiAlias = true }
        val normalPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val valuePaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val greenValuePaint = Paint().apply { color = Color.parseColor("#2E7D32"); textSize = 13f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val redValuePaint = Paint().apply { color = Color.parseColor("#C62828"); textSize = 11f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val linePaint = Paint().apply { color = Color.parseColor("#C8E6C9"); strokeWidth = 1.5f }
        val thickLinePaint = Paint().apply { color = Color.parseColor("#1B5E20"); strokeWidth = 3f }
        val lightGrayPaint = Paint().apply { color = Color.parseColor("#F5F5F5") }

        // ========== HEADER ==========
        // Top green bar
        val greenBarPaint = Paint().apply { color = Color.parseColor("#1B5E20") }
        canvas.drawRect(0f, 0f, pageWidth, 6f, greenBarPaint)
        y = 36f

        canvas.drawText("PT NIGER", marginLeft, y, titlePaint)
        y += 18f
        canvas.drawText("Jl. Contoh Alamat No. 123, Jakarta Selatan 12345", marginLeft, y, subtitlePaint)
        y += 14f
        canvas.drawText("Telp: (021) 1234-5678  |  Email: hr@ptniger.com", marginLeft, y, subtitlePaint)
        y += 20f

        // Thick divider
        canvas.drawLine(marginLeft, y, marginRight, y, thickLinePaint)
        y += 20f

        // TITLE
        val slipTitlePaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 16f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("SLIP GAJI KARYAWAN", pageWidth / 2, y, slipTitlePaint)
        y += 16f
        val periodPaint = Paint().apply { color = Color.DKGRAY; textSize = 12f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("Periode: ${DateUtils.formatMonthYear(p.month, p.year)}", pageWidth / 2, y, periodPaint)
        y += 20f

        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 18f

        // ========== EMPLOYEE INFO ==========
        canvas.drawText("INFORMASI KARYAWAN", marginLeft, y, headerPaint)
        y += 20f

        // Background for employee info
        canvas.drawRect(marginLeft, y - 12f, marginRight, y + 60f, lightGrayPaint)

        drawInfoRow(canvas, "Nama Lengkap", employeeName.ifEmpty { p.employeeName }, marginLeft + 10, marginLeft + 140, y, normalPaint, boldPaint)
        y += 18f
        drawInfoRow(canvas, "NIK", nik.ifEmpty { "-" }, marginLeft + 10, marginLeft + 140, y, normalPaint, boldPaint)
        y += 18f
        drawInfoRow(canvas, "Jabatan", position.ifEmpty { "-" }, marginLeft + 10, marginLeft + 140, y, normalPaint, boldPaint)
        y += 18f
        drawInfoRow(canvas, "Departemen", department.ifEmpty { "-" }, marginLeft + 10, marginLeft + 140, y, normalPaint, boldPaint)
        y += 28f

        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 18f

        // ========== PENDAPATAN ==========
        canvas.drawText("PENDAPATAN", marginLeft, y, headerPaint)
        y += 20f

        y = drawSlipRow(canvas, "Gaji Pokok", currencyFmt.format(p.baseSalary), marginLeft, marginRight, y, normalPaint, valuePaint)
        y = drawSlipRow(canvas, "Tunjangan", currencyFmt.format(p.allowance), marginLeft, marginRight, y, normalPaint, valuePaint)
        y = drawSlipRow(canvas, "Lembur (${p.overtimeHours} jam)", currencyFmt.format(p.overtimePay), marginLeft, marginRight, y, normalPaint, valuePaint)
        y = drawSlipRow(canvas, "Bonus KPI (Skor: ${String.format("%.1f", p.kpiScore)})", currencyFmt.format(p.kpiBonus), marginLeft, marginRight, y, normalPaint, valuePaint)

        val totalPendapatan = p.baseSalary + p.allowance + p.overtimePay + p.kpiBonus
        y += 4f
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 16f
        canvas.drawText("Total Pendapatan", marginLeft, y, boldPaint)
        canvas.drawText(currencyFmt.format(totalPendapatan), marginRight, y, greenValuePaint)
        y += 24f

        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 18f

        // ========== POTONGAN ==========
        canvas.drawText("POTONGAN", marginLeft, y, headerPaint)
        y += 20f

        y = drawSlipRow(canvas, "BPJS Kesehatan (1%)", "-${currencyFmt.format(p.bpjsKesehatan)}", marginLeft, marginRight, y, normalPaint, redValuePaint)
        y = drawSlipRow(canvas, "BPJS JHT (2%)", "-${currencyFmt.format(p.bpjsJht)}", marginLeft, marginRight, y, normalPaint, redValuePaint)
        y = drawSlipRow(canvas, "BPJS JP (1%)", "-${currencyFmt.format(p.bpjsJp)}", marginLeft, marginRight, y, normalPaint, redValuePaint)
        y = drawSlipRow(canvas, "Potongan Lainnya", "-${currencyFmt.format(p.deductions)}", marginLeft, marginRight, y, normalPaint, redValuePaint)

        val totalPotongan = p.bpjsKesehatan + p.bpjsJht + p.bpjsJp + p.deductions
        y += 4f
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 16f
        val redBoldValuePaint = Paint().apply { color = Color.parseColor("#C62828"); textSize = 13f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("Total Potongan", marginLeft, y, boldPaint)
        canvas.drawText("-${currencyFmt.format(totalPotongan)}", marginRight, y, redBoldValuePaint)
        y += 24f

        // ========== TOTAL BERSIH ==========
        // Green background box for net salary
        val netBoxPaint = Paint().apply { color = Color.parseColor("#E8F5E9") }
        canvas.drawRect(marginLeft, y - 4f, marginRight, y + 30f, netBoxPaint)
        canvas.drawRect(marginLeft, y - 4f, marginRight, y + 30f, Paint().apply { color = Color.parseColor("#1B5E20"); style = Paint.Style.STROKE; strokeWidth = 2f })

        y += 18f
        val netLabelPaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val netValuePaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 16f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("GAJI BERSIH (TAKE HOME PAY)", marginLeft + 10, y, netLabelPaint)
        canvas.drawText(currencyFmt.format(p.netSalary), marginRight - 10, y, netValuePaint)
        y += 40f

        // ========== FOOTER ==========
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 18f

        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true }
        canvas.drawText("Slip gaji ini dihasilkan secara otomatis oleh sistem HRIS PT Niger.", marginLeft, y, footerPaint)
        y += 14f
        canvas.drawText("Dokumen ini sah tanpa tanda tangan basah.", marginLeft, y, footerPaint)
        y += 14f

        val datePaint = Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("Dicetak: ${DateUtils.formatDate(DateUtils.today())} ${DateUtils.nowTime()}", marginRight, y, datePaint)
        y += 14f

        val statusPaint = Paint().apply {
            color = when(p.status) { "paid" -> Color.parseColor("#2E7D32"); "finalized" -> Color.parseColor("#1565C0"); else -> Color.GRAY }
            textSize = 10f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Status: ${p.status.uppercase()}", marginRight, y, statusPaint)

        // Bottom green bar
        canvas.drawRect(0f, 836f, pageWidth, 842f, greenBarPaint)
    }

    private fun drawInfoRow(canvas: Canvas, label: String, value: String, labelX: Float, valueX: Float, y: Float, labelPaint: Paint, valuePaint: Paint) {
        canvas.drawText("$label:", labelX, y, labelPaint)
        canvas.drawText(value, valueX, y, valuePaint)
    }

    private fun drawSlipRow(canvas: Canvas, label: String, value: String, left: Float, right: Float, y: Float, labelPaint: Paint, valuePaint: Paint): Float {
        canvas.drawText(label, left + 10, y, labelPaint)
        canvas.drawText(value, right, y, valuePaint)
        return y + 18f
    }
}
