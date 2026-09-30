package com.example.engine.analyzer

import com.example.data.model.CandidatePackage
import com.example.data.model.ImportConfidence

data class KnownMapping(
    val importName: String,
    val packageName: String,
    val confidence: ImportConfidence = ImportConfidence.CONFIRMED,
    val isAndroidCompatible: Boolean = true,
    val requiresNativeRecipe: Boolean = false,
    val nativeRecipeName: String? = null,
    val description: String = "",
    val ambiguousCandidates: List<CandidatePackage> = emptyList()
)

object ImportMappingDatabase {
    private val MAPPINGS = mutableMapOf<String, KnownMapping>()
    private val USER_CORRECTIONS = mutableMapOf<String, String>()

    init {
        // Confirmed standard packages
        register(KnownMapping("requests", "requests", ImportConfidence.CONFIRMED, true, false, null, "HTTP library for Python"))
        register(KnownMapping("PIL", "Pillow", ImportConfidence.CONFIRMED, true, false, null, "Python Imaging Library fork"))
        register(KnownMapping("yaml", "PyYAML", ImportConfidence.CONFIRMED, true, false, null, "YAML parser and emitter"))
        register(KnownMapping("bs4", "beautifulsoup4", ImportConfidence.CONFIRMED, true, false, null, "Screen-scraping HTML/XML parser"))
        register(KnownMapping("telegram", "python-telegram-bot", ImportConfidence.CONFIRMED, true, false, null, "Python interface for Telegram Bot API"))
        register(KnownMapping("kivy", "kivy", ImportConfidence.CONFIRMED, true, true, "kivy", "Open source Python framework for GUI development"))
        register(KnownMapping("kivymd", "kivymd", ImportConfidence.CONFIRMED, true, false, null, "Material Design widgets for Kivy"))
        register(KnownMapping("toga", "toga", ImportConfidence.CONFIRMED, true, false, null, "BeeWare cross-platform GUI toolkit"))
        register(KnownMapping("briefcase", "briefcase", ImportConfidence.CONFIRMED, true, false, null, "BeeWare packaging tool"))
        register(KnownMapping("numpy", "numpy", ImportConfidence.CONFIRMED, true, true, "numpy", "Scientific computing with Python (requires NDK recipe)"))
        register(KnownMapping("pandas", "pandas", ImportConfidence.CONFIRMED, true, true, "pandas", "Data analysis library (requires NDK recipe)"))
        register(KnownMapping("cv2", "opencv-python", ImportConfidence.CONFIRMED, true, true, "opencv", "OpenCV computer vision library (requires native Android build)"))
        register(KnownMapping("dateutil", "python-dateutil", ImportConfidence.CONFIRMED, true, false, null, "Extensions to standard Python datetime"))
        register(KnownMapping("jwt", "PyJWT", ImportConfidence.CONFIRMED, true, false, null, "JSON Web Token implementation"))
        register(KnownMapping("dotenv", "python-dotenv", ImportConfidence.CONFIRMED, true, false, null, "Read key-value pairs from a .env file"))
        register(KnownMapping("serial", "pyserial", ImportConfidence.CONFIRMED, true, false, null, "Python serial port access"))
        register(KnownMapping("cryptography", "cryptography", ImportConfidence.CONFIRMED, true, true, "cryptography", "Cryptographic recipes and primitives"))
        register(KnownMapping("httpx", "httpx", ImportConfidence.CONFIRMED, true, false, null, "Next-generation HTTP client"))
        register(KnownMapping("flask", "Flask", ImportConfidence.CONFIRMED, true, false, null, "Lightweight WSGI web application framework"))
        register(KnownMapping("fastapi", "fastapi", ImportConfidence.CONFIRMED, true, false, null, "Fast, high-performance web framework"))
        register(KnownMapping("pydantic", "pydantic", ImportConfidence.CONFIRMED, true, false, null, "Data validation using Python type hints"))
        register(KnownMapping("aiohttp", "aiohttp", ImportConfidence.CONFIRMED, true, true, "aiohttp", "Async HTTP client/server for asyncio"))
        register(KnownMapping("fitz", "PyMuPDF", ImportConfidence.CONFIRMED, false, true, null, "MuPDF rendering library (requires custom recipe)"))
        register(KnownMapping("docx", "python-docx", ImportConfidence.CONFIRMED, true, false, null, "Create and modify Word documents"))
        register(KnownMapping("pptx", "python-pptx", ImportConfidence.CONFIRMED, true, false, null, "Create and modify PowerPoint presentations"))
        register(KnownMapping("skimage", "scikit-image", ImportConfidence.CONFIRMED, true, true, "scikit-image", "Image processing algorithms"))
        register(KnownMapping("sklearn", "scikit-learn", ImportConfidence.CONFIRMED, true, true, "scikit-learn", "Machine learning tools"))
        register(KnownMapping("websocket", "websocket-client", ImportConfidence.CONFIRMED, true, false, null, "WebSocket client for Python"))
        register(KnownMapping("magic", "python-magic", ImportConfidence.CONFIRMED, false, true, null, "File type identification using libmagic"))
        register(KnownMapping("plyer", "plyer", ImportConfidence.CONFIRMED, true, false, null, "Platform-independent API to features of your hardware (GPS, Camera, Accelerometer)"))

        // Ambiguous imports: explicitly declare multiple candidates!
        register(
            KnownMapping(
                importName = "color",
                packageName = "", // No default!
                confidence = ImportConfidence.UNKNOWN,
                isAndroidCompatible = true,
                requiresNativeRecipe = false,
                description = "Ambiguous import: multiple candidate packages provide terminal/graphics color utilities",
                ambiguousCandidates = listOf(
                    CandidatePackage("colorama", "Cross-platform colored terminal text", "0.4.6", 0.9f, true, "Pure Python"),
                    CandidatePackage("ansicolors", "ANSI colors for Python console output", "1.1.8", 0.7f, true, "Pure Python"),
                    CandidatePackage("termcolor", "ANSII Color formatting for output in terminal", "2.4.0", 0.8f, true, "Pure Python"),
                    CandidatePackage("colored", "Very simple Python library for color and formatting in terminal", "2.2.4", 0.7f, true, "Pure Python"),
                    CandidatePackage("color", "Color-related Python package", "0.1.0", 0.4f, true, "Pure Python")
                )
            )
        )
    }

    private fun register(mapping: KnownMapping) {
        MAPPINGS[mapping.importName.lowercase()] = mapping
    }

    fun findMapping(importName: String, rawStatement: String = "", submodules: List<String> = emptyList()): KnownMapping? {
        val key = importName.lowercase()

        // Check user override first
        val userOverride = USER_CORRECTIONS[key]
        if (userOverride != null) {
            return KnownMapping(importName, userOverride, ImportConfidence.CONFIRMED, true, false, null, "User corrected mapping")
        }

        // Context-aware API inspection for telegram
        if (key == "telegram") {
            if (rawStatement.contains("telegram.ext") || submodules.contains("ext") || rawStatement.contains("Bot") || rawStatement.contains("Application")) {
                return KnownMapping("telegram", "python-telegram-bot", ImportConfidence.CONFIRMED, true, false, null, "Python Telegram Bot API (Application/Bot class detected)")
            }
            if (rawStatement.contains("TelegramClient")) {
                return KnownMapping("telegram", "telethon", ImportConfidence.CONFIRMED, true, false, null, "Telethon MTProto Telegram client detected")
            }
        }

        // Context-aware API inspection for PIL
        if (key == "pil") {
            return KnownMapping("PIL", "Pillow", ImportConfidence.CONFIRMED, true, false, null, "Pillow library (PIL fork)")
        }

        return MAPPINGS[key]
    }

    fun setUserCorrection(importName: String, packageName: String) {
        USER_CORRECTIONS[importName.lowercase()] = packageName.trim()
    }

    fun getAllMappings(): Map<String, KnownMapping> = MAPPINGS
}
