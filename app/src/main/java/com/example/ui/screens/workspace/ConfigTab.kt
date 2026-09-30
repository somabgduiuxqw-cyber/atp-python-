package com.example.ui.screens.workspace

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProjectEntity
import com.example.ui.theme.*
import com.example.ui.viewmodels.ProjectDetailViewModel
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigTab(viewModel: ProjectDetailViewModel) {
    val project by viewModel.project.collectAsState()
    val context = LocalContext.current

    if (project == null) return
    val proj = project!!

    var appName by remember(proj.appName) { mutableStateOf(proj.appName) }
    var packageName by remember(proj.packageName) { mutableStateOf(proj.packageName) }
    var versionName by remember(proj.versionName) { mutableStateOf(proj.versionName) }
    var versionCode by remember(proj.versionCode) { mutableStateOf(proj.versionCode) }
    var orientation by remember(proj.orientation) { mutableStateOf(proj.orientation) }
    var framework by remember(proj.framework) { mutableStateOf(proj.framework) }
    var backgroundMode by remember(proj.backgroundMode) { mutableStateOf(proj.backgroundMode) }
    var creatorName by remember(proj.creatorName) { mutableStateOf(proj.creatorName) }
    var creatorUsername by remember(proj.creatorUsername) { mutableStateOf(proj.creatorUsername) }
    var showBranding by remember(proj.showBranding) { mutableStateOf(proj.showBranding) }
    var showCreator by remember(proj.showCreator) { mutableStateOf(proj.showCreator) }

    // Permissions list
    val currentPerms = remember(proj.permissionsJson) {
        val list = mutableSetOf<String>()
        try {
            val arr = JSONArray(proj.permissionsJson)
            for (i in 0 until arr.length()) list.add(arr.getString(i))
        } catch (e: Exception) {
            list.add("android.permission.INTERNET")
        }
        list.toSet()
    }
    var selectedPermissions by remember { mutableStateOf<Set<String>>(currentPerms) }

    val hasTelegram = proj.name.contains("telegram", ignoreCase = true) ||
            proj.packageName.contains("telegram", ignoreCase = true) ||
            (viewModel.analysis.value?.allImports?.any { it.moduleName == "telegram" } == true)

    fun saveChanges() {
        viewModel.updateConfig(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            orientation = orientation,
            framework = framework,
            permissions = selectedPermissions.toList(),
            backgroundMode = backgroundMode,
            creatorName = creatorName,
            creatorUsername = creatorUsername,
            showBranding = showBranding,
            showCreator = showCreator
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Save Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Android Application Configuration", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            Button(
                onClick = { saveChanges() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_config_button")
            ) {
                Text("Save Config", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // SECTION 1: App Identity
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Application Identity", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                val isValidPkg = packageName.matches(Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$"""))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (e.g. com.atp.myapp)") },
                    isError = !isValidPkg,
                    supportingText = {
                        if (!isValidPkg) Text("Invalid package name syntax", color = RedError)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = versionCode.toString(),
                        onValueChange = { versionCode = it.toIntOrNull() ?: versionCode },
                        label = { Text("Version Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text("Screen Orientation:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PORTRAIT", "LANDSCAPE", "SENSOR").forEach { ori ->
                        FilterChip(
                            selected = orientation == ori,
                            onClick = { orientation = ori },
                            label = { Text(ori, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Python Android Backend:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("KIVY", "KIVYMD", "BEEWARE", "AUTO").forEach { fw ->
                        FilterChip(
                            selected = framework == fw,
                            onClick = { framework = fw },
                            label = { Text(fw, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // SECTION 2: App Branding & Startup Screen (Prompt 27, 28, 79, 80)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("App & Startup Branding", fontWeight = FontWeight.Bold, color = AmberAccent, fontSize = 14.sp)
                Text("Configures the launcher icon and first-launch welcome screen in the generated APK.", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = creatorName,
                    onValueChange = { creatorName = it },
                    label = { Text("Creator Full Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = creatorUsername,
                    onValueChange = { creatorUsername = it },
                    label = { Text("Creator Username (e.g. @devuser)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBranding, onCheckedChange = { showBranding = it })
                    Text("Show 'Made with ATP Python' on launch", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showCreator, onCheckedChange = { showCreator = it })
                    Text("Show creator username on launch", fontSize = 13.sp)
                }

                Spacer(Modifier.height(6.dp))

                // Startup preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("STARTUP SCREEN PREVIEW", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(appName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CyanPrimary)
                        Spacer(Modifier.height(4.dp))
                        if (showBranding) {
                            Text("Made with ATP Python", fontSize = 12.sp, color = TextSecondary)
                        }
                        if (showCreator && creatorUsername.isNotBlank()) {
                            Text("Made by $creatorUsername", fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // SECTION 3: Android Permission Manager (Prompt 29, 81, 82)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Android Permissions", fontWeight = FontWeight.Bold, color = CyanSecondary, fontSize = 14.sp)
                Text("Only permissions selected here will be included in the APK AndroidManifest.xml.", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                val availablePerms = listOf(
                    "android.permission.INTERNET" to "Internet access for APIs and networking",
                    "android.permission.POST_NOTIFICATIONS" to "Push and foreground notifications",
                    "android.permission.FOREGROUND_SERVICE" to "Continuous background execution",
                    "android.permission.CAMERA" to "Access device camera sensor",
                    "android.permission.RECORD_AUDIO" to "Microphone audio recording",
                    "android.permission.ACCESS_FINE_LOCATION" to "Precise GPS location telemetry",
                    "android.permission.BLUETOOTH" to "Bluetooth connectivity and beacons"
                )

                availablePerms.forEach { (perm, desc) ->
                    val isChecked = selectedPermissions.contains(perm)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedPermissions = if (checked) {
                                    selectedPermissions + perm
                                } else {
                                    selectedPermissions - perm
                                }
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(perm.substringAfterLast("."), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(desc, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // SECTION 4: Background Operation & Battery Guidance (Prompt 30, 33, 34, 83, 84)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Background Operation & Reliability", fontWeight = FontWeight.Bold, color = AmberAccent, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE" to "None", "BACKGROUND_WORK" to "WorkManager", "FOREGROUND_SERVICE" to "Foreground Service").forEach { (mode, label) ->
                        FilterChip(
                            selected = backgroundMode == mode,
                            onClick = {
                                backgroundMode = mode
                                if (mode == "FOREGROUND_SERVICE") {
                                    selectedPermissions = selectedPermissions + "android.permission.FOREGROUND_SERVICE" + "android.permission.POST_NOTIFICATIONS"
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Special Telegram Bot note if applicable
                if (hasTelegram) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖 Telegram Bot Configuration", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Network: ✓ Internet", fontSize = 11.sp, color = GreenSuccess)
                            Text("Background: ✓ Foreground Service", fontSize = 11.sp, color = GreenSuccess)
                            Text("Notifications: ✓ Enabled", fontSize = 11.sp, color = GreenSuccess)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Important: Android may still restrict background operation on some devices. For better reliability, configure unrestricted battery usage through Android settings. Continuous 24/7 execution cannot be guaranteed by Android OS without battery exclusions.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    "Android may restrict background applications to save battery. The application will never silently modify system power settings.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Battery Settings", fontSize = 12.sp)
                }
            }
        }
    }
}
