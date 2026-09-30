package com.example.engine.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class TerminalCommandResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long
)

data class TerminalEnvironmentInfo(
    val osName: String,
    val arch: String,
    val shell: String,
    val isPythonAvailable: Boolean,
    val isPipAvailable: Boolean,
    val isPkgAvailable: Boolean, // Termux
    val isAptAvailable: Boolean
)

class TerminalEngine(private val workingDir: File) {

    suspend fun execute(command: String, timeoutSecs: Long = 10): TerminalCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val parts = parseCommandLine(command.trim())
        if (parts.isEmpty()) {
            return@withContext TerminalCommandResult(command, "", "Empty command", -1, 0)
        }

        try {
            // Check if user is calling pkg/apt on standard Android without termux
            val head = parts.first().lowercase()
            if ((head == "pkg" || head == "apt") && !hasBinary(head)) {
                return@withContext TerminalCommandResult(
                    command = command,
                    stdout = "",
                    stderr = "$head: command not found. The '$head' package manager is only available within a Termux environment.",
                    exitCode = 127,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }

            val processBuilder = ProcessBuilder(parts)
                .directory(if (workingDir.exists()) workingDir else null)

            val env = processBuilder.environment()
            env["PATH"] = (System.getenv("PATH") ?: "/system/bin:/system/xbin") + ":/data/data/com.termux/files/usr/bin"
            env["PYTHONUNBUFFERED"] = "1"

            val process = processBuilder.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append("\n")
                    }
                }
            }

            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append("\n")
                    }
                }
            }

            outThread.start()
            errThread.start()

            val completed = process.waitFor(timeoutSecs, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                stderrBuilder.append("\nProcess timed out after ${timeoutSecs}s.")
            }

            outThread.join(500)
            errThread.join(500)

            val exitCode = if (completed) process.exitValue() else -1
            val duration = System.currentTimeMillis() - startTime

            TerminalCommandResult(
                command = command,
                stdout = stdoutBuilder.toString().trimEnd(),
                stderr = stderrBuilder.toString().trimEnd(),
                exitCode = exitCode,
                durationMs = duration
            )
        } catch (e: Exception) {
            TerminalCommandResult(
                command = command,
                stdout = "",
                stderr = "Execution failed: ${e.message}",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun parseCommandLine(cmd: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        var quoteChar = ' '
        val current = StringBuilder()

        for (ch in cmd) {
            when {
                (ch == '"' || ch == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                ch == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                ch.isWhitespace() && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    private fun hasBinary(name: String): Boolean {
        val paths = (System.getenv("PATH") ?: "/system/bin:/system/xbin").split(":") + listOf("/data/data/com.termux/files/usr/bin")
        for (p in paths) {
            if (File(p, name).canExecute()) return true
        }
        return false
    }

    fun getEnvironmentInfo(): TerminalEnvironmentInfo {
        return TerminalEnvironmentInfo(
            osName = "Android " + android.os.Build.VERSION.RELEASE,
            arch = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            shell = System.getenv("SHELL") ?: "/system/bin/sh",
            isPythonAvailable = hasBinary("python3") || hasBinary("python"),
            isPipAvailable = hasBinary("pip3") || hasBinary("pip"),
            isPkgAvailable = hasBinary("pkg"),
            isAptAvailable = hasBinary("apt")
        )
    }
}
