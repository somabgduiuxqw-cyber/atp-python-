package com.example.engine.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WorkspaceManager(private val context: Context) {

    val projectsDir: File = File(context.filesDir, "projects").apply { mkdirs() }
    val packageCacheDir: File = File(context.cacheDir, "packages").apply { mkdirs() }
    val buildTmpDir: File = File(context.cacheDir, "build_tmp").apply { mkdirs() }
    val outputApkDir: File = File(context.filesDir, "output").apply { mkdirs() }

    suspend fun createProjectWorkspace(projectId: Long, templateId: String): File = withContext(Dispatchers.IO) {
        val dir = File(projectsDir, "proj_$projectId").apply { mkdirs() }
        File(dir, "assets").mkdirs()

        when (templateId) {
            "kivy" -> {
                File(dir, "main.py").writeText(
                    """from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
import json
import os

class MainApp(App):
    def build(self):
        layout = BoxLayout(orientation='vertical', padding=20, spacing=10)
        self.label = Label(text="Welcome to ATP Python Kivy App", font_size='22sp')
        btn = Button(text="Tap Me", size_hint=(1, 0.3))
        btn.bind(on_press=self.on_button_click)
        layout.add_widget(self.label)
        layout.add_widget(btn)
        return layout

    def on_button_click(self, instance):
        self.label.text = "Hello from Android!"

if __name__ == '__main__':
    MainApp().run()
"""
                )
                File(dir, "requirements.txt").writeText("kivy\n")
            }
            "kivymd" -> {
                File(dir, "main.py").writeText(
                    """from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivymd.uix.button import MDRaisedButton
from kivymd.uix.label import MDLabel
import requests
import json

class MainMDApp(MDApp):
    def build(self):
        self.theme_cls.primary_palette = "Indigo"
        screen = MDScreen()
        label = MDLabel(
            text="ATP Python + KivyMD",
            halign="center",
            font_style="H4"
        )
        button = MDRaisedButton(
            text="Fetch API Data",
            pos_hint={"center_x": 0.5, "center_y": 0.4},
            on_release=self.fetch_data
        )
        screen.add_widget(label)
        screen.add_widget(button)
        return screen

    def fetch_data(self, *args):
        try:
            r = requests.get("https://httpbin.org/get", timeout=5)
            print("Status code:", r.status_code)
        except Exception as e:
            print("Network error:", e)

if __name__ == '__main__':
    MainMDApp().run()
"""
                )
                File(dir, "requirements.txt").writeText("kivy\nkivymd\nrequests\n")
            }
            "telegram_bot" -> {
                File(dir, "main.py").writeText(
                    """import logging
import requests
from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("Hello! I am running continuously via ATP Python Android Foreground Service.")

async def status(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("Device bot active and responsive.")

def main():
    TOKEN = "YOUR_TELEGRAM_BOT_TOKEN_HERE"
    app = Application.builder().token(TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("status", status))
    print("Bot starting polling loop...")
    app.run_polling()

if __name__ == "__main__":
    main()
"""
                )
                File(dir, "requirements.txt").writeText("python-telegram-bot\nrequests\n")
            }
            "http_client" -> {
                File(dir, "main.py").writeText(
                    """import requests
import json
from PIL import Image
import os

def fetch_json(url):
    response = requests.get(url, timeout=10)
    response.raise_for_status()
    return response.json()

if __name__ == "__main__":
    print("Fetching data...")
    data = fetch_json("https://api.github.com/zen")
    print("GitHub Zen:", data)
"""
                )
                File(dir, "requirements.txt").writeText("requests\nPillow\n")
            }
            "test_multidep" -> {
                // Test project corresponding to section 101 in prompt!
                File(dir, "main.py").writeText(
                    """from kivy.app import App
from kivy.uix.label import Label
import requests
from PIL import Image
import json
import os

class MultiDepApp(App):
    def build(self):
        return Label(text="Multi-dependency Test")

if __name__ == '__main__':
    MultiDepApp().run()
"""
                )
                File(dir, "requirements.txt").writeText("kivy\nrequests\nPillow\n")
            }
            else -> {
                File(dir, "main.py").writeText(
                    """# ATP Python Application
import sys
import os

def main():
    print("Hello from ATP Python!")

if __name__ == "__main__":
    main()
"""
                )
                File(dir, "requirements.txt").writeText("")
            }
        }
        dir
    }

    suspend fun importSinglePythonFile(projectId: Long, fileName: String, content: String): File = withContext(Dispatchers.IO) {
        val dir = File(projectsDir, "proj_$projectId").apply { mkdirs() }
        File(dir, "assets").mkdirs()
        File(dir, "main.py").writeText(content)
        File(dir, "requirements.txt").writeText("")
        dir
    }

    /**
     * Extracts ZIP safely to prevent Zip Path Traversal attacks (../../system/file).
     */
    suspend fun extractZipSafely(zipInputStream: InputStream, targetDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            targetDir.mkdirs()
            val destCanonical = targetDir.canonicalPath
            val zis = ZipInputStream(BufferedInputStream(zipInputStream))
            var entry: ZipEntry?

            while (zis.nextEntry.also { entry = it } != null) {
                val currentEntry = entry ?: break
                val destinationFile = File(targetDir, currentEntry.name)
                val canonicalDestination = destinationFile.canonicalPath

                // Strict Path Traversal Protection
                if (!canonicalDestination.startsWith(destCanonical + File.separator) && canonicalDestination != destCanonical) {
                    throw SecurityException("ZIP traversal path detected in archive: ${currentEntry.name}")
                }

                if (currentEntry.isDirectory) {
                    destinationFile.mkdirs()
                } else {
                    destinationFile.parentFile?.mkdirs()
                    FileOutputStream(destinationFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
            zis.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports project directory into clean ZIP archive.
     */
    suspend fun exportProjectZip(projectDir: File, outputZip: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputZip.parentFile?.mkdirs()
            val zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip)))
            projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = file.relativeTo(projectDir).path.replace("\\", "/")
                // Skip cache or temp dirs
                if (!relPath.startsWith("__pycache__") && !relPath.startsWith(".git")) {
                    val entry = ZipEntry(relPath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            zos.finish()
            zos.close()
            Result.success(outputZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStorageBreakdown(): Map<String, Long> {
        return mapOf(
            "Projects" to getDirectorySize(projectsDir),
            "Package Cache" to getDirectorySize(packageCacheDir),
            "Build Cache" to getDirectorySize(buildTmpDir),
            "Output APKs" to getDirectorySize(outputApkDir)
        )
    }

    fun clearCaches(): Boolean {
        return try {
            packageCacheDir.deleteRecursively()
            packageCacheDir.mkdirs()
            buildTmpDir.deleteRecursively()
            buildTmpDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
