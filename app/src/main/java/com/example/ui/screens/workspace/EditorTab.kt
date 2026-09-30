package com.example.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PythonSyntaxHighlighter
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel
import com.example.ui.viewmodels.ProjectTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTab(viewModel: ProjectDetailViewModel) {
    val code by viewModel.editorCode.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val availableFiles by viewModel.availableFiles.collectAsState()
    val isDirty by viewModel.isEditorDirty.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }

    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top Toolbar: File chips & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableFiles.forEach { file ->
                FilterChip(
                    selected = file == selectedFile,
                    onClick = { viewModel.selectFile(file) },
                    label = {
                        Text(
                            file + if (file == selectedFile && isDirty) " •" else "",
                            fontSize = 12.sp,
                            fontWeight = if (file == selectedFile) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            IconButton(
                onClick = { showNewFileDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File", tint = CyanPrimary)
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { searchMode = !searchMode },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchMode) AmberAccent else TextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.saveCurrentFile() },
                enabled = isDirty,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("save_file_button")
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (isDirty) GreenSuccess else TextMuted
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (isDirty) viewModel.saveCurrentFile()
                    viewModel.analyzeDependencies()
                    viewModel.setTab(ProjectTab.ANALYZER)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("analyze_imports_button")
            ) {
                Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Analyze Imports", fontSize = 12.sp, color = DarkBg, fontWeight = FontWeight.Bold)
            }
        }

        // Search & Replace bar if active
        if (searchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    placeholder = { Text("Replace...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            viewModel.updateEditorCode(code.replace(searchQuery, replaceQuery))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("All", fontSize = 11.sp)
                }
            }
        }

        // Code Editor Area with Line Numbers
        val lines = remember(code) { code.split("\n") }
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CodeBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Line numbers column
                Column(
                    modifier = Modifier
                        .background(Color(0xFF070B14))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .widthIn(min = 36.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..maxOf(1, lines.size)) {
                        Text(
                            text = i.toString(),
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }

                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = DarkBorder
                )

                // Editable text field
                BasicTextField(
                    value = code,
                    onValueChange = { viewModel.updateEditorCode(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("code_editor_field"),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(CyanPrimary),
                    visualTransformation = {
                        androidx.compose.ui.text.input.TransformedText(
                            PythonSyntaxHighlighter.highlight(code),
                            androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    }
                )
            }
        }

        // Editor Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Python 3 • UTF-8 • ${lines.size} lines",
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            if (isDirty) {
                Text(
                    "Unsaved changes",
                    fontSize = 11.sp,
                    color = AmberAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New File in Project") },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    label = { Text("File Name (e.g. utils.py, color.py)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            viewModel.createNewFileInProject(newFileNameInput.trim())
                            showNewFileDialog = false
                            newFileNameInput = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }
}
