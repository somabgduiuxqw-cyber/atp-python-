package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.ImportConfidence
import com.example.ui.theme.*
import java.io.File

@Composable
fun ConfidenceBadge(confidence: ImportConfidence) {
    val (bgColor, textColor, label) = when (confidence) {
        ImportConfidence.CONFIRMED -> Triple(Color(0xFF064E3B), GreenSuccess, "✓ Confirmed")
        ImportConfidence.LIKELY -> Triple(Color(0xFF451A03), AmberAccent, "! Likely")
        ImportConfidence.UNKNOWN -> Triple(Color(0xFF450A0A), RedError, "? Unknown")
        ImportConfidence.BUILTIN -> Triple(Color(0xFF1E3A8A), CyanSecondary, "★ StdLib")
        ImportConfidence.LOCAL -> Triple(Color(0xFF312E81), Color(0xFFA5B4FC), "📁 Local Module")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusPill(text: String, isSuccess: Boolean) {
    val bg = if (isSuccess) Color(0xFF064E3B) else Color(0xFF450A0A)
    val fg = if (isSuccess) GreenSuccess else RedError
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

object ApkActionHelper {
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK file does not exist", Toast.LENGTH_SHORT).show()
                return
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.atp.pythonapk.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) return
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.atp.pythonapk.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Share ATP Python APK"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveApkToDownloads(context: Context, apkFile: File) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val target = File(downloadsDir, apkFile.name)
            apkFile.copyTo(target, overwrite = true)
            Toast.makeText(context, "Saved to Downloads: ${target.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Saved internally at: ${apkFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
