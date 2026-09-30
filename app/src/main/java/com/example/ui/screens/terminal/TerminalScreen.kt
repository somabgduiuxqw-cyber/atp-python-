package com.example.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.engine.terminal.TerminalCommandResult
import com.example.ui.theme.*
import com.example.ui.viewmodels.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel) {
    val history by viewModel.history.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val envInfo by viewModel.envInfo.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Environment Detection Banner (Prompt 18)
        if (envInfo != null) {
            val env = envInfo!!
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Terminal Environment", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 13.sp)
                        Text("Arch: ${env.arch}", fontSize = 11.sp, color = AmberAccent, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (env.isPythonAvailable) "✓ Python" else "✗ Python",
                            color = if (env.isPythonAvailable) GreenSuccess else RedError,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (env.isPipAvailable) "✓ pip" else "✗ pip",
                            color = if (env.isPipAvailable) GreenSuccess else RedError,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (env.isPkgAvailable) "✓ pkg (Termux)" else "✗ pkg",
                            color = if (env.isPkgAvailable) GreenSuccess else RedError,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (env.isAptAvailable) "✓ apt" else "✗ apt",
                            color = if (env.isAptAvailable) GreenSuccess else RedError,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Quick command chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("pip list", "python --version", "uname -m", "ls -la", "df -h").forEach { cmd ->
                SuggestionChip(
                    onClick = {
                        inputCommand = cmd
                        viewModel.executeCommand(cmd)
                    },
                    label = { Text(cmd, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Terminal Console Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CodeBg, RoundedCornerShape(8.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (history.isEmpty()) {
                Text(
                    text = "ATP Python Terminal Session\nType a command or tap a quick chip above.\nPackage managers like 'pkg' require a Termux installation.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        CommandResultView(item = item)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Command Prompt Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = { Text("Command (e.g. pip install ...)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                leadingIcon = {
                    Text("$", color = CyanPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        val cmd = inputCommand
                        inputCommand = ""
                        viewModel.executeCommand(cmd)
                    }
                },
                enabled = !isExecuting && inputCommand.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("terminal_run_button")
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBg, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = DarkBg)
                }
            }
        }
    }
}

@Composable
private fun CommandResultView(item: TerminalCommandResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$ " + item.command,
                fontWeight = FontWeight.Bold,
                color = CyanSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${item.durationMs}ms", fontSize = 10.sp, color = TextMuted)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (item.exitCode == 0) Color(0xFF064E3B) else Color(0xFF450A0A),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        "exit: ${item.exitCode}",
                        fontSize = 10.sp,
                        color = if (item.exitCode == 0) GreenSuccess else RedError,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (item.stdout.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                item.stdout,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextPrimary,
                lineHeight = 16.sp
            )
        }

        if (item.stderr.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                item.stderr,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = RedError,
                lineHeight = 16.sp
            )
        }
    }
}
