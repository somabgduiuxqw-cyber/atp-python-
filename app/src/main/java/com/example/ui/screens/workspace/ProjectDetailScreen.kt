package com.example.ui.screens.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel
import com.example.ui.viewmodels.ProjectTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: ProjectDetailViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.project.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    BackHandler {
        onBack()
    }

    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyanPrimary)
        }
        return
    }

    val proj = project!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(proj.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Badge(containerColor = CyanPrimary) {
                                Text(proj.framework, color = DarkBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(proj.packageName, fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("project_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(ProjectTab.EDITOR, "Editor", Icons.Default.Code),
                    Triple(ProjectTab.ANALYZER, "Analysis", Icons.Default.Troubleshoot),
                    Triple(ProjectTab.PACKAGES, "Packages", Icons.Default.Inventory2),
                    Triple(ProjectTab.CONFIG, "Config", Icons.Default.Settings),
                    Triple(ProjectTab.BUILD, "Build APK", Icons.Default.Build)
                )

                navItems.forEach { (tab, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color(0xFF0E3A4D)
                        ),
                        modifier = Modifier.testTag("tab_${label.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ProjectTab.EDITOR -> EditorTab(viewModel = viewModel)
                ProjectTab.ANALYZER -> AnalyzerTab(viewModel = viewModel)
                ProjectTab.PACKAGES -> PackagesTab(viewModel = viewModel)
                ProjectTab.CONFIG -> ConfigTab(viewModel = viewModel)
                ProjectTab.BUILD -> BuildTab(viewModel = viewModel)
            }
        }
    }
}
