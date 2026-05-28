package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeathEcho
import com.example.data.Progression
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- CUSTOM AESTHETICS COLOR TOKENS ---
val CarbonDark = Color(0xFF050505)
val SteelGrayCard = Color(0xE00D0E15)
val NeonCrimson = Color(0xFFE11D48)
val NeonGreen = Color(0xFF10B981)
val ElectricCyan = Color(0xFF06B6D4)
val WarningYellow = Color(0xFFF59E0B)
val GhostPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Floor99MainContainer(viewModel: GameViewModel) {
    val progression by viewModel.progression.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeDeathEchoes by viewModel.activeDeathEchoes.collectAsState()
    val floor by viewModel.currentFloor.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(CarbonDark)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentScreen) {
                                            GameScreen.ACTIVE_RUN -> NeonCrimson.copy(alpha = pulseAlpha)
                                            GameScreen.HUB_BASE_CAMP -> NeonGreen.copy(alpha = pulseAlpha)
                                            GameScreen.GDD_HUBS -> ElectricCyan.copy(alpha = pulseAlpha)
                                            else -> GhostPurple.copy(alpha = pulseAlpha)
                                        }
                                    )
                            )
                            Text(
                                text = when (currentScreen) {
                                    GameScreen.ACTIVE_RUN -> "LIFE LINK ACTIVE"
                                    GameScreen.HUB_BASE_CAMP -> "BASE CAMP SECURE"
                                    GameScreen.GDD_HUBS -> "DESIGN CONSOLE ACTIVE"
                                    else -> "VICTORY DECK UNLOCKED"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when (currentScreen) {
                                    GameScreen.ACTIVE_RUN -> NeonCrimson
                                    GameScreen.HUB_BASE_CAMP -> NeonGreen
                                    GameScreen.GDD_HUBS -> ElectricCyan
                                    else -> GhostPurple
                                }
                            )
                        }

                        val bar1Progress = when (currentScreen) {
                            GameScreen.ACTIVE_RUN -> {
                                val pValue by viewModel.player.collectAsState()
                                if (pValue.maxHp > 0) (pValue.hp / pValue.maxHp).coerceIn(0f, 1f) else 1f
                            }
                            else -> 1f
                        }
                        val bar2Progress = when (currentScreen) {
                            GameScreen.ACTIVE_RUN -> (floor.toFloat() / 99f).coerceIn(0f, 1f)
                            GameScreen.HUB_BASE_CAMP -> (progression.totalScrap.toFloat() / 5000f).coerceAtMost(1f)
                            GameScreen.GDD_HUBS -> 0.45f
                            else -> 1f
                        }

                        // Bar 1 (Rose)
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bar1Progress)
                                    .background(NeonCrimson)
                            )
                        }

                        // Bar 2 (Cyan)
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bar2Progress)
                                    .background(ElectricCyan)
                            )
                        }
                    }

                    // RIGHT COLUMN
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (currentScreen) {
                                GameScreen.ACTIVE_RUN -> "SYSTEM_RUN_${progression.runsCompleted + 1}"
                                GameScreen.HUB_BASE_CAMP -> "CAMP_RECON_SECURE"
                                GameScreen.GDD_HUBS -> "CONSOLE_RECON_V1.1"
                                else -> "RECON_SUCCESS"
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = when (currentScreen) {
                                    GameScreen.ACTIVE_RUN -> "FLR "
                                    GameScreen.HUB_BASE_CAMP -> "CAMP "
                                    GameScreen.GDD_HUBS -> "GDD "
                                    else -> "VIC "
                                },
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = when (currentScreen) {
                                    GameScreen.ACTIVE_RUN -> "$floor"
                                    GameScreen.HUB_BASE_CAMP -> "99"
                                    GameScreen.GDD_HUBS -> "CORE"
                                    else -> "99"
                                },
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                color = NeonCrimson
                            )
                        }

                        if (currentScreen != GameScreen.ACTIVE_RUN) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${progression.totalScrap} SCRAP ⭐",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // High-End Custom Navigation Bar
            NavigationBar(
                containerColor = CarbonDark.copy(alpha = 0.85f),
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            ) {
                NavigationBarItem(
                    selected = currentScreen == GameScreen.GDD_HUBS,
                    onClick = { viewModel.setScreen(GameScreen.GDD_HUBS) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == GameScreen.GDD_HUBS) Icons.Default.Info else Icons.Outlined.Info,
                            contentDescription = "GDD Hub"
                        )
                    },
                    label = { Text("Design Console", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == GameScreen.HUB_BASE_CAMP,
                    onClick = { viewModel.setScreen(GameScreen.HUB_BASE_CAMP) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == GameScreen.HUB_BASE_CAMP) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Base Camp"
                        )
                    },
                    label = { Text("Base Camp", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == GameScreen.ACTIVE_RUN || currentScreen == GameScreen.VICTORY_DECK,
                    onClick = {
                        if (currentScreen != GameScreen.ACTIVE_RUN) {
                            viewModel.startNewRun(1)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Live Sandbox"
                        )
                    },
                    label = { Text("Run Core", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCrimson,
                        selectedTextColor = NeonCrimson,
                        indicatorColor = NeonCrimson.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(color = CarbonDark)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCrimson.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.15f),
                            radius = size.width * 0.85f
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = size.width * 0.85f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricCyan.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.85f),
                            radius = size.width * 0.75f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.75f
                    )
                }
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    GameScreen.GDD_HUBS -> GameDesignDashboard(viewModel)
                    GameScreen.HUB_BASE_CAMP -> BaseCampUpgrades(viewModel, progression)
                    GameScreen.ACTIVE_RUN -> ActivePlayableArena(viewModel)
                    GameScreen.VICTORY_DECK -> VictoryDeckScreen(viewModel)
                }
            }
        }
    }
}

// ==========================================
// I. GAME DESIGN CONSOLE (GDD) VIEW
// ==========================================
@Composable
fun GameDesignDashboard(viewModel: GameViewModel) {
    val selectedTab by viewModel.selectedGddTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Horizontal Custom Segmented Control Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SteelGrayCard)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("COMBAT", "SCALING", "SYNERGY", "STEPS")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                val tint = when (tab) {
                    "COMBAT" -> ElectricCyan
                    "SCALING" -> WarningYellow
                    "SYNERGY" -> NeonGreen
                    "STEPS" -> NeonCrimson
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) tint.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) tint else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { viewModel.setGddTab(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) tint else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                "COMBAT" -> CombatAndMatrixTab()
                "SCALING" -> BiomeScalingTab(viewModel)
                "SYNERGY" -> SynergyForgeTab(viewModel)
                "STEPS" -> RoadmapStepsTab()
            }
        }
    }
}

@Composable
fun CombatAndMatrixTab() {
    var testActiveAction by remember { mutableStateOf<String?>("NONE") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SECOND-TO-SECOND ACTION ENGINE",
                        color = ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Floor 99 utilizes high-friction kinetic systems optimized first for single-hand mobile touch controls. Character steering relies on a responsive dynamic floating joystick, with active tap gestures driving defensive dodge frames and sweeping slash arcs.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "ACTION CONTROL MAPPING MATRIX",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SteelGrayCard)) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp)
                    ) {
                        Text("ACTION", modifier = Modifier.weight(1f), color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("MOBILE TOUCH", modifier = Modifier.weight(1.5f), color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("PC EXPANSION MAPPING", modifier = Modifier.weight(2f), color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                    // Row 1: Steering
                    MappingRow(
                        action = "Steer / Move",
                        mobile = "Dynamic Virtual Joystick (Left or Right Thumb)",
                        pc = "WASD keys or Left Controller Thumbstick",
                        onHighlight = { testActiveAction = "STEER" }
                    )

                    // Row 2: Slash
                    MappingRow(
                        action = "Sword Slash",
                        mobile = "Primary Action Circle (Right Tap with weapon brush)",
                        pc = "Left Mouse Button or Controller [X] button",
                        onHighlight = { testActiveAction = "SLASH" }
                    )

                    // Row 3: Dodge Dash
                    MappingRow(
                        action = "Dodge Dash",
                        mobile = "Dodge Charger Quick-Dodge Swipe/Tap button",
                        pc = "Spacebar or Controller [B] button",
                        onHighlight = { testActiveAction = "DODGE" }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelGrayCard.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "INTELLIGENT RE-MAPPING MATRIX SIMULATOR",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Highlighting a controller target triggers automatically mapped PC directives. Tap any row above to bind and calibrate layouts.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOUCH ELEMENT", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (testActiveAction != "NONE") ElectricCyan.copy(alpha = 0.2f) else SteelGrayCard)
                                    .border(1.dp, ElectricCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (testActiveAction == "STEER") Icons.Default.Menu else Icons.Default.Star,
                                    contentDescription = "Simulated Touch input",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Map direction",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PC HARDBOUND", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (testActiveAction) {
                                        "STEER" -> "WASD / KEYS"
                                        "SLASH" -> "MOUSE_LEFT / [X]"
                                        "DODGE" -> "SPACEBAR / [B]"
                                        else -> "STANDBY"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MappingRow(action: String, mobile: String, pc: String, onHighlight: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHighlight() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(action, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Column(modifier = Modifier.weight(1.5f)) {
            Text(mobile, color = Color.LightGray, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(2f)) {
            Text(pc, color = ElectricCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
}

@Composable
fun BiomeScalingTab(viewModel: GameViewModel) {
    val selectedIndex by viewModel.selectedBiomeIndex.collectAsState()

    val biomes = listOf(
        BiomeMeta("Floors 1-10", "The Rust Slums", "NORMAL", "The Scrap Golem", "Rusty metallic cells with weak scavenger bots. Serves as basic introduction onboarding."),
        BiomeMeta("Floors 11-20", "The Cyan Reactor", "ENEMIES EXPLODE", "Oxide Core", "Reactor fuel rods. Slaying enemies triggers thermal overload explosions."),
        BiomeMeta("Floors 21-30", "Crystal Hollows", "NO RECOVERY", "Prism Warden", "Static fields restrict the player's core automatic life siphons."),
        BiomeMeta("Floors 31-50", "Burning Foundry", "BURNING PATHS", "Magma Titan", "Enemies leak hot combustion fuel. High reflex tracking dodging required."),
        BiomeMeta("Floors 51-99", "The Apex Sanctum", "TOTAL APOCALYPSE", "Floor 99 Monarch", "Aggressive gravity matrices, multiple overlapping hazards.")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BIOME DIFFICULTY & MULTIPLIER REGISTRY",
                    color = WarningYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To keep the journey up the 99 layers from feeling stagnant, rooms are grouped into visual biomes, injecting unique floor-wide environment rules.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Biome Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            biomes.forEachIndexed { idx, biome ->
                val active = selectedIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) WarningYellow.copy(alpha = 0.2f) else SteelGrayCard)
                        .border(1.dp, if (active) WarningYellow else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { viewModel.setBiomeIndex(idx) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        biome.floors,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) WarningYellow else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Biome Details Card
        val selectedBiome = biomes[selectedIndex]
        Card(
            colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
            border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedBiome.name.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningYellow.copy(alpha = 0.2f))
                                .border(1.dp, WarningYellow, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedBiome.rule,
                                color = WarningYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedBiome.desc, color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "CHALLENGE REGISTRY SPECIFICS",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    BiomeSpecCard(
                        label = "Biome Boss (10th Floor Cap)",
                        value = selectedBiome.boss,
                        imageVector = Icons.Default.Warning,
                        color = NeonCrimson
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    BiomeSpecCard(
                        label = "Loot Grade Distribution",
                        value = when (selectedIndex) {
                            0 -> "Common Alloys, Basic blueprints"
                            1 -> "Uncommon battery terminals, basic circuit"
                            2 -> "Rare Prism Crystals, Tesla Cores"
                            3 -> "Epic Magma fuel, Obsidian handles"
                            else -> "Legendary Apex cores, God-tier synergy combinations"
                        },
                        imageVector = Icons.Default.Star,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}

data class BiomeMeta(val floors: String, val name: String, val rule: String, val boss: String, val desc: String)

@Composable
fun BiomeSpecCard(label: String, value: String, imageVector: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonDark)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = imageVector, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SynergyForgeTab(viewModel: GameViewModel) {
    val slot1 by viewModel.synergyItem1.collectAsState()
    val slot2 by viewModel.synergyItem2.collectAsState()

    // Synergies available to inspect
    val predefinedSynergies = listOf(
        SynergyFormula("Vampiric Inferno", RunItemType.LIFESTEAL_FANGS, RunItemType.SOLAR_VEIL, "Slaying enemies drops blazing chemical fire tracks. Sparks restore health and trigger siphons."),
        SynergyFormula("Electrolyte Dash", RunItemType.TESLA_RING, RunItemType.DODGE_CHARGER, "Dodging discharges dynamic high-voltage ring lightning bolts in full 360 sweeping patterns."),
        SynergyFormula("Nuclear Retaliation", RunItemType.NITROLYTE_CORE, RunItemType.COLOSSUS_BLADE, "Giant sword slash triggers micro explosions on hit that cascade chain-reacting across the screen.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "THE LATE-GAME PROGRESSION PARADOX",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Because Floor 99 features absolute permadeath, the early levels could easily feel repetitive. The Synergy Forge guarantees that dynamic loot combinations completely redefine scaling, making lucky drops feel like game-breaking wins.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        item {
            Text(
                "INTERACTIVE RUN-BUILDER LAB",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("EQUIPMENT MATRIX REVOLVER", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot A
                        SynergySlotBox(
                            label = "SLOT A",
                            item = slot1,
                            onClear = { viewModel.setSynergySlots(null, slot2) },
                            onAssign = { viewModel.setSynergySlots(it, slot2) }
                        )

                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = FontFamily.Monospace)

                        // Slot B
                        SynergySlotBox(
                            label = "SLOT B",
                            item = slot2,
                            onClear = { viewModel.setSynergySlots(slot1, null) },
                            onAssign = { viewModel.setSynergySlots(slot1, it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Synergy Result Output
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonDark)
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        val matching = predefinedSynergies.firstOrNull {
                            (it.itemA == slot1 && it.itemB == slot2) || (it.itemA == slot2 && it.itemB == slot1)
                        }

                        if (matching != null) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = matching.name.uppercase(),
                                        color = NeonGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "⚡ SYNERGY UNLOCKED",
                                        color = ElectricCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = matching.desc,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "SELECT RECON COMBOS OR TWEAK THE SLOTS ABOVE TO MINE ACTIVE PATTERNS.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "UNLOCKED REFERENCE SCHEMATICS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        items(predefinedSynergies) { schematic ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelGrayCard.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSynergySlots(schematic.itemA, schematic.itemB) }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(schematic.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(schematic.desc, color = Color.Gray, fontSize = 10.sp)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Simulate", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class SynergyFormula(val name: String, val itemA: RunItemType, val itemB: RunItemType, val desc: String)

@Composable
fun SynergySlotBox(
    label: String,
    item: RunItemType?,
    onClear: () -> Unit,
    onAssign: (RunItemType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        Pair("Lifesteal Fangs", RunItemType.LIFESTEAL_FANGS),
        Pair("Solar Veil", RunItemType.SOLAR_VEIL),
        Pair("Tesla Ring", RunItemType.TESLA_RING),
        Pair("Dodge Charger", RunItemType.DODGE_CHARGER),
        Pair("Nitrolyte Core", RunItemType.NITROLYTE_CORE),
        Pair("Colossus Blade", RunItemType.COLOSSUS_BLADE)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CarbonDark)
                .border(
                    width = 1.dp,
                    color = if (item != null) NeonGreen else Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            if (item != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = item.name.replace("_", " "),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CLEAR",
                        color = NeonCrimson,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            onClear()
                        }
                    )
                }
            } else {
                Text(
                    text = "CHOOSE ITEM",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SteelGrayCard)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.first, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        onClick = {
                            onAssign(option.second)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RoadmapStepsTab() {
    val items = remember {
        mutableStateListOf(
            RoadmapItem("1. HIGH-KINETIC CONTROLS CALIBRATION", "Build character position matrices, drag joystick vectors, and bound check coordinate walls inside high-rate physics frames.", true),
            RoadmapItem("2. DEATH ECHO CORPSE PERSISTENCE", "Setup SQL tables capturing floor indexes and coordinates upon code level siphons. Redraw Headstones dynamically on level rebuild.", true),
            RoadmapItem("3. DYNAMIC MODIFIER TRIGGERS", "Implement room environment factories that inject active variables (e.g., 'ENEMIES ENERGIZE', 'BURN LANES') causing canvas hazard loops to flare.", true),
            RoadmapItem("4. GRAPHICS INTEGRATION & SEEDINGS", "Map gorgeous skeletal visual sheets, and vector background layers for biomes 1-5.", false),
            RoadmapItem("5. STEAM EXPORTS & CONSOLE EXTENSIONS", "Calibrate mouse coordinate lockouts and standard keyboard input bridges for PC expansion targets.", false)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SteelGrayCard)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonCrimson)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROADMAP TECHNICAL INTEGRATIONS",
                            color = NeonCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Checklist trackers outlining code phases. Fully interactive workspace allows marking integrated segments below.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        items(items) { roadmapItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SteelGrayCard)
                    .clickable {
                        val index = items.indexOf(roadmapItem)
                        if (index != -1) {
                            items[index] = roadmapItem.copy(checked = !roadmapItem.checked)
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = roadmapItem.checked,
                    onCheckedChange = { _ ->
                        val index = items.indexOf(roadmapItem)
                        if (index != -1) {
                            items[index] = roadmapItem.copy(checked = !roadmapItem.checked)
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCrimson,
                        checkmarkColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = roadmapItem.title,
                        color = if (roadmapItem.checked) Color.Gray else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        style = if (roadmapItem.checked) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = roadmapItem.desc,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

data class RoadmapItem(val title: String, val desc: String, val checked: Boolean)

// ==========================================
// II. HUB BASE CAMP ("FLOOR 0")
// ==========================================
@Composable
fun BaseCampUpgrades(viewModel: GameViewModel, progression: Progression) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BASE CAMP: LEVEL 0",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A safe-haven base before starting standard ascent. Exchange scaling Scraps permanently to secure permanent upgrades.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PERMANENT HARDWARE UPGRADES",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val upgrades = listOf(
            UpgradeItem("Health Regulator", "health", progression.baseHealthLevel, "Increases baseline health reserves by +10 pts per level.", (progression.baseHealthLevel + 1) * 20),
            UpgradeItem("Alloy Slasher Edge", "damage", progression.baseDamageLevel, "Increases blade swing base strike outputs by +2 pts per level.", (progression.baseDamageLevel + 1) * 25),
            UpgradeItem("Kinetic Servos", "speed", progression.baseSpeedLevel, "Accelerates coordinate travel speed by +5% per level.", (progression.baseSpeedLevel + 1) * 30),
            UpgradeItem("Phase Dodge Shifter", "dodge", progression.baseDodgeLevel, "Unlocks rapid frame dodges, reducing cool downs by +5% per level.", (progression.baseDodgeLevel + 1) * 35)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(upgrades) { upgrade ->
                val canAfford = progression.totalScrap >= upgrade.cost

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SteelGrayCard)
                        .border(1.dp, if (canAfford) NeonGreen.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                upgrade.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LVL ${upgrade.currentLvl}",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(upgrade.desc, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.purchaseUpgrade(upgrade.type) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.1f),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("upgrade_${upgrade.type}")
                    ) {
                        Text("${upgrade.cost} ⭐", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // START ASCENT BUTTON
        Button(
            onClick = { viewModel.startNewRun(1) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_ascent_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "DEEP ASCENT: COMMENCE RUN ON FLOOR 1",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

data class UpgradeItem(val name: String, val type: String, val currentLvl: Int, val desc: String, val cost: Int)

// ==========================================
// III. PLAYABLE ACTIONS CONTROLS & CANVAS
// ==========================================
@Composable
fun ActivePlayableArena(viewModel: GameViewModel) {
    val floor by viewModel.currentFloor.collectAsState()
    val floorState by viewModel.floorStage.collectAsState()
    val modifierStr by viewModel.activeModifierString.collectAsState()

    val pValue by viewModel.player.collectAsState()
    val mEnemies by viewModel.enemies.collectAsState()
    val mProjectiles by viewModel.projectiles.collectAsState()
    val mDrops by viewModel.scrapsInRun.collectAsState()
    val mTrails by viewModel.flameTrails.collectAsState()
    val mVfxList by viewModel.vfxList.collectAsState()

    val runScraps by viewModel.currentRunScraps.collectAsState()
    val activeItems by viewModel.activeRunItems.collectAsState()
    val lootOptions by viewModel.lootOptions.collectAsState()
    val echoCorpse by viewModel.floorDeathEcho.collectAsState()
    val bossMsg by viewModel.bossDefeatedMessage.collectAsState()

    var speedMultiplier by remember { mutableStateOf(1f) }

    // Floating Joystick variables
    var joystickCenter by remember { mutableStateOf(Offset(100f, 100f)) }
    var joystickOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var isDraggingJoystick by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // TOP CONSOLE TELEMETRY HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SteelGrayCard)
                .border(
                    1.dp,
                    if (modifierStr != "NORMAL") WarningYellow.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FLOOR LEVEL: $floor / 99",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (modifierStr == "NORMAL") NeonGreen else WarningYellow)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ROOM MODIFIER: $modifierStr",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (modifierStr == "NORMAL") Color.Gray else WarningYellow
                    )
                }
            }

            // High priority live stats ticker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RECOVERED SCRAPS",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "+$runScraps ⭐",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live boss notifications helper
        AnimatedVisibility(
            visible = bossMsg != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            if (bossMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GhostPurple.copy(alpha = 0.2f))
                        .border(1.dp, GhostPurple, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bossMsg ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Death Echo Tombstone indicator on active Floor
        if (echoCorpse != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(GhostPurple.copy(alpha = 0.15f))
                    .border(1.dp, GhostPurple, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = GhostPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DEATH ECHO TARGET on this floor! Reclaim previous +${echoCorpse?.scrapStored} scraps by defeating the Spectre!",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // THE PRIMARY ACTION CANVAS ARENA (800 x 800 Virtual grid space scaled to box constraints)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CarbonDark)
                .border(
                    width = 2.dp,
                    color = if (floorState == FloorStage.ESCAPE) NeonGreen else ElectricCyan.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            val componentWidth = maxWidth
            val componentHeight = maxHeight

            // Canvas drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 800f
                val scaleY = size.height / 800f

                // A. Draw cyber grid lines
                val gridLineCount = 10
                for (i in 0..gridLineCount) {
                    val x = (800f / gridLineCount) * i
                    val y = (800f / gridLineCount) * i

                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(x * scaleX, 0f),
                        end = Offset(x * scaleX, size.height),
                        strokeWidth = 1f * scaleX
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(0f, y * scaleY),
                        end = Offset(size.width, y * scaleY),
                        strokeWidth = 1f * scaleY
                    )
                }

                // B. Escape Portal Stairs if stage is cleared
                if (floorState == FloorStage.ESCAPE) {
                    val portalX = 400f * scaleX
                    val portalY = 200f * scaleY

                    // Outward animation circle pulse
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.15f + 0.1f * sin(System.currentTimeMillis() / 150.0).toFloat()),
                        center = Offset(portalX, portalY),
                        radius = 45f * scaleX
                    )

                    drawCircle(
                        color = NeonGreen,
                        center = Offset(portalX, portalY),
                        radius = 25f * scaleX,
                        style = Stroke(
                            width = 4f * scaleX,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 10f)
                        )
                    )

                    drawCircle(
                        color = Color.White,
                        center = Offset(portalX, portalY),
                        radius = 10f * scaleX
                    )
                }

                // C. Draw Flame Trails (Solar Veil)
                mTrails.forEach { trail ->
                    drawCircle(
                        color = Color(0xFFFFAA00).copy(alpha = trail.life / 60f),
                        center = Offset(trail.x * scaleX, trail.y * scaleY),
                        radius = 14f * scaleX
                    )
                }

                // D. Draw dropped scraps
                mDrops.forEach { drop ->
                    drawCircle(
                        color = NeonGreen,
                        center = Offset(drop.x * scaleX, drop.y * scaleY),
                        radius = 7f * scaleX
                    )
                    // Outer glow pulse
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.2f),
                        center = Offset(drop.x * scaleX, drop.y * scaleY),
                        radius = 12f * scaleX,
                        style = Stroke(width = 1.5f * scaleX)
                    )
                }

                // E. Render Projectiles
                mProjectiles.forEach { proj ->
                    drawCircle(
                        color = if (proj.isFriendly) ElectricCyan else NeonYellowWarning,
                        center = Offset(proj.x * scaleX, proj.y * scaleY),
                        radius = proj.radius * scaleX
                    )
                }

                // F. Render Enemies
                mEnemies.forEach { enemy ->
                    val color = when (enemy.type) {
                        EnemyType.Scout -> NeonCrimson
                        EnemyType.Shooter -> WarningYellow
                        EnemyType.Bomber -> Color(0xFFFF5500)
                        EnemyType.BossEcho -> GhostPurple
                    }

                    // Draw body circle
                    drawCircle(
                        color = color,
                        center = Offset(enemy.x * scaleX, enemy.y * scaleY),
                        radius = enemy.radius * scaleX
                    )

                    // Glow or shield arc for shade
                    if (enemy.type == EnemyType.BossEcho) {
                        drawCircle(
                            color = GhostPurple.copy(alpha = 0.3f),
                            center = Offset(enemy.x * scaleX, enemy.y * scaleY),
                            radius = (enemy.radius + 10f) * scaleX,
                            style = Stroke(width = 2f * scaleX)
                        )
                    }

                    // Mini Health bar above enemy
                    val barWidth = 40f * scaleX
                    val barHeight = 4f * scaleY
                    val barX = (enemy.x - 20f) * scaleX
                    val barY = (enemy.y - enemy.radius - 12f) * scaleY

                    // Background
                    drawRect(
                        color = Color.DarkGray,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, barHeight)
                    )
                    // Fills
                    val ratio = enemy.hp / enemy.maxHp
                    drawRect(
                        color = color,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth * ratio, barHeight)
                    )
                }

                // G. Render Player character
                val px = pValue.x * scaleX
                val py = pValue.y * scaleY
                val pr = pValue.radius * scaleX

                // Draw dash ghost traces if dodging
                if (pValue.isDodging) {
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.3f),
                        center = Offset(px - (pValue.dodgeDx * 20f * scaleX), py - (pValue.dodgeDy * 20f * scaleY)),
                        radius = pr
                    )
                }

                drawCircle(
                    color = ElectricCyan,
                    center = Offset(px, py),
                    radius = pr
                )

                // White accent inner battery core
                drawCircle(
                    color = Color.White,
                    center = Offset(px, py),
                    radius = pr * 0.45f
                )

                // Drawing slashing sweep brush arc if slashing
                if (pValue.isSlashing) {
                    val slashRange = (60f + if (activeItems.contains(RunItemType.COLOSSUS_BLADE)) 40f else 0f) * scaleX
                    val angleRad = pValue.slashAngle

                    drawArc(
                        color = ElectricCyan.copy(alpha = 0.4f),
                        startAngle = Math.toDegrees(angleRad.toDouble()).toFloat() - 45f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset(px - slashRange, py - slashRange),
                        size = Size(slashRange * 2f, slashRange * 2f)
                    )
                }
            }

            // FLOATING VISUAL TEXTS & DAMAGE SPARKS (overlay Comosable nodes)
            mVfxList.forEach { vfx ->
                val scaleXOffset = componentWidth / 800f
                val scaleYOffset = componentHeight / 800f
                val xPos = (vfx.x * scaleXOffset.value).dp
                val yPos = (vfx.y * scaleYOffset.value).dp

                Box(
                    modifier = Modifier
                        .offset(x = xPos, y = yPos)
                ) {
                    Text(
                        vfx.text,
                        color = vfx.color,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // CHEST ROGUELIKE UPGRADE SELECTION LAYER
            if (floorState == FloorStage.LOOT_SELECTION) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CarbonDark.copy(alpha = 0.85f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ASCENT CHEST UNLOCKED",
                            color = NeonGreen,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Acquire one mechanical blueprint modification card",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Column of 3 sliding option cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            lootOptions.forEach { option ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectLootChoice(option.type) }
                                        .testTag("loot_card_${option.type}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = option.name.uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            if (activeItems.contains(option.type)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "STOCKED",
                                                        color = NeonGreen,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(option.description, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Synergy Code: " + option.synergyComboText,
                                            color = ElectricCyan,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // BOTTOM COMPACT CONTROLS CONTROLLER DRIVER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. DYNAMIC VIRTUAL DRAGGING JOYSTICK CANVAS BOUNDARY
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SteelGrayCard)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                joystickCenter = offset
                                joystickOffset = Offset(0f, 0f)
                                isDraggingJoystick = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val rawNext = joystickOffset + dragAmount
                                // clamp inside 45f boundaries
                                val dist = sqrt(rawNext.x * rawNext.x + rawNext.y * rawNext.y)
                                if (dist <= 45f) {
                                    joystickOffset = rawNext
                                } else {
                                    val angle = atan2(rawNext.y, rawNext.x)
                                    joystickOffset = Offset(cos(angle) * 45f, sin(angle) * 45f)
                                }

                                // Drive player movement coordinates
                                val stepVectorX = joystickOffset.x / 45f
                                val stepVectorY = joystickOffset.y / 45f
                                    viewModel.player.value = pValue.copy(
                                        x = pValue.x + stepVectorX * pValue.speed,
                                        y = pValue.y + stepVectorY * pValue.speed
                                    )
                            },
                            onDragEnd = {
                                joystickOffset = Offset(0f, 0f)
                                isDraggingJoystick = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radiusBase = 45f
                    val knobRadius = 18f

                    // base circular channel
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radiusBase,
                        center = center
                    )
                    // base outline
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = radiusBase,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )

                    // Steering Knob
                    drawCircle(
                        color = if (isDraggingJoystick) ElectricCyan else Color.LightGray,
                        radius = knobRadius,
                        center = center + joystickOffset
                    )

                    // Joystick direction helper
                    if (isDraggingJoystick) {
                        drawLine(
                            color = ElectricCyan.copy(alpha = 0.6f),
                            start = center,
                            end = center + joystickOffset,
                            strokeWidth = 3f
                        )
                    }
                }
            }

            // B. PLAYER VECTORS LIFE/COOLDOWNS & MELEE ATTACK
            Column(modifier = Modifier.weight(1f)) {
                // Action Buttons Row: DASH & SLASH
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ACTION BUTTON 1: MELEE SWEEP SLASH (touches 48dp criteria)
                    Button(
                        onClick = { viewModel.executeSlash(0f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("action_slash_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "SLASH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // ACTION BUTTON 2: DODGE DASH / PHASING
                    Button(
                        onClick = { viewModel.executeDodge(1f, 0f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("action_dodge_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "DODGE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LIFE TELEMETRY STREAM
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HEALTH INTEGRITY",
                            fontSize = 8.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${pValue.hp.toInt()} / ${pValue.maxHp.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (pValue.hp < pValue.maxHp * 0.3f) NeonCrimson else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    LinearProgressIndicator(
                        progress = { pValue.hp / pValue.maxHp },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (pValue.hp < pValue.maxHp * 0.3f) NeonCrimson else ElectricCyan,
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ADVANCED DEBUG CHEAT CONSOLE (Warps to levels)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SteelGrayCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "CHEAT CONSOLE DUPLICATE WARP:",
                fontSize = 9.sp,
                color = WarningYellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 14, 45, 99).forEach { targetF ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CarbonDark)
                            .border(1.dp, WarningYellow.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .clickable { viewModel.skipToFloor(targetF) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (targetF == 99) "F99 🏆" else "F$targetF",
                            fontSize = 8.sp,
                            color = WarningYellow,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// IV. VICTORY SCREEN
// ==========================================
@Composable
fun VictoryDeckScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "CHALLENGE MASTERED",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "YOU BEAT THE GAME AND CONQUERED FLOOR 99!",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SteelGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A major achievement! Scaling of 99 individual procedural steps required extreme synergy execution. Your scrap multiplier has been rewarded with premium bonuses of +500 items to level up permanently at Base Camp.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.setScreen(GameScreen.HUB_BASE_CAMP) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "RETURN TO BASE CAMP HUB",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Warning color token fallback
val NeonYellowWarning = Color(0xFFFFAA00)
