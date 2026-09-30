package com.example.engine.build

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.model.DeviceResourceReport
import com.example.data.model.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class EnvironmentDetector(private val context: Context) {

    suspend fun inspectTools(): List<ToolStatus> = withContext(Dispatchers.IO) {
        val tools = mutableListOf<ToolStatus>()

        tools.add(checkTool("python", "Python 3", listOf("python3", "python"), "--version", "3.8+"))
        tools.add(checkTool("pip", "pip Package Manager", listOf("pip3", "pip", "python3 -m pip"), "--version", "21.0+"))
        tools.add(checkTool("java", "Java JDK", listOf("javac", "java"), "-version", "OpenJDK 11/17"))
        tools.add(checkTool("android_sdk", "Android SDK", listOf("sdkmanager"), "--version", "API 33-35"))
        tools.add(checkTool("gradle", "Gradle Build Tool", listOf("gradle"), "--version", "7.0 - 8.x"))
        tools.add(checkTool("ndk", "Android NDK", listOf("ndk-build"), "--version", "r25b+"))
        tools.add(checkTool("buildozer", "Buildozer", listOf("buildozer"), "--version", "1.5+"))
        tools.add(checkTool("p4a", "python-for-android", listOf("p4a"), "--version", "2024+"))
        tools.add(checkTool("briefcase", "BeeWare Briefcase", listOf("briefcase"), "--version", "0.3+"))

        tools
    }

    private fun checkTool(
        id: String,
        name: String,
        candidateCommands: List<String>,
        versionFlag: String,
        requirement: String
    ): ToolStatus {
        for (cmd in candidateCommands) {
            val res = executeCommand(cmd, versionFlag)
            if (res.exitCode == 0 && res.output.isNotEmpty()) {
                val cleaned = res.output.lines().firstOrNull()?.trim() ?: "Installed"
                return ToolStatus(
                    id = id,
                    name = name,
                    command = cmd,
                    isInstalled = true,
                    detectedVersion = cleaned,
                    expectedRequirement = requirement,
                    notes = "Executable verified in environment path.",
                    isCompatible = true
                )
            }
        }

        // Also check standard Termux or common Android locations if installed
        val termuxPrefix = "/data/data/com.termux/files/usr/bin/"
        for (cmd in candidateCommands) {
            val f = File(termuxPrefix, cmd.split(" ").first())
            if (f.exists() && f.canExecute()) {
                val res = executeCommand(f.absolutePath, versionFlag)
                if (res.exitCode == 0) {
                    val cleaned = res.output.lines().firstOrNull()?.trim() ?: "Installed (Termux)"
                    return ToolStatus(
                        id = id,
                        name = name,
                        command = f.absolutePath,
                        isInstalled = true,
                        detectedVersion = cleaned,
                        expectedRequirement = requirement,
                        notes = "Detected in Termux path ($termuxPrefix)",
                        isCompatible = true
                    )
                }
            }
        }

        return ToolStatus(
            id = id,
            name = name,
            command = candidateCommands.first(),
            isInstalled = false,
            detectedVersion = "Not Installed",
            expectedRequirement = requirement,
            notes = "Executable not found in local system PATH. Use Online Build or install prerequisites.",
            isCompatible = false
        )
    }

    private fun executeCommand(command: String, arg: String): ExecResult {
        return try {
            val parts = command.split(" ").toMutableList()
            parts.add(arg)
            val process = ProcessBuilder(parts)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(2, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return ExecResult(-1, "Timeout")
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText().trim()
            ExecResult(process.exitValue(), output)
        } catch (e: Exception) {
            ExecResult(-1, e.message ?: "Failed")
        }
    }

    private data class ExecResult(val exitCode: Int, val output: String)

    fun checkDeviceResources(): DeviceResourceReport {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        val totalStorageMb = bytesTotal / (1024 * 1024)
        val availStorageMb = bytesAvailable / (1024 * 1024)

        val abis = Build.SUPPORTED_ABIS.toList()
        val primaryAbi = abis.firstOrNull() ?: "arm64-v8a"

        val hasEnoughRam = availRamMb >= 1800 // Local NDK/Gradle compilation requires min 1.8GB free RAM
        val hasEnoughStorage = availStorageMb >= 3500 // Min 3.5GB free storage for toolchains and builds
        val isViable = hasEnoughRam && hasEnoughStorage

        val recommendation = when {
            !hasEnoughRam -> "Device has only ${availRamMb}MB free RAM (1800MB required for local NDK compilation). We strongly recommend using Online Build."
            !hasEnoughStorage -> "Device has only ${availStorageMb}MB free storage (3500MB required for toolchains). Use Online Build."
            else -> "Device meets minimum RAM and storage thresholds. Ensure local SDK/NDK tools are installed if building locally."
        }

        return DeviceResourceReport(
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            totalStorageMb = totalStorageMb,
            availableStorageMb = availStorageMb,
            primaryAbi = primaryAbi,
            supportedAbis = abis,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            isLocalBuildViable = isViable,
            recommendation = recommendation
        )
    }
}
