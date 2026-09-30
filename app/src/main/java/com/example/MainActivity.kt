package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.history.BuildHistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.terminal.TerminalScreen
import com.example.ui.screens.tools.ToolsStorageScreen
import com.example.ui.screens.workspace.ProjectDetailScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodels.*
import kotlinx.coroutines.launch

enum class MainNavTab {
    PROJECTS,
    TERMINAL,
    TOOLS_STORAGE,
    HISTORY
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                ATPAppRoot()
            }
        }
    }
}

@Composable
fun ATPAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as ATPApplication
    val coroutineScope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.projectRepository)
    )
    val terminalViewModel: TerminalViewModel = viewModel(
        factory = TerminalViewModel.Factory(context)
    )
    val toolsStorageViewModel: ToolsStorageViewModel = viewModel(
        factory = ToolsStorageViewModel.Factory(
            app.environmentDetector,
            app.workspaceManager,
            app.packageRepository
        )
    )
    val buildHistoryViewModel: BuildHistoryViewModel = viewModel(
        factory = BuildHistoryViewModel.Factory(app.buildRepository)
    )

    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var currentNavTab by remember { mutableStateOf(MainNavTab.PROJECTS) }

    val projects by homeViewModel.projects.collectAsState()

    // Initialize initial test project if empty (Sections 101 & 102 in prompt)
    LaunchedEffect(projects) {
        if (projects.isEmpty()) {
            homeViewModel.createProjectFromTemplate(
                name = "KivyTestApp",
                appName = "ATP Kivy App",
                packageName = "com.atp.kivyapp",
                framework = "KIVY",
                templateId = "test_multidep"
            ) {}
        }
    }

    if (selectedProjectId != null) {
        val detailViewModel: ProjectDetailViewModel = viewModel(
            key = "project_${selectedProjectId}",
            factory = ProjectDetailViewModel.Factory(
                context = context,
                projectId = selectedProjectId!!,
                projectRepository = app.projectRepository,
                packageRepository = app.packageRepository,
                buildRepository = app.buildRepository,
                environmentDetector = app.environmentDetector
            )
        )

        ProjectDetailScreen(
            viewModel = detailViewModel,
            onBack = { selectedProjectId = null }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple(MainNavTab.PROJECTS, "Projects", Icons.Default.Folder),
                        Triple(MainNavTab.TERMINAL, "Terminal", Icons.Default.Terminal),
                        Triple(MainNavTab.TOOLS_STORAGE, "Tools & Storage", Icons.Default.BuildCircle),
                        Triple(MainNavTab.HISTORY, "History", Icons.Default.History)
                    )

                    items.forEach { (tab, label, icon) ->
                        NavigationBarItem(
                            selected = currentNavTab == tab,
                            onClick = { currentNavTab = tab },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF0E3A4D)
                            ),
                            modifier = Modifier.testTag("nav_${label.lowercase().replace(" ", "_")}")
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
                when (currentNavTab) {
                    MainNavTab.PROJECTS -> HomeScreen(
                        viewModel = homeViewModel,
                        onProjectClick = { selectedProjectId = it }
                    )
                    MainNavTab.TERMINAL -> TerminalScreen(viewModel = terminalViewModel)
                    MainNavTab.TOOLS_STORAGE -> ToolsStorageScreen(viewModel = toolsStorageViewModel)
                    MainNavTab.HISTORY -> BuildHistoryScreen(viewModel = buildHistoryViewModel)
                }
            }
        }
    }
}
