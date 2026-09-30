package com.example.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuildMode
import com.example.data.model.StepState
import com.example.ui.components.ApkActionHelper
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel
import org.json.JSONArray

@Composable
fun BuildTab(viewModel: ProjectDetailViewModel) {
    val project by viewModel.project.collectAsState()
    val buildMode by viewModel.buildMode.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val buildSteps by viewModel.buildSteps.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()
    val verification by viewModel.apkVerification.collectAsState()
    val onlineJob by viewModel.onlineJob.collectAsState()
    val estimatedSize by viewModel.estimatedApkSize.collectAsState()
    val context = LocalContext.current

    if (project == null) return
    val proj = project!!

    val confirmedDeps = viewModel.analysis.value?.required?.mapNotNull { it.mappedPackage } ?: emptyList()
    val missingDeps = viewModel.analysis.value?.unknown?.map { it.moduleName } ?: emptyList()

    val permissionsCount = try {
        JSONArray(proj.permissionsJson).length()
    } catch (e: Exception) { 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // SECTION 1: Pre-Build Summary Card (Prompt 35)
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
                    Text("Build Summary", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 16.sp)
                    Text("Estimated: ${estimatedSize.formattedTotal()}", fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(8.dp))

                SummaryRow("Application", proj.appName)
                SummaryRow("Package Name", proj.packageName)
                SummaryRow("Framework", proj.framework)
                SummaryRow("Python Dependencies", if (confirmedDeps.isNotEmpty()) "✓ " + confirmedDeps.joinToString(", ") else "None")
                SummaryRow("Missing Dependencies", if (missingDeps.isNotEmpty()) "⚠ " + missingDeps.joinToString(", ") else "None", isWarning = missingDeps.isNotEmpty())
                SummaryRow("App Logo", "✓ Custom ATP Icon")
                SummaryRow("Permissions", "✓ $permissionsCount configured")
                SummaryRow("Background Mode", proj.backgroundMode)
                SummaryRow("Creator", if (proj.creatorUsername.isNotBlank()) proj.creatorUsername else "ATP Python")
            }
        }

        Spacer(Modifier.height(12.dp))

        // SECTION 2: Build Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = buildMode == BuildMode.LOCAL,
                onClick = { viewModel.setBuildMode(BuildMode.LOCAL) },
                label = { Text("Local Build", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = buildMode == BuildMode.ONLINE,
                onClick = { viewModel.setBuildMode(BuildMode.ONLINE) },
                label = { Text("Online Server Build", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // BUILD BUTTON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.startBuild() },
                enabled = !isBuilding,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("build_apk_button")
            ) {
                if (isBuilding) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkBg, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Building...", color = DarkBg, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Build, contentDescription = null, tint = DarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text("BUILD APK", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (isBuilding) {
                OutlinedButton(
                    onClick = { viewModel.cancelBuild() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // SECTION 3: Live Build Steps & Logs (Prompt 36, 91)
        if (buildSteps.isNotEmpty() || buildLogs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Build Progress & Pipeline", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))

                    buildSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (step.state) {
                                StepState.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                                StepState.FAILED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = RedError, modifier = Modifier.size(18.dp))
                                StepState.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanPrimary, strokeWidth = 2.dp)
                                else -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(step.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                                if (step.logOutput.isNotBlank()) {
                                    Text(step.logOutput, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Real Build Console Logs:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .background(CodeBg, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (buildLogs.isEmpty()) "Waiting for pipeline..." else buildLogs.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // SECTION 4: APK Verification & Output (Prompt 37, 38)
        if (verification != null) {
            val ver = verification!!
            if (ver.isValid && ver.apkFile != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF063E2D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GreenSuccess, RoundedCornerShape(8.dp))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess)
                            Spacer(Modifier.width(8.dp))
                            Text("BUILD SUCCESSFUL", fontWeight = FontWeight.Bold, color = GreenSuccess, fontSize = 16.sp)
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(ver.apkFile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Actual APK Size: ${ver.apkSize / (1024 * 1024)} MB (${ver.apkSize} bytes)", fontSize = 12.sp, color = TextSecondary)
                        Text("Package: ${ver.packageName} • Version: ${ver.versionName} (${ver.versionCode})", fontSize = 12.sp, color = TextSecondary)
                        Text("SHA-256: ${ver.sha256Checksum.take(24)}...", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)

                        Spacer(Modifier.height(10.dp))
                        Text("APK Verification Checks Passed:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CyanSecondary)
                        ver.checks.forEach { check ->
                            Text("✓ ${check.name}: ${check.details}", fontSize = 11.sp, color = GreenSuccess)
                        }

                        Spacer(Modifier.height(12.dp))

                        // Action buttons: Install, Share, Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { ApkActionHelper.installApk(context, ver.apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("install_apk_button")
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { ApkActionHelper.shareApk(context, ver.apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                            }

                            OutlinedButton(
                                onClick = { ApkActionHelper.saveApkToDownloads(context, ver.apkFile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF381014)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RedError, RoundedCornerShape(8.dp))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = RedError)
                            Spacer(Modifier.width(8.dp))
                            Text("BUILD FAILED", fontWeight = FontWeight.Bold, color = RedError, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(ver.failureReason ?: "Unknown build verification failure", fontSize = 13.sp, color = TextPrimary)

                        if (ver.checks.any { !it.passed }) {
                            Spacer(Modifier.height(8.dp))
                            Text("Failed Checks:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RedError)
                            ver.checks.filter { !it.passed }.forEach { check ->
                                Text("✗ ${check.name}: ${check.details}", fontSize = 11.sp, color = RedError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isWarning) AmberAccent else TextPrimary
        )
    }
}
