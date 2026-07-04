package com.example

import android.os.Bundle
import android.speech.RecognizerIntent
import android.content.Intent
import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessageEntity
import com.example.data.DeviceEntity
import com.example.data.LogEntity
import com.example.ui.HomeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeOsApp()
            }
        }
    }
}

@Composable
fun HomeOsApp(viewModel: HomeViewModel = viewModel()) {
    val devices by viewModel.devices.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentTab = viewModel.currentTab,
                onTabSelected = { viewModel.currentTab = it }
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg,
                            Color(0xFF0F1116)
                        )
                    )
                )
        ) {
            // Ambient glowing animated background for Lyra
            val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
            val alphaGlow by infiniteTransition.animateFloat(
                initialValue = 0.05f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = alphaGlow }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanAccent.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 1200f
                        )
                    )
            )

            // Status bar clock / battery mock row at the top
            Column(modifier = Modifier.fillMaxSize()) {
                StatusBarRow()

                // Header view
                HeaderView(activeTab = viewModel.currentTab)

                // Main sliding layout content
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    AnimatedContent(
                        targetState = viewModel.currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                        },
                        label = "TabTransition"
                    ) { tab ->
                        when (tab) {
                            "home" -> HomeScreen(
                                viewModel = viewModel,
                                devices = devices,
                                logs = logs
                            )
                            "devices" -> DevicesScreen(
                                viewModel = viewModel,
                                devices = devices
                            )
                            "logs" -> LogsScreen(
                                viewModel = viewModel,
                                logs = logs
                            )
                            "config" -> ConfigScreen(
                                viewModel = viewModel,
                                chatMessages = chatMessages
                            )
                        }
                    }
                }
            }
        }
    }

    // Add device Dialog
    if (viewModel.showAddDeviceDialog) {
        AddDeviceDialog(viewModel = viewModel)
    }

    // Quick Task trigger Dialog
    if (viewModel.showQuickActionDialog) {
        QuickActionDialog(viewModel = viewModel)
    }

    // Lyra API Settings Dialog
    if (viewModel.showApiSettingsDialog) {
        LyraApiSettingsDialog(viewModel = viewModel)
    }

    // Voice Control Dialog
    if (viewModel.showVoiceDialog) {
        LyraVoiceControlDialog(viewModel = viewModel)
    }
}

@Composable
fun StatusBarRow() {
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTime,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("status_bar_clock")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wi-Fi Icon representation
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi Status",
                tint = EmeraldGreen,
                modifier = Modifier.size(16.dp)
            )
            // System secure status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(EmeraldGreen, shape = CircleShape)
            )
        }
    }
}

@Composable
fun HeaderView(activeTab: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Home_OS",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "DEPLOYMENT CYCLE: v2.4.0-STABLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Profile Avatar Widget with glow
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(IndigoGlow, PurpleGlow)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .clickable { /* Profile info */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Profile",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            // Active notification or lock badge
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(EmeraldGreen, shape = CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2.dp))
                    .border(1.5.dp, ObsidianBg, CircleShape)
            )
        }
    }
}

@Composable
fun BottomNavBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .drawBehind {
                drawLine(
                    color = BorderWhite,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        color = NavBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentTab == "home",
                onClick = { onTabSelected("home") },
                testTag = "nav_home_tab"
            )
            NavItem(
                label = "Devices",
                icon = Icons.Default.Devices,
                selected = currentTab == "devices",
                onClick = { onTabSelected("devices") },
                testTag = "nav_devices_tab"
            )
            NavItem(
                label = "Logs",
                icon = Icons.Default.History,
                selected = currentTab == "logs",
                onClick = { onTabSelected("logs") },
                testTag = "nav_logs_tab"
            )
            NavItem(
                label = "Lyra",
                icon = Icons.AutoMirrored.Filled.Chat,
                selected = currentTab == "config",
                onClick = { onTabSelected("config") },
                testTag = "nav_config_tab"
            )
        }
    }
}

@Composable
fun RowScope.NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    val opacity by animateFloatAsState(targetValue = if (selected) 1f else 0.45f, label = "opacity")
    val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1f, label = "scale")

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(30.dp)
                .background(
                    color = if (selected) IndigoGlow.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) IndigoGlow else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) IndigoGlow else TextSecondary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer(alpha = opacity)
        )
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    devices: List<DeviceEntity>,
    logs: List<LogEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            LyraPresenceCard(viewModel = viewModel)
        }

        item {
            EnergyAndDevicesSummary(devices = devices)
        }

        item {
            RoomsHorizontalScroll(viewModel = viewModel, devices = devices)
        }

        item {
            ActiveScenesPanel(viewModel = viewModel)
        }

        item {
            DashboardDeviceControls(viewModel = viewModel, devices = devices)
        }

        item {
            SecurityAndModulesGrid(viewModel = viewModel, devices = devices)
        }

        item {
            RecentActionCard(logs = logs)
        }
    }
}

@Composable
fun EnergyAndDevicesSummary(devices: List<DeviceEntity>) {
    val activeCount = devices.count { it.status }
    val totalCount = devices.size
    val offlineCount = if (totalCount > activeCount) totalCount - activeCount else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Energy Card
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENERGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "⚡ 340W",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Current Draw",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Est. Monthly: ₹450",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Active Devices Card
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NODES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "● Online",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$activeCount Active Nodes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$offlineCount Offline / Paused",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RoomsHorizontalScroll(viewModel: HomeViewModel, devices: List<DeviceEntity>) {
    val rooms = listOf("Living Room", "Bedroom", "Office", "Kitchen")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPATIAL ZONES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Text(
                text = "See All",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoGlow,
                modifier = Modifier.clickable { viewModel.currentTab = "devices" }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rooms.forEach { room ->
                val roomDevices = devices.filter { it.room.equals(room, ignoreCase = true) }
                val activeInRoom = roomDevices.count { it.status }
                val totalInRoom = roomDevices.size
                val isAnyOn = activeInRoom > 0

                val bgGlow = if (isAnyOn) AmberYellow.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f)
                val borderGlow = if (isAnyOn) AmberYellow.copy(alpha = 0.2f) else BorderWhite

                Surface(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { viewModel.currentTab = "devices" },
                    shape = RoundedCornerShape(24.dp),
                    color = bgGlow,
                    border = BorderStroke(1.dp, borderGlow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (room) {
                                    "Bedroom" -> "🛏"
                                    "Office" -> "💼"
                                    "Kitchen" -> "🍳"
                                    else -> "🛋"
                                },
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (totalInRoom > 0) EmeraldGreen else TextSecondary, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = room,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (totalInRoom > 0) "$activeInRoom/$totalInRoom active" else "No paired nodes",
                            fontSize = 11.sp,
                            color = if (isAnyOn) AmberYellow else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveScenesPanel(viewModel: HomeViewModel) {
    val scenes = listOf("Study Mode", "Night Mode", "Movie Mode")
    val infiniteTransition = rememberInfiniteTransition(label = "sceneBreathe")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACTIVE SCENES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            scenes.forEach { scene ->
                val isActive = viewModel.activeScene == scene
                val borderCol = if (isActive) AmberYellow.copy(alpha = glowAlpha + 0.15f) else BorderWhite
                val bgCol = if (isActive) AmberYellow.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)
                val textCol = if (isActive) AmberYellow else TextSecondary

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.activateScene(scene) }
                        .testTag("scene_button_${scene.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(24.dp),
                    color = bgCol,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (scene) {
                                "Study Mode" -> "📖"
                                "Night Mode" -> "🌙"
                                "Movie Mode" -> "🎬"
                                else -> "✨"
                            },
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scene.replace(" Mode", ""),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                        Text(
                            text = if (isActive) "ACTIVE" else "READY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyraPresenceCard(viewModel: HomeViewModel) {
    // Elegant infinite pulse breathing animation for the neural orb
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lyra_presence_card"),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, BorderWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Absolute position background radial glow effects
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = IndigoGlow.copy(alpha = 0.05f),
                    radius = 300f,
                    center = center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Pulsating Neural Orb
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { viewModel.currentTab = "config" },
                    contentAlignment = Alignment.Center
                ) {
                    val coreColor = CyanAccent

                    // Glowing outer ring 2
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = scale * 1.15f, scaleY = scale * 1.15f, alpha = 0.12f)
                            .background(coreColor, shape = CircleShape)
                    )
                    // Glowing outer ring 1
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = 0.22f)
                            .background(coreColor.copy(alpha = 0.5f), shape = CircleShape)
                    )
                    // Inner swirling core
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .graphicsLayer(rotationZ = rotateAngle)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(coreColor, coreColor.copy(alpha = 0.3f), coreColor)
                                ),
                                shape = CircleShape
                            )
                    )
                    // Core point
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val activeName = "Lyra Core AI"

                val coreColor = CyanAccent

                Text(
                    text = activeName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                val modeLabel = when (viewModel.aiMode) {
                    "LOW_LATENCY" -> "FLASH LITE • ULTRA SNAPPY LATENCY"
                    "DEEP_THINKING" -> "HIGH REASONING ACTIVE • DEEP THOUGHT"
                    "GROUNDED" -> "GROUNDED COGNITION • GOOGLE SEARCH"
                    else -> "STANDARD CORE • INTEGRATED ECOSYSTEM"
                }

                Text(
                    text = modeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = coreColor,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.showQuickActionDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("quick_task_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Quick Command",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = { viewModel.currentTab = "config" },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("ask_lyra_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoGlow),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Ask Lyra",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityAndModulesGrid(viewModel: HomeViewModel, devices: List<DeviceEntity>) {
    val activeCount = devices.count { it.status }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Integrity Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { viewModel.applySecurityPatch() },
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Active",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SECURITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "System secure\nIntegrity 100%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Active Modules Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { viewModel.reEncryptMatterBridge() },
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AmberYellow.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = "Active Modules",
                        tint = AmberYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "MODULES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "12 Core Active\n$activeCount node power",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AmberYellow,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RecentActionCard(logs: List<LogEntity>) {
    val recentLog = logs.firstOrNull() ?: LogEntity(
        message = "No actions processed yet.",
        level = "INFO",
        module = "SYSTEM"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, BorderWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECENT TELEMETRY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recentLog.message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Module: ${recentLog.module} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(recentLog.timestamp))}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Glowing Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            ) {
                val accentColor = when (recentLog.level) {
                    "SUCCESS" -> EmeraldGreen
                    "WARNING" -> AmberYellow
                    else -> IndigoGlow
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .background(accentColor, RoundedCornerShape(2.dp))
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// --- DEVICES SCREEN ---
@Composable
fun DevicesScreen(viewModel: HomeViewModel, devices: List<DeviceEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home Nodes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Button(
                onClick = { viewModel.showAddDeviceDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoGlow),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_device_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Node",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pair Device",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "No Devices",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No custom smart home devices paired yet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(device = device, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: DeviceEntity, viewModel: HomeViewModel) {
    val haptic = LocalHapticFeedback.current
    val isAiUpdated = viewModel.lastAiUpdatedDeviceId == device.id
    val borderColor = if (isAiUpdated) CyanAccent.copy(alpha = 0.6f) else BorderWhite
    val cardBg = if (isAiUpdated) CyanAccent.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.03f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.id}"),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(if (isAiUpdated) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Device category icon
                val devIcon = when (device.type) {
                    "Light" -> Icons.Default.Lightbulb
                    "Thermostat" -> Icons.Default.Thermostat
                    "Lock" -> if (device.status) Icons.Default.Lock else Icons.Default.LockOpen
                    "Plug" -> Icons.Default.Power
                    else -> Icons.Default.Hub
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (device.status) IndigoGlow.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = devIcon,
                        contentDescription = device.type,
                        tint = if (device.status) IndigoGlow else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAiUpdated) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "✨ AI OUTCOME",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Delete node button
                    IconButton(
                        onClick = { viewModel.deleteDevice(device.id, device.name, device.protocol) },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Node",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = device.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${device.room} • ${device.protocol}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Brightness / Temperature slider if applicable
            if (device.type == "Light" || device.type == "Thermostat") {
                val valueRange = if (device.type == "Thermostat") 16f..30f else 0f..100f
                val unit = if (device.type == "Thermostat") "°C" else "%"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (device.type == "Thermostat") "Target Temp" else "Brightness",
                        fontSize = 11.sp,
                        color = if (device.status) TextSecondary else TextSecondary
                    )
                    Text(
                        text = "${device.value.toInt()}$unit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (device.status) CyanAccent else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = device.value,
                    onValueChange = { viewModel.updateDeviceValue(device, it) },
                    onValueChangeFinished = { viewModel.logDeviceValueChange(device, device.value) },
                    valueRange = valueRange,
                    enabled = device.status,
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoGlow,
                        activeTrackColor = IndigoGlow,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                        disabledThumbColor = TextSecondary.copy(alpha = 0.5f),
                        disabledActiveTrackColor = TextSecondary.copy(alpha = 0.2f),
                        disabledInactiveTrackColor = Color.White.copy(alpha = 0.04f)
                    ),
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("device_slider_${device.id}")
                )
            } else if (device.type == "Lock") {
                Text(
                    text = if (device.status) "SECURELY LOCKED" else "UNLOCKED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (device.status) EmeraldGreen else AmberYellow
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Text(
                    text = if (device.status) "ONLINE" else "OFFLINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (device.status) EmeraldGreen else TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Power control Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Power State",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Switch(
                    checked = device.status,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleDevice(device) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = IndigoGlow,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier
                        .scale(0.85f)
                        .testTag("device_switch_${device.id}")
                )
            }
        }
    }
}

@Composable
fun DashboardDeviceControls(viewModel: HomeViewModel, devices: List<DeviceEntity>) {
    val haptic = LocalHapticFeedback.current
    val controllableDevices = devices.filter { it.type == "Light" || it.type == "Thermostat" }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE DEVICE CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Adjust brightness & temperature directly",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            
            // AI Link status badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(CyanAccent, CircleShape)
                    )
                    Text(
                        text = "AI-SYNCD",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (controllableDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.02f),
                border = BorderStroke(1.dp, BorderWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No light or thermostat nodes registered.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                controllableDevices.forEach { device ->
                    val isAiUpdated = viewModel.lastAiUpdatedDeviceId == device.id
                    val borderColor = if (isAiUpdated) CyanAccent.copy(alpha = 0.4f) else BorderWhite
                    val bgGlow = if (isAiUpdated) CyanAccent.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.03f)
                    
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = bgGlow,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = if (device.type == "Thermostat") Icons.Default.Thermostat else Icons.Default.Lightbulb
                                    val iconColor = if (device.status) CyanAccent else TextSecondary
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (device.status) CyanAccent.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = device.name,
                                            tint = iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = device.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            
                                            if (isAiUpdated) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = "✨ AI OUTCOME",
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = EmeraldGreen,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${device.room} • ${device.protocol}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                
                                Switch(
                                    checked = device.status,
                                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleDevice(device) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = CyanAccent,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f)
                                    ),
                                    modifier = Modifier
                                        .scale(0.8f)
                                        .testTag("dashboard_switch_${device.id}")
                                )
                            }
                            
                            // Interactive Slider for brightness or temperature
                            val valueRange = if (device.type == "Thermostat") 16f..30f else 0f..100f
                            val unit = if (device.type == "Thermostat") "°C" else "%"
                            val label = if (device.type == "Thermostat") "Target Temperature" else "Brightness Level"
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (device.status) TextSecondary else TextSecondary
                                )
                                Text(
                                    text = "${device.value.toInt()}$unit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (device.status) CyanAccent else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Slider(
                                value = device.value,
                                onValueChange = { viewModel.updateDeviceValue(device, it) },
                                onValueChangeFinished = { viewModel.logDeviceValueChange(device, device.value) },
                                valueRange = valueRange,
                                enabled = device.status,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanAccent,
                                    activeTrackColor = CyanAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                                    disabledThumbColor = TextSecondary.copy(alpha = 0.5f),
                                    disabledActiveTrackColor = TextSecondary.copy(alpha = 0.2f),
                                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.04f)
                                ),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("dashboard_slider_${device.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- LOGS SCREEN ---
@Composable
fun LogsScreen(viewModel: HomeViewModel, logs: List<LogEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ecosystem Telemetry Logs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            IconButton(
                onClick = { viewModel.clearAllLogs() },
                modifier = Modifier.testTag("clear_logs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Audit Log",
                    tint = TextSecondary
                )
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent telemetry entries found.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItemRow(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: LogEntity) {
    val levelColor = when (log.level) {
        "SUCCESS" -> EmeraldGreen
        "WARNING" -> AmberYellow
        else -> IndigoGlow
    }

    val icon = when (log.level) {
        "SUCCESS" -> Icons.Default.CheckCircle
        "WARNING" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, BorderWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = log.level,
                tint = levelColor,
                modifier = Modifier
                    .size(16.dp)
                    .offset(y = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Module: ${log.module.uppercase()}",
                        fontSize = 10.sp,
                        color = levelColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- CONFIG & CHAT TERMINAL ---
@Composable
fun ConfigScreen(viewModel: HomeViewModel, chatMessages: List<ChatMessageEntity>) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Lyra Neural Command Core",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Neural Chat Terminal (Top 65% of screen)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.02f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of terminal with clear conversation option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .background(Color.White.copy(alpha = 0.01f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (viewModel.isLyraThinking) CyanAccent else EmeraldGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isLyraThinking) "LYRA PROCESSING..." else "LYRA RECEPTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.showApiSettingsDialog = true },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("configure_api_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure API",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Flush Memory",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderWhite, thickness = 1.dp)

                // High-End AI Mode Selector Tab Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modes = listOf(
                            "STANDARD" to "Balanced",
                            "LOW_LATENCY" to "Lite",
                            "DEEP_THINKING" to "High Think",
                            "GROUNDED" to "Search",
                            "LIVE_API" to "Live",
                            "OFFLINE_LOCAL" to "Local"
                        )
                        
                        modes.forEach { (modeKey, modeLabel) ->
                            val isSelected = viewModel.aiMode == modeKey
                            val activeColor = when (modeKey) {
                                "LOW_LATENCY" -> IndigoGlow
                                "DEEP_THINKING" -> PurpleGlow
                                "GROUNDED" -> CyanAccent
                                "LIVE_API" -> Pink80
                                "OFFLINE_LOCAL" -> Color.Gray
                                else -> EmeraldGreen
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) activeColor.copy(alpha = 0.25f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setAiModeAndPersist(modeKey) }
                                    .testTag("ai_mode_${modeKey.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val icon = when (modeKey) {
                                        "LOW_LATENCY" -> Icons.Default.Bolt
                                        "DEEP_THINKING" -> Icons.Default.Psychology
                                        "GROUNDED" -> Icons.Default.Language
                                        "LIVE_API" -> Icons.Default.Stream
                                        "OFFLINE_LOCAL" -> Icons.Default.WifiOff
                                        else -> Icons.Default.Memory
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeColor else TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = modeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) activeColor else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // A brief futuristic status description line for the active mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (modeTitle, modeDesc) = when (viewModel.aiMode) {
                            "LOW_LATENCY" -> "Flash Lite Core" to "Optimized for sub-second, ultra-snappy home controls"
                            "DEEP_THINKING" -> "High Thinking Core" to "Deep reasoning enabled. Solves complex automation & rules"
                            "GROUNDED" -> "Search Grounded Core" to "Accesses live web search data for up-to-date information"
                            "LIVE_API" -> "Live Conversation API" to "Real-time streaming integration with gemini-3.1-flash-live-preview"
                            "OFFLINE_LOCAL" -> "Offline Local Execution" to "Runs entirely on-device for basic fallback reasoning"
                            else -> "Standard Gemini Core" to "Balanced intelligence & versatility for general queries"
                        }
                        
                        val activeColor = when (viewModel.aiMode) {
                            "LOW_LATENCY" -> IndigoGlow
                            "DEEP_THINKING" -> PurpleGlow
                            "GROUNDED" -> CyanAccent
                            "LIVE_API" -> Pink80
                            "OFFLINE_LOCAL" -> Color.Gray
                            else -> EmeraldGreen
                        }

                        Text(
                            text = modeTitle.uppercase(),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = modeDesc,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = BorderWhite, thickness = 1.dp)

                // Message bubbles List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages, key = { it.id }) { message ->
                        Box(modifier = Modifier.animateItem()) {
                            ChatBubble(message = message)
                        }
                    }

                    if (viewModel.isLyraThinking) {
                        item {
                            Box(modifier = Modifier.animateItem()) {
                                TypingIndicator()
                            }
                        }
                    }
                }

                // Chat Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = viewModel.typedMessage,
                        onValueChange = { viewModel.typedMessage = it },
                        placeholder = { Text("Command Lyra AI...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("chat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            disabledContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { viewModel.showVoiceDialog = true },
                        enabled = !viewModel.isLyraThinking,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (!viewModel.isLyraThinking) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f),
                                RoundedCornerShape(24.dp)
                            )
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (!viewModel.isLyraThinking) CyanAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.sendUserMessage() },
                        enabled = viewModel.typedMessage.isNotBlank() && !viewModel.isLyraThinking,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (viewModel.typedMessage.isNotBlank() && !viewModel.isLyraThinking) IndigoGlow else Color.White.copy(alpha = 0.04f),
                                RoundedCornerShape(24.dp)
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (viewModel.typedMessage.isNotBlank() && !viewModel.isLyraThinking) Color.Black else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Operating System Core Controls (Bottom 35%)
        Text(
            text = "System Diagnostics",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            item {
                DiagControlCard(
                    title = "Bridge Keys",
                    actionText = "Re-Encrypt",
                    icon = Icons.Default.VpnKey,
                    onClick = { viewModel.reEncryptMatterBridge() }
                )
            }
            item {
                DiagControlCard(
                    title = "System Sec",
                    actionText = "Apply Patch 08",
                    icon = Icons.Default.Shield,
                    onClick = { viewModel.applySecurityPatch() }
                )
            }
            item {
                DiagControlCard(
                    title = "Lyra Core API",
                    actionText = "Configure",
                    icon = Icons.Default.Settings,
                    onClick = { viewModel.showApiSettingsDialog = true }
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleTtsEnabled() },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, BorderWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CyanAccent.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isTtsEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                                    contentDescription = "Voice Confirmation",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AI Voice Confirm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (viewModel.isTtsEnabled) "Audible [Lyra]" else "Muted",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (viewModel.isTtsEnabled) EmeraldGreen else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.isTtsEnabled,
                            onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleTtsEnabled() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyanAccent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.06f)
                            ),
                            modifier = Modifier
                                .scale(0.7f)
                                .testTag("tts_toggle")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.sender == "USER"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) IndigoGlow.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f)
    val textColor = if (isUser) Color.White else TextPrimary
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = align
    ) {
        Surface(
            shape = shape,
            color = bg,
            border = if (!isUser) BorderStroke(0.5.dp, BorderWhite) else null,
            shadowElevation = if (isUser) 4.dp else 0.dp
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typingIndicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, BorderWhite)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer(alpha = alpha)
                        .background(CyanAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer(alpha = alpha * 0.7f)
                        .background(CyanAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer(alpha = alpha * 0.4f)
                        .background(CyanAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lyra is executing command...",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DiagControlCard(
    title: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, BorderWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(IndigoGlow.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IndigoGlow,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = actionText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )
            }
        }
    }
}

// --- ADD DEVICE DIALOG ---
@Composable
fun AddDeviceDialog(viewModel: HomeViewModel) {
    Dialog(onDismissRequest = { viewModel.showAddDeviceDialog = false }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkGreyBg,
            border = BorderStroke(1.dp, BorderWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pair Matter Smart Node",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = viewModel.newDeviceName,
                    onValueChange = { viewModel.newDeviceName = it },
                    label = { Text("Device / Node Name", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoGlow,
                        unfocusedBorderColor = BorderWhite,
                        focusedLabelColor = IndigoGlow,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_device_name_field")
                )

                // Room Input
                OutlinedTextField(
                    value = viewModel.newDeviceRoom,
                    onValueChange = { viewModel.newDeviceRoom = it },
                    label = { Text("Room Location", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoGlow,
                        unfocusedBorderColor = BorderWhite,
                        focusedLabelColor = IndigoGlow,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector for type
                Column {
                    Text(text = "Node Type", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Light", "Thermostat", "Lock", "Plug").forEach { type ->
                            val selected = viewModel.newDeviceType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) IndigoGlow else Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(1.dp, if (selected) IndigoGlow else BorderWhite, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.newDeviceType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Selector for Protocol
                Column {
                    Text(text = "Communication Protocol", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Matter", "MQTT", "BLE", "ESP32").forEach { protocol ->
                            val selected = viewModel.newDeviceProtocol == protocol
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) CyanAccent else Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(1.dp, if (selected) CyanAccent else BorderWhite, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.newDeviceProtocol = protocol }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = protocol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.showAddDeviceDialog = false },
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.addNewDevice() },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoGlow),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("dialog_submit_button")
                    ) {
                        Text("Establish Link", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- QUICK COMMANDS DIALOG ---
@Composable
fun QuickActionDialog(viewModel: HomeViewModel) {
    val quickTasks = listOf(
        "Run Full Security Audit" to "Ask Lyra to check and audit the security status of all smart devices.",
        "Turn On All Lights" to "Instruct Lyra to broadcast a command powering up all lights.",
        "Secure Front Door lock" to "Signal MQTT to lock the main entrance deadlock immediately.",
        "Reset Matter Hub session" to "Initiate full bridge session reset and re-encryption cycle."
    )

    Dialog(onDismissRequest = { viewModel.showQuickActionDialog = false }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkGreyBg,
            border = BorderStroke(1.dp, BorderWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lyra Neural Quick Commands",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickTasks.forEach { (task, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.typedMessage = task
                                    viewModel.currentTab = "config"
                                    viewModel.showQuickActionDialog = false
                                    viewModel.sendUserMessage()
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.03f),
                            border = BorderStroke(1.dp, BorderWhite)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = task,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoGlow
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.showQuickActionDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun LyraApiSettingsDialog(viewModel: HomeViewModel) {
    var keyInput by remember { mutableStateOf(viewModel.customApiKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.showApiSettingsDialog = false }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = GlassSlate,
            border = BorderStroke(1.dp, BorderWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("lyra_api_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🤖",
                            fontSize = 24.sp
                        )
                        Column {
                            Text(
                                text = "Lyra Neural API",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "COGNITIVE MODULE PORT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = PurpleGlow,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (viewModel.testResultSuccess == true) EmeraldGreen 
                                else if (viewModel.testResultSuccess == false) Color.Red 
                                else AmberYellow, 
                                CircleShape
                            )
                    )
                }

                HorizontalDivider(color = BorderWhite, thickness = 1.dp)

                Text(
                    text = "Configure your custom Gemini API key here. By default, Lyra attempts to use the secure system-wide API credentials injected from your AI Studio Secrets panel.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                // Input Field for API Key
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "GEMINI API KEY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    TextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        placeholder = { Text("Enter custom GEMINI_API_KEY...", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            val description = if (passwordVisible) "Hide Key" else "Show Key"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description, tint = TextSecondary)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedIndicatorColor = IndigoGlow,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                // Interactive Status Indicator & Test Handshake Panel
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, BorderWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API Connection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (viewModel.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyanAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = when {
                                        viewModel.testResultSuccess == true -> "ONLINE"
                                        viewModel.testResultSuccess == false -> "CONNECTION FAILED"
                                        viewModel.customApiKey.isNotEmpty() -> "UNVERIFIED CUSTOM"
                                        com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> "READY (SYSTEM CORE)"
                                        else -> "KEY REQUIRED"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        viewModel.testResultSuccess == true -> EmeraldGreen
                                        viewModel.testResultSuccess == false -> Color.Red
                                        viewModel.customApiKey.isNotEmpty() -> AmberYellow
                                        com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> EmeraldGreen
                                        else -> AmberYellow
                                    }
                                )
                            }
                        }

                        if (viewModel.apiStatusMessage.isNotEmpty()) {
                            Text(
                                text = viewModel.apiStatusMessage,
                                fontSize = 11.sp,
                                color = if (viewModel.testResultSuccess == false) Color.Red.copy(alpha = 0.9f) else TextSecondary,
                                lineHeight = 16.sp
                            )
                        } else {
                            Text(
                                text = "Provide a key or use the default configuration, then test connection to initiate secure TLS handshake.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.testLyraConnection() },
                            enabled = !viewModel.isTestingConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("test_api_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (viewModel.isTestingConnection) "Pinging Neural Core..." else "Test Connection",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Actions (Save, Clear, Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.customApiKey.isNotEmpty() || keyInput.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                keyInput = ""
                                viewModel.saveCustomApiKey("")
                            },
                            modifier = Modifier.testTag("clear_api_key_button")
                        ) {
                            Text("Reset Default", color = Color.Red.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = { viewModel.showApiSettingsDialog = false }
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.saveCustomApiKey(keyInput)
                            viewModel.showApiSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoGlow),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("Save & Exit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LyraVoiceControlDialog(viewModel: HomeViewModel) {
    val context = LocalContext.current
    var isListeningState by remember { mutableStateOf(true) }
    var transcriptionResult by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningState = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0) ?: ""
            if (spokenText.isNotBlank()) {
                transcriptionResult = spokenText
            } else {
                Toast.makeText(context, "No speech detected.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Speech capture cancelled or unavailable.", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerSpeechInput = {
        isListeningState = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Command Lyra Home Assistant...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListeningState = false
            Toast.makeText(context, "Voice input failed: using simulated command templates.", Toast.LENGTH_LONG).show()
        }
    }

    // Launch Speech Recognizer on open
    LaunchedEffect(Unit) {
        triggerSpeechInput()
    }

    val simulatedCommands = listOf(
        "Turn off bedroom lights",
        "Activate Night Mode",
        "Set Office Thermostat to 22 degrees",
        "Lock front door",
        "Active Movie Mode",
        "Show energy usage diagnostics"
    )

    Dialog(onDismissRequest = { viewModel.showVoiceDialog = false }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = GlassSlate,
            border = BorderStroke(1.dp, BorderWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("voice_control_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lyra Voice Link",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "NATURAL SPEECH COPROCESSOR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isListeningState) CyanAccent else TextSecondary, CircleShape)
                    )
                }

                HorizontalDivider(color = BorderWhite, thickness = 1.dp)

                // Pulsing animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListeningState) {
                        VoicePulsingRipple()
                    } else {
                        // Static Mic with custom border
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                                .border(1.dp, BorderWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙", fontSize = 28.sp)
                        }
                    }
                }

                // Status text / transcription
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isListeningState) "LISTENING FOR VOICE..." else "SPEECH COPROCESSOR DE-ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isListeningState) CyanAccent else TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    if (transcriptionResult.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, BorderWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Transcribed:",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"$transcriptionResult\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isListeningState) "Speak your smart home request..." else "No speech captured yet. Tap below or try speaking again.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // Preset Suggestions List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SUGGESTED VOICE COMMANDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Display as horizontal scroll of quick voice templates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        simulatedCommands.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, BorderWhite),
                                modifier = Modifier
                                    .clickable {
                                        transcriptionResult = suggestion
                                        isListeningState = false
                                    }
                                    .testTag("suggested_voice_command_${suggestion.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            suggestion.contains("light", ignoreCase = true) -> "💡"
                                            suggestion.contains("door", ignoreCase = true) -> "🔒"
                                            suggestion.contains("thermostat", ignoreCase = true) -> "🌡"
                                            suggestion.contains("scene", ignoreCase = true) || suggestion.contains("mode", ignoreCase = true) -> "🎬"
                                            else -> "⚡"
                                        },
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = suggestion,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Actions: Cancel / Send / Re-record
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.showVoiceDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = TextSecondary, fontSize = 13.sp)
                    }

                    if (!isListeningState && transcriptionResult.isEmpty()) {
                        Button(
                            onClick = { triggerSpeechInput() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, BorderWhite),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Speak Again", color = TextPrimary, fontSize = 13.sp)
                        }
                    } else if (transcriptionResult.isNotEmpty()) {
                        Button(
                            onClick = { triggerSpeechInput() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, BorderWhite),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text("Re-Speak", color = TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.sendVoiceCommand(transcriptionResult)
                                viewModel.showVoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("execute_voice_command_button")
                        ) {
                            Text("Execute", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoicePulsingRipple() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale1 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val alpha1 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val scale2 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale1)
                .background(CyanAccent.copy(alpha = alpha1), CircleShape)
        )
        // Outer pulsing ring 2
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale2)
                .background(IndigoGlow.copy(alpha = alpha2), CircleShape)
        )
        // Center Core
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyanAccent, IndigoGlow)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🎙", fontSize = 28.sp)
        }
    }
}
