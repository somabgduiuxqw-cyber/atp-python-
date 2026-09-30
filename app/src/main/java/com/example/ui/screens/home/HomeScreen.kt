package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProjectEntity
import com.example.ui.components.ProjectCreationDialog
import com.example.ui.theme.*
import com.example.ui.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProjectClick: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyanPrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ATP", fontWeight = FontWeight.Black, color = DarkBg, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ATP Python", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Real Python-to-Android APK Builder", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = CyanPrimary,
                contentColor = DarkBg,
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Project", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Quick import chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("Import .py", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                SuggestionChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("Import ZIP", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                SuggestionChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("Clone Git", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Welcome to ATP Python", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Real Python dependency management, native packaging, and verified APK compilation.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.createProjectFromTemplate(
                                        name = "MultiDepTest",
                                        appName = "Multi Dep App",
                                        packageName = "com.atp.multidep",
                                        framework = "KIVY",
                                        templateId = "test_multidep",
                                        onCreated = onProjectClick
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Create Multi-Dep Test Project", color = DarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Python Projects (${projects.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects, key = { it.id }) { proj ->
                        ProjectItemCard(
                            project = proj,
                            onClick = { onProjectClick(proj.id) },
                            onDelete = { projectToDelete = proj }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectCreationDialog(
            onDismiss = { showCreateDialog = false },
            onCreateFromTemplate = { name, appName, pkg, fw, tmpl ->
                viewModel.createProjectFromTemplate(name, appName, pkg, fw, tmpl) { id ->
                    showCreateDialog = false
                    onProjectClick(id)
                }
            },
            onImportFile = { name, appName, pkg, fw, fileName, content ->
                viewModel.importSingleFile(name, appName, pkg, fw, fileName, content) { id ->
                    showCreateDialog = false
                    onProjectClick(id)
                }
            },
            onImportZip = { name, appName, pkg, fw ->
                // Create blank project as imported ZIP staging
                viewModel.createProjectFromTemplate(name, appName, pkg, fw, "custom") { id ->
                    showCreateDialog = false
                    onProjectClick(id)
                }
            }
        )
    }

    if (projectToDelete != null) {
        val proj = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete '${proj.name}' and all its source files?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(proj)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.lastModified) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(project.lastModified))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF0E3A4D), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanSecondary)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = CyanPrimary) {
                        Text(project.framework, color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(project.packageName, fontSize = 12.sp, color = CyanSecondary)
                Text(dateStr, fontSize = 11.sp, color = TextMuted)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
            }
        }
    }
}
