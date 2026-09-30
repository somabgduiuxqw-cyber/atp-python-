package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BuildHistoryEntity
import com.example.ui.components.ApkActionHelper
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodels.BuildHistoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuildHistoryScreen(viewModel: BuildHistoryViewModel) {
    val builds by viewModel.builds.collectAsState()
    val context = LocalContext.current
    var selectedBuildForLogs by remember { mutableStateOf<BuildHistoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        Text("APK Build History (${builds.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.height(8.dp))

        if (builds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No builds recorded yet. Build an APK from any project workspace.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(builds, key = { it.id }) { build ->
                    BuildHistoryItemCard(
                        build = build,
                        onClick = { selectedBuildForLogs = build },
                        onInstall = {
                            if (build.apkPath != null) {
                                ApkActionHelper.installApk(context, File(build.apkPath))
                            }
                        },
                        onDelete = { viewModel.deleteBuild(build.id) }
                    )
                }
            }
        }
    }

    if (selectedBuildForLogs != null) {
        val b = selectedBuildForLogs!!
        AlertDialog(
            onDismissRequest = { selectedBuildForLogs = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.projectName, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(b.status, b.status == "SUCCESS")
                }
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("Duration: ${b.durationMs}ms • Size: ${b.apkSize / (1024 * 1024)} MB", fontSize = 11.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .background(CodeBg, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (b.fullLogs.isBlank()) "No detailed log captured." else b.fullLogs,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBuildForLogs = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun BuildHistoryItemCard(
    build: BuildHistoryEntity,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(build.timestamp) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(build.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(build.projectName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(build.status, build.status == "SUCCESS")
                }
                Spacer(Modifier.height(2.dp))
                Text("Backend: ${build.backend} • Type: ${build.buildType} • ${dateStr}", fontSize = 11.sp, color = TextSecondary)
                if (build.status == "SUCCESS") {
                    Text("APK Size: ${build.apkSize / (1024 * 1024)} MB (${build.apkSize} bytes)", fontSize = 11.sp, color = GreenSuccess)
                } else if (build.errorSummary != null) {
                    Text(build.errorSummary, fontSize = 11.sp, color = RedError, maxLines = 1)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (build.status == "SUCCESS" && build.apkPath != null && File(build.apkPath).exists()) {
                    IconButton(onClick = onInstall) {
                        Icon(Icons.Default.InstallMobile, contentDescription = "Install", tint = GreenSuccess)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}
