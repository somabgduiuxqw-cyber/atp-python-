package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreationDialog(
    onDismiss: () -> Unit,
    onCreateFromTemplate: (name: String, appName: String, packageName: String, framework: String, templateId: String) -> Unit,
    onImportFile: (name: String, appName: String, packageName: String, framework: String, fileName: String, content: String) -> Unit,
    onImportZip: (name: String, appName: String, packageName: String, framework: String) -> Unit
) {
    var mode by remember { mutableStateOf("TEMPLATE") } // TEMPLATE, FILE, ZIP, GIT

    var projectName by remember { mutableStateOf("MyPythonApp") }
    var appName by remember { mutableStateOf("My App") }
    var packageName by remember { mutableStateOf("com.atp.myapp") }
    var selectedFramework by remember { mutableStateOf("KIVY") }
    var selectedTemplate by remember { mutableStateOf("test_multidep") }

    // File import fields
    var importedFileName by remember { mutableStateOf("main.py") }
    var importedCode by remember {
        mutableStateOf(
            """from kivy.app import App
from kivy.uix.label import Label
import requests
from PIL import Image
import json
import os

class TestApp(App):
    def build(self):
        return Label(text="Hello ATP Python!")

if __name__ == '__main__':
    TestApp().run()
"""
        )
    }

    // Git import fields
    var gitUrl by remember { mutableStateOf("https://github.com/kivy/kivy.git") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("New Python Project", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mode selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == "TEMPLATE",
                        onClick = { mode = "TEMPLATE" },
                        label = { Text("Templates", fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_templates_chip")
                    )
                    FilterChip(
                        selected = mode == "FILE",
                        onClick = { mode = "FILE" },
                        label = { Text(".py File", fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_file_chip")
                    )
                    FilterChip(
                        selected = mode == "ZIP",
                        onClick = { mode = "ZIP" },
                        label = { Text("ZIP", fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_zip_chip")
                    )
                    FilterChip(
                        selected = mode == "GIT",
                        onClick = { mode = "GIT" },
                        label = { Text("Git", fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_git_chip")
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Common settings
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        appName = it.replace("([A-Z])".toRegex(), " $1").trim()
                        packageName = "com.atp." + it.lowercase().replace("[^a-z0-9]".toRegex(), "")
                    },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (e.g. com.atp.myapp)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("package_name_input")
                )

                Spacer(Modifier.height(12.dp))

                if (mode == "TEMPLATE") {
                    Text("Select Template:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))

                    TemplateCard(
                        title = "Multi-Dependency Test (Prompt 101)",
                        description = "Tests Kivy, requests, PIL (Pillow), json, os (Standard Library detection)",
                        selected = selectedTemplate == "test_multidep",
                        onClick = {
                            selectedTemplate = "test_multidep"
                            selectedFramework = "KIVY"
                            projectName = "MultiDepTest"
                            appName = "Multi Dep App"
                            packageName = "com.atp.multidep"
                        }
                    )

                    TemplateCard(
                        title = "Telegram Bot",
                        description = "python-telegram-bot + requests + Foreground Service auto-configuration",
                        selected = selectedTemplate == "telegram_bot",
                        onClick = {
                            selectedTemplate = "telegram_bot"
                            selectedFramework = "KIVY"
                            projectName = "MyTelegramBot"
                            appName = "Telegram Bot"
                            packageName = "com.atp.telegrambot"
                        }
                    )

                    TemplateCard(
                        title = "KivyMD Material App",
                        description = "Modern Material Design components, screens and HTTP requests",
                        selected = selectedTemplate == "kivymd",
                        onClick = {
                            selectedTemplate = "kivymd"
                            selectedFramework = "KIVYMD"
                            projectName = "KivyMDApp"
                            appName = "KivyMD Showcase"
                            packageName = "com.atp.kivymdapp"
                        }
                    )

                    TemplateCard(
                        title = "Basic Kivy App",
                        description = "Standard cross-platform OpenGL UI widgets and touch events",
                        selected = selectedTemplate == "kivy",
                        onClick = {
                            selectedTemplate = "kivy"
                            selectedFramework = "KIVY"
                            projectName = "KivyApp"
                            appName = "Kivy App"
                            packageName = "com.atp.kivyapp"
                        }
                    )

                    TemplateCard(
                        title = "HTTP & REST Client",
                        description = "Requests, Pillow image downloader and JSON parser",
                        selected = selectedTemplate == "http_client",
                        onClick = {
                            selectedTemplate = "http_client"
                            selectedFramework = "KIVY"
                            projectName = "HttpClientApp"
                            appName = "HTTP Client"
                            packageName = "com.atp.httpclient"
                        }
                    )
                } else if (mode == "FILE") {
                    OutlinedTextField(
                        value = importedFileName,
                        onValueChange = { importedFileName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importedCode,
                        onValueChange = { importedCode = it },
                        label = { Text("Python Source Code (.py)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                } else if (mode == "ZIP") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Safe ZIP Extraction", fontWeight = FontWeight.Bold, color = CyanPrimary)
                            Text(
                                "ATP Python verifies all ZIP entry paths against canonical directory boundaries to prevent path traversal attacks (../../).",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else if (mode == "GIT") {
                    OutlinedTextField(
                        value = gitUrl,
                        onValueChange = { gitUrl = it },
                        label = { Text("Git Repository URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Clones repository into an isolated workspace directory (/projects/...).",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == "TEMPLATE") {
                        onCreateFromTemplate(projectName, appName, packageName, selectedFramework, selectedTemplate)
                    } else if (mode == "FILE") {
                        onImportFile(projectName, appName, packageName, selectedFramework, importedFileName, importedCode)
                    } else if (mode == "ZIP") {
                        onImportZip(projectName, appName, packageName, selectedFramework)
                    } else {
                        // Git template fallback
                        onCreateFromTemplate(projectName, appName, packageName, selectedFramework, "http_client")
                    }
                },
                modifier = Modifier.testTag("create_project_confirm_button")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TemplateCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF0E3A4D) else DarkSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selected) CyanSecondary else TextPrimary)
                Text(description, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
