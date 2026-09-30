package com.example.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ToolStatus
import com.example.ui.theme.*
import com.example.ui.viewmodels.ToolsStorageViewModel

@Composable
fun ToolsStorageScreen(viewModel: ToolsStorageViewModel) {
    val tools by viewModel.tools.collectAsState()
    val isLoading by viewModel.isLoadingTools.collectAsState()
    val report by viewModel.deviceReport.collectAsState()
    val storage by viewModel.storageBreakdown.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Environment & Storage Manager", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            IconButton(onClick = { viewModel.refreshAll() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // DEVICE RESOURCE CHECK (Prompt 53)
            if (report != null) {
                item {
                    val rep = report!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeviceHub, contentDescription = null, tint = CyanPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Device Resources & Hardware Check", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyanPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Available RAM:", fontSize = 12.sp, color = TextSecondary)
                                Text("${rep.availableRamMb} MB / ${rep.totalRamMb} MB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Free Storage:", fontSize = 12.sp, color = TextSecondary)
                                Text("${rep.availableStorageMb} MB / ${rep.totalStorageMb} MB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Architecture:", fontSize = 12.sp, color = TextSecondary)
                                Text(rep.primaryAbi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AmberAccent, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Android OS:", fontSize = 12.sp, color = TextSecondary)
                                Text("Android ${rep.androidVersion} (API ${rep.apiLevel})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Spacer(Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (rep.isLocalBuildViable) Color(0xFF063E2D) else Color(0xFF381014)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(
                                        if (rep.isLocalBuildViable) "✓ Local Build Feasible" else "⚠ Online Build Recommended",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (rep.isLocalBuildViable) GreenSuccess else AmberAccent
                                    )
                                    Text(rep.recommendation, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // STORAGE MANAGER (Prompt 40)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Disk Storage Allocation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmberAccent)
                            Button(
                                onClick = { showClearDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Caches", fontSize = 11.sp, color = RedError)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        storage.forEach { (cat, bytes) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    String.format("%.2f MB", bytes / (1024.0 * 1024.0)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // TOOL MANAGER LIST (Prompt 41)
            item {
                Text(
                    "Local Toolchain Verification (Authentic check)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyanSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanPrimary)
                    }
                }
            } else {
                items(tools) { tool ->
                    ToolItemCard(tool = tool)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Temporary Caches") },
            text = { Text("This will purge the downloaded package wheels and intermediate build directories. Projects will NOT be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Clear Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ToolItemCard(tool: ToolStatus) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (tool.isInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (tool.isInstalled) GreenSuccess else RedError,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("(${tool.command})", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }
                Text(tool.detectedVersion, fontSize = 11.sp, color = if (tool.isInstalled) GreenSuccess else RedError, fontWeight = FontWeight.Medium)
                Text(tool.notes, fontSize = 10.sp, color = TextMuted)
            }

            Box(
                modifier = Modifier
                    .background(
                        if (tool.isInstalled) Color(0xFF064E3B) else Color(0xFF450A0A),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (tool.isInstalled) "Installed" else "Missing",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tool.isInstalled) GreenSuccess else RedError
                )
            }
        }
    }
}
