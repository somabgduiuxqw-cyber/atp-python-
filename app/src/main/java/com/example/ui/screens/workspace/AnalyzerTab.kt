package com.example.ui.screens.workspace

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandidatePackage
import com.example.data.model.DependencyStatus
import com.example.data.model.DetectedImport
import com.example.data.model.UnusedDependency
import com.example.ui.components.ConfidenceBadge
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel
import com.example.ui.viewmodels.ProjectTab

@Composable
fun AnalyzerTab(viewModel: ProjectDetailViewModel) {
    val analysis by viewModel.analysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()

    var showSyncDialog by remember { mutableStateOf(false) }
    var selectedAmbiguousImport by remember { mutableStateOf<DetectedImport?>(null) }
    var wrongMappingTarget by remember { mutableStateOf<DetectedImport?>(null) }
    var manualPackageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.installAllConfirmed() },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("install_all_confirmed_button")
            ) {
                Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Install All Confirmed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.analyzeDependencies() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("rescan_imports_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Scan Again", fontSize = 12.sp)
            }

            IconButton(
                onClick = { showSyncDialog = true },
                modifier = Modifier.testTag("sync_requirements_button")
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Sync Requirements", tint = CyanPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress banner if installing
        if (installProgress != null) {
            val prog = installProgress!!
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Installing ${prog.packageName} (${prog.stage})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanPrimary)
                        Text("${(prog.progressPercent * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { prog.progressPercent },
                        modifier = Modifier.fillMaxWidth(),
                        color = CyanPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(prog.currentLog, fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (isAnalyzing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return
        }

        val res = analysis
        if (res == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No dependency data available. Tap 'Scan Again'.", color = TextSecondary)
            }
            return
        }

        // Summary Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryCounter("Confirmed", res.required.size, GreenSuccess)
            SummaryCounter("Suggested", res.suggested.size, AmberAccent)
            SummaryCounter("Unknown", res.unknown.size, RedError)
            SummaryCounter("StdLib", res.standardLibrary.size, CyanSecondary)
            SummaryCounter("Local", res.localModules.size, Color(0xFFA5B4FC))
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // UNUSED DEPENDENCY SECTION (Prompt 12)
            if (res.unusedInRequirements.isNotEmpty()) {
                item {
                    SectionHeader("Possible Unused Dependencies in requirements.txt", RedError)
                }
                items(res.unusedInRequirements) { unused ->
                    UnusedDependencyCard(
                        unused = unused,
                        onRemove = { viewModel.removeUnusedDependency(unused.packageName) }
                    )
                }
            }

            // UNKNOWN / AMBIGUOUS IMPORTS SECTION (Prompt 7, 9, 10, 46)
            if (res.unknown.isNotEmpty()) {
                item {
                    SectionHeader("Ambiguous / Unknown Dependencies (${res.unknown.size})", AmberAccent)
                }
                items(res.unknown) { item ->
                    UnknownImportCard(
                        item = item,
                        onChooseCandidate = { cand ->
                            viewModel.resolveAmbiguousPackage(item.moduleName, cand.name)
                        },
                        onSearchPyPi = {
                            viewModel.searchPyPi(item.moduleName)
                            viewModel.setTab(ProjectTab.PACKAGES)
                        },
                        onManualAdd = {
                            selectedAmbiguousImport = item
                        }
                    )
                }
            }

            // CONFIRMED DEPENDENCIES SECTION
            if (res.required.isNotEmpty()) {
                item {
                    SectionHeader("Confirmed Project Dependencies (${res.required.size})", GreenSuccess)
                }
                items(res.required) { item ->
                    ConfirmedImportCard(
                        item = item,
                        onInstall = { item.mappedPackage?.let { viewModel.installSinglePackage(it) } },
                        onWrongMapping = { wrongMappingTarget = item }
                    )
                }
            }

            // SUGGESTED / CONDITIONAL IMPORTS
            if (res.suggested.isNotEmpty()) {
                item {
                    SectionHeader("Suggested & Conditional Dependencies (${res.suggested.size})", AmberAccent)
                }
                items(res.suggested) { item ->
                    ConfirmedImportCard(
                        item = item,
                        onInstall = { item.mappedPackage?.let { viewModel.installSinglePackage(it) } },
                        onWrongMapping = { wrongMappingTarget = item }
                    )
                }
            }

            // LOCAL PROJECT MODULES (Prompt 62, 63, 64)
            if (res.localModules.isNotEmpty()) {
                item {
                    SectionHeader("Local Project Modules (${res.localModules.size})", Color(0xFFA5B4FC))
                }
                items(res.localModules) { item ->
                    LocalModuleCard(item = item)
                }
            }

            // PYTHON STANDARD LIBRARY (Prompt 60, 61)
            if (res.standardLibrary.isNotEmpty()) {
                item {
                    SectionHeader("Python Standard Library (${res.standardLibrary.size})", CyanSecondary)
                }
                items(res.standardLibrary) { item ->
                    StdLibCard(item = item)
                }
            }
        }
    }

    // Manual package mapping dialog
    if (selectedAmbiguousImport != null) {
        val target = selectedAmbiguousImport!!
        AlertDialog(
            onDismissRequest = { selectedAmbiguousImport = null },
            title = { Text("Map Import '${target.moduleName}'") },
            text = {
                Column {
                    Text(
                        "Specify the authentic PyPI package providing 'import ${target.moduleName}':",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualPackageInput,
                        onValueChange = { manualPackageInput = it },
                        label = { Text("PyPI Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualPackageInput.isNotBlank()) {
                            viewModel.resolveAmbiguousPackage(target.moduleName, manualPackageInput.trim())
                            selectedAmbiguousImport = null
                            manualPackageInput = ""
                        }
                    }
                ) {
                    Text("Save & Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAmbiguousImport = null }) { Text("Cancel") }
            }
        )
    }

    // Wrong mapping reporting dialog (Prompt 56)
    if (wrongMappingTarget != null) {
        val target = wrongMappingTarget!!
        var newMappingInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { wrongMappingTarget = null },
            title = { Text("Correct Import Mapping") },
            text = {
                Column {
                    Text("Current mapping: import ${target.moduleName} → ${target.mappedPackage}")
                    Spacer(Modifier.height(6.dp))
                    Text("Enter the correct PyPI package name:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newMappingInput,
                        onValueChange = { newMappingInput = it },
                        label = { Text("Correct Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMappingInput.isNotBlank()) {
                            viewModel.resolveAmbiguousPackage(target.moduleName, newMappingInput.trim())
                            wrongMappingTarget = null
                        }
                    }
                ) {
                    Text("Update Mapping")
                }
            },
            dismissButton = {
                TextButton(onClick = { wrongMappingTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Sync Requirements Dialog (Prompt 50)
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync requirements.txt") },
            text = {
                Text(
                    "Detected confirmed dependencies will be cleanly added to requirements.txt, preserving comments and constraints. Do you want to review and apply changes?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.syncRequirementsTxt()
                        showSyncDialog = false
                    }
                ) {
                    Text("Sync Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp, 14.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}

@Composable
private fun ConfirmedImportCard(
    item: DetectedImport,
    onInstall: () -> Unit,
    onWrongMapping: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ ${item.moduleName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenSuccess)
                    Spacer(Modifier.width(8.dp))
                    ConfidenceBadge(item.confidence)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Package: ${item.mappedPackage ?: item.moduleName}",
                    fontSize = 12.sp,
                    color = CyanSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Found in: ${item.sourceFile}:${item.lineNumber} • ${item.rawStatement}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                if (item.requiresNativeRecipe) {
                    Text(
                        "⚠ Requires Android NDK recipe",
                        fontSize = 11.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.status == DependencyStatus.INSTALLED) {
                    Text("Installed", color = GreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Install", fontSize = 11.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = onWrongMapping,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Wrong Mapping?", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun UnknownImportCard(
    item: DetectedImport,
    onChooseCandidate: (CandidatePackage) -> Unit,
    onSearchPyPi: () -> Unit,
    onManualAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("? ${item.moduleName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmberAccent)
                Spacer(Modifier.width(8.dp))
                ConfidenceBadge(item.confidence)
                Spacer(Modifier.weight(1f))
                Text(
                    "in ${item.sourceFile}:${item.lineNumber}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "No confirmed single package. Never blindly installing unrelated packages.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            // Multiple Candidates Picker (Section 7, 46)
            if (item.candidatePackages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Candidate packages found:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyanSecondary)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.candidatePackages.forEach { cand ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cand.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Text(cand.summary, fontSize = 10.sp, color = TextSecondary)
                            }
                            Button(
                                onClick = { onChooseCandidate(cand) },
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Choose", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSearchPyPi,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Search PyPI", fontSize = 11.sp)
                }

                TextButton(
                    onClick = onManualAdd,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Add Manually", fontSize = 11.sp, color = CyanPrimary)
                }
            }
        }
    }
}

@Composable
private fun StdLibCard(item: DetectedImport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ ${item.moduleName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyanSecondary)
                    Spacer(Modifier.width(6.dp))
                    ConfidenceBadge(item.confidence)
                }
                Text("Python Standard Library • No installation required", fontSize = 11.sp, color = TextSecondary)
            }
            Text("Built-in", fontSize = 11.sp, color = CyanSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LocalModuleCard(item: DetectedImport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁 ${item.moduleName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFA5B4FC))
                    Spacer(Modifier.width(6.dp))
                    ConfidenceBadge(item.confidence)
                }
                Text(item.notes, fontSize = 11.sp, color = TextSecondary)
            }
            Text("Local File", fontSize = 11.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UnusedDependencyCard(
    unused: UnusedDependency,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261014)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RedError.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(unused.packageName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedError)
                Text(
                    "In ${unused.definedIn} but not imported anywhere. Increases APK by ${unused.estimatedSizeImpact}.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Row {
                OutlinedButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Remove", fontSize = 11.sp)
                }
            }
        }
    }
}
