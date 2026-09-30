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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DependencyTreeNode
import com.example.data.model.PyPiPackageInfo
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel

@Composable
fun PackagesTab(viewModel: ProjectDetailViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val installedPackages by viewModel.installedPackages.collectAsState()
    val dependencyTree by viewModel.dependencyTree.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()

    var activeSubTab by remember { mutableStateOf("SEARCH") } // SEARCH, TREE, INSTALLED

    LaunchedEffect(Unit) {
        viewModel.updateDependencyTree()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Tab switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeSubTab == "SEARCH",
                onClick = { activeSubTab = "SEARCH" },
                label = { Text("PyPI Search", fontSize = 12.sp) }
            )
            FilterChip(
                selected = activeSubTab == "INSTALLED",
                onClick = { activeSubTab = "INSTALLED" },
                label = { Text("Installed (${installedPackages.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = activeSubTab == "TREE",
                onClick = {
                    activeSubTab = "TREE"
                    viewModel.updateDependencyTree()
                },
                label = { Text("Dependency Tree", fontSize = 12.sp) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Install Progress card if ongoing
        if (installProgress != null) {
            val prog = installProgress!!
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "Package: ${prog.packageName} [${prog.stage}]",
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        fontSize = 12.sp
                    )
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
            Spacer(Modifier.height(6.dp))
        }

        if (activeSubTab == "SEARCH") {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchPyPi(it) },
                label = { Text("Search authentic PyPI packages...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pypi_search_input")
            )

            Spacer(Modifier.height(8.dp))

            if (isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No packages found on PyPI matching '$searchQuery'.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { pkg ->
                        PyPiPackageCard(
                            pkg = pkg,
                            isInstalled = installedPackages.any { it.equals(pkg.name, ignoreCase = true) },
                            onInstall = { viewModel.installSinglePackage(pkg.name) }
                        )
                    }
                }
            }
        } else if (activeSubTab == "INSTALLED") {
            if (installedPackages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No packages installed yet. Search PyPI or import requirements.txt.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installedPackages) { pkgName ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pkgName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Installed in project environment", fontSize = 11.sp, color = GreenSuccess)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.removeUnusedDependency(pkgName) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Uninstall", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeSubTab == "TREE") {
            // Interactive Dependency Tree (Prompt 16)
            if (dependencyTree.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No dependency tree available. Install packages first.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Project Dependency Tree", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 14.sp)
                                Text("Transitive dependency hierarchy resolved from package metadata:", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    items(dependencyTree) { node ->
                        DependencyTreeNodeItem(node = node, level = 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun PyPiPackageCard(
    pkg: PyPiPackageInfo,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(pkg.version, fontSize = 12.sp, color = CyanSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Text(pkg.summary, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                }

                Button(
                    onClick = onInstall,
                    enabled = !isInstalled,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isInstalled) DarkSurfaceVariant else CyanPrimary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (isInstalled) "Installed" else "Install", fontSize = 11.sp, color = if (isInstalled) TextSecondary else DarkBg, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pkg.isAndroidCompatible) {
                    Text("✓ Android Compatible", fontSize = 11.sp, color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("⚠ Native Recipe Required", fontSize = 11.sp, color = AmberAccent, fontWeight = FontWeight.SemiBold)
                }

                if (pkg.license.isNotBlank()) {
                    Text("• License: ${pkg.license}", fontSize = 11.sp, color = TextMuted)
                }
            }

            if (pkg.requiresDist.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Requires: " + pkg.requiresDist.take(4).joinToString(", "),
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DependencyTreeNodeItem(node: DependencyTreeNode, level: Int) {
    val indent = (level * 16).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (level == 0) "📦" else "↳", fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(node.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.width(6.dp))
            Text(node.version, fontSize = 10.sp, color = CyanSecondary, fontFamily = FontFamily.Monospace)
        }
        for (child in node.children) {
            DependencyTreeNodeItem(child, level + 1)
        }
    }
}
