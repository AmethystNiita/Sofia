package com.amethystniita.sofia

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import com.amethystniita.sofia.ui.theme.Pearls


val ToneTan = Color(0xFFD0B49F)       
val ToneRose = Color(0xFFAE827A)      
val ToneUmber = Color(0xFF946F5F)     
val ToneBackground = Color(0xFFFAF6F0)
val ToneDarkText = Color(0xFF3E2D27)  

private val SofiaColorScheme = lightColorScheme(
    primary = ToneUmber,
    secondary = ToneRose,
    background = ToneBackground,
    surface = ToneTan,
    surfaceVariant = ToneTan.copy(alpha = 0.5f),
    onPrimary = Color.White,
    onBackground = ToneDarkText,
    onSurface = ToneDarkText
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        setContent {
            MaterialTheme(colorScheme = SofiaColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SofiaMainMenu()
                }
            }
        }
    }
}

@Composable
fun SofiaMainMenu() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var showConfirmDialog by remember { mutableStateOf(false) }
    var targetPackages by remember { mutableStateOf<List<String>>(emptyList()) }

    
    var hasAccessibility by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isShizukuBinderAlive by remember { mutableStateOf(AdbEngine.isBinderAlive()) }
    var hasShizukuPermission by remember { mutableStateOf(AdbEngine.hasPermission()) }
    var isRootAvailable by remember { mutableStateOf(RootEngine.isRootAvailable()) }

    val refreshPermissions = {
        hasAccessibility = isAccessibilityEnabled(context)
        hasOverlayPermission = Settings.canDrawOverlays(context)
        isShizukuBinderAlive = AdbEngine.isBinderAlive()
        hasShizukuPermission = AdbEngine.hasPermission()
        isRootAvailable = RootEngine.isRootAvailable()
    }

    
    LaunchedEffect(Unit) {
        while (true) {
            refreshPermissions()
            delay(1500)
        }
    }

    
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }

        val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshPermissions() }
        val binderDeadListener = Shizuku.OnBinderDeadListener { refreshPermissions() }
        val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshPermissions() }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        }
    }

    
    val bestEngineText = when {
        isRootAvailable -> "Root"
        hasShizukuPermission -> "Shizuku ADB"
        hasAccessibility && hasOverlayPermission -> "Accessibility"
        else -> "Setup required"
    }

    val bestEngineButtonText = when {
        isRootAvailable || hasShizukuPermission || (hasAccessibility && hasOverlayPermission) -> "Start"
        else -> "Setup needed"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sofia),
                contentDescription = "Sofia",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Sofia",
                fontSize = 44.sp,
                fontFamily = Pearls,
                color = ToneUmber
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ToneUmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ToneTan.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Best method for you",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                fontFamily = Pearls,
                                color = ToneUmber
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recommended method: $bestEngineText",
                            fontSize = 15.sp,
                            fontFamily = Pearls,
                            color = ToneDarkText.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                when {
                                    isRootAvailable -> {
                                        val success = RootEngine.clearAllCaches()
                                        val msg = if (success) "Root cache wipe completed!" else "Root access failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                    hasShizukuPermission -> {
                                        val success = AdbEngine.trimCaches()
                                        val msg = if (success) "ADB cache trim completed!" else "ADB cache trim failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                    hasAccessibility && hasOverlayPermission -> {
                                        targetPackages = AppUtils.getInstalledAppPackages(context)
                                        showConfirmDialog = true
                                    }
                                    else -> {
                                        Toast.makeText(context, "Please follow the setup steps on one of the cards below.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToneRose),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = bestEngineButtonText,
                                fontSize = 16.sp,
                                fontFamily = Pearls,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            
            item {
                val accessibilityButtonText = when {
                    !hasOverlayPermission -> "Grant overlay"
                    !hasAccessibility -> "Grant access"
                    else -> "Start"
                }

                val accessibilityStatusText = when {
                    hasAccessibility && hasOverlayPermission -> "Ready"
                    !hasOverlayPermission -> "Needs overlay"
                    else -> "Needs service"
                }

                EngineCard(
                    title = "Accessibility",
                    description = """
1. Tap 'Grant Overlay' to allow floating controls over Settings.
2. Tap 'Grant Access' to open Android Accessibility Settings.
3. Find 'Installed Apps' -> select 'Sofia' -> turn it on.
4. Tap 'Start' to clear the cache.
(Note: It might take a while as it opens settings screens one-by-one.)
                    """.trimIndent(),
                    isReady = hasAccessibility && hasOverlayPermission,
                    statusText = accessibilityStatusText,
                    buttonText = accessibilityButtonText,
                    onClick = {
                        if (!hasOverlayPermission) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else if (!hasAccessibility) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } else {
                            targetPackages = AppUtils.getInstalledAppPackages(context)
                            showConfirmDialog = true
                        }
                    }
                )
            }

            
            item {
                val adbStatusText = when {
                    hasShizukuPermission -> "Ready"
                    isShizukuBinderAlive -> "Needs permission"
                    else -> "Not running"
                }

                val adbButtonText = when {
                    hasShizukuPermission -> "Start"
                    isShizukuBinderAlive -> "Grant Shizuku"
                    else -> "Open Shizuku"
                }

                EngineCard(
                    title = "Shizuku ADB",
                    description = """
1. Install and open the Shizuku app.
2. Enable Wireless Debugging in Android Developer Options.
3. Pair and tap 'Start' inside Shizuku.
4. Tap 'Grant' and confirm when prompted.
5. Tap 'Start' to clear the cache.
(Note: It clears the cache instantly in the background.)
                    """.trimIndent(),
                    isReady = hasShizukuPermission,
                    statusText = adbStatusText,
                    buttonText = adbButtonText,
                    onClick = {
                        if (!isShizukuBinderAlive) {
                            
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "Shizuku not found, please install it first.", Toast.LENGTH_LONG).show()
                            }
                        } else if (!hasShizukuPermission) {
                            AdbEngine.requestPermission()
                        } else {
                            val success = AdbEngine.trimCaches()
                            val msg = if (success) "ADB cache trim completed!" else "ADB cache trim failed."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            
            item {
                EngineCard(
                    title = "Root",
                    description = """
1. Requires a rooted phone.
2. Open your root manager app to grant root permission to Sofia if needed.
2. Tap 'Start' to clear the cache.
(Note: It clears the cache instantly at system level.)
                    """.trimIndent(),
                    isReady = isRootAvailable,
                    statusText = if (isRootAvailable) "Ready" else "No root",
                    buttonText = if (isRootAvailable) "Start" else "Grant root",
                    onClick = {
                        val success = RootEngine.clearAllCaches()
                        val msg = if (success) "Root cache wipe completed!" else "Root access denied or not available."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = ToneBackground,
            title = {
                val appText = if (targetPackages.size == 1) "app" else "apps"
                Text(
                    text = "Start cleaning ${targetPackages.size} $appText?",
                    fontFamily = Pearls,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ToneUmber
                )
            },
            text = {
                Text(
                    text = "Sofia will now automatically cycle through installed apps to clear their caches. Tap the floating icon anytime to pause, or hold it to stop.",
                    fontFamily = Pearls,
                    fontSize = 16.sp,
                    color = ToneDarkText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        startSofiaCleaningSequence(context, targetPackages)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToneRose)
                ) {
                    Text("Start", fontFamily = Pearls, fontSize = 16.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", fontFamily = Pearls, fontSize = 16.sp, color = ToneUmber)
                }
            }
        )
    }
}

@Composable
fun EngineCard(
    title: String,
    description: String,
    isReady: Boolean,
    statusText: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ToneTan.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    fontFamily = Pearls,
                    color = ToneUmber
                )

                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isReady) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontFamily = Pearls,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 15.sp,
                fontFamily = Pearls,
                color = ToneDarkText
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToneRose),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontFamily = Pearls,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

fun startSofiaCleaningSequence(context: Context, appPackages: List<String>) {
    if (!Settings.canDrawOverlays(context)) {
        Toast.makeText(context, "Please grant the overlay permission.", Toast.LENGTH_SHORT).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
        return
    }

    if (appPackages.isEmpty()) {
        Toast.makeText(context, "No third-party apps found to clean.", Toast.LENGTH_SHORT).show()
        return
    }

    SofiaAccessibilityService.packageList = appPackages
    SofiaAccessibilityService.currentPackageIndex = 0
    SofiaAccessibilityService.isRunning = true
    SofiaAccessibilityService.isCleaningPaused = false

    context.startService(Intent(context, FloatingControlService::class.java))

    val firstPackage = appPackages[0]
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$firstPackage")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
}