package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DeathEcho
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.Progression
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// --- Core Gameplay Models ---
data class PlayerState(
    val x: Float = 400f,
    val y: Float = 400f,
    val radius: Float = 16f,
    val hp: Float = 100f,
    val maxHp: Float = 100f,
    val speed: Float = 6.5f,
    val slashCooldown: Int = 0,
    val isSlashing: Boolean = false,
    val slashAngle: Float = 0f,
    val slashArcRemaining: Int = 0,
    val dodgeCooldown: Int = 0,
    val isDodging: Boolean = false,
    val dodgeRemaining: Int = 0,
    val dodgeDx: Float = 0f,
    val dodgeDy: Float = 0f
)

sealed interface EnemyType {
    object Scout : EnemyType
    object Shooter : EnemyType
    object Bomber : EnemyType
    object BossEcho : EnemyType // The Death Echo corpse boss!
}

data class Enemy(
    val id: String = Random.nextLong().toString(),
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val radius: Float,
    val hp: Float,
    val maxHp: Float,
    val speed: Float,
    val shootCooldown: Int = 0
)

data class Projectile(
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val radius: Float,
    val damage: Float,
    val isFriendly: Boolean
)

data class ScrapDrop(
    val x: Float,
    val y: Float,
    val amount: Int
)

data class FlameTrial(
    val x: Float,
    val y: Float,
    var life: Int = 60
)

data class GameVfx(
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    var life: Int = 30, // ticks
    val vx: Float = 0f,
    val vy: Float = -1.5f
)

enum class GameScreen {
    GDD_HUBS,
    HUB_BASE_CAMP,
    ACTIVE_RUN,
    VICTORY_DECK
}

enum class FloorStage {
    COMBAT,
    ESCAPE, // Portal is active, step on it to finish the level!
    LOOT_SELECTION
}

// Custom structure for items the player can equip during runs
data class RunItem(
    val name: String,
    val description: String,
    val type: RunItemType,
    val synergyComboText: String = ""
)

enum class RunItemType {
    LIFESTEAL_FANGS,
    SOLAR_VEIL,     // Flame trail
    TESLA_RING,     // Chain lightning
    DODGE_CHARGER,  // Fast dodge / thunder dash
    NITROLYTE_CORE, // Death explosion
    COLOSSUS_BLADE  // Giant slash
}

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // --- Database Flows ---
    val progression: StateFlow<Progression> = repository.progression
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Progression())

    val activeDeathEchoes: StateFlow<List<DeathEcho>> = repository.activeDeathEchoes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation & High-level State Flow ---
    private val _currentScreen = MutableStateFlow(GameScreen.ACTIVE_RUN)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    // --- Interactive GDD UI State ---
    private val _selectedGddTab = MutableStateFlow("COMBAT") // COMBAT, PROGRESSION, DESIGN, STEPS
    val selectedGddTab: StateFlow<String> = _selectedGddTab.asStateFlow()

    private val _selectedBiomeIndex = MutableStateFlow(0) // 0-4
    val selectedBiomeIndex: StateFlow<Int> = _selectedBiomeIndex.asStateFlow()

    private val _synergyItem1 = MutableStateFlow<RunItemType?>(null)
    val synergyItem1: StateFlow<RunItemType?> = _synergyItem1.asStateFlow()

    private val _synergyItem2 = MutableStateFlow<RunItemType?>(null)
    val synergyItem2: StateFlow<RunItemType?> = _synergyItem2.asStateFlow()

    // --- ACTIVE RUN GAMEPLAY STATE Flow ---
    val currentFloor = MutableStateFlow(1)
    val floorStage = MutableStateFlow(FloorStage.COMBAT)

    val player = MutableStateFlow(PlayerState())
    val enemies = MutableStateFlow<List<Enemy>>(emptyList())
    val projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val scrapsInRun = MutableStateFlow<List<ScrapDrop>>(emptyList())
    val flameTrails = MutableStateFlow<List<FlameTrial>>(emptyList())
    val vfxList = MutableStateFlow<List<GameVfx>>(emptyList())

    val currentRunScraps = MutableStateFlow(0)
    val activeRunItems = MutableStateFlow<Set<RunItemType>>(emptySet())
    val lootOptions = MutableStateFlow<List<RunItem>>(emptyList())

    // Modifier for current floor
    val activeModifierString = MutableStateFlow("NORMAL") // e.g., "ENEMIES EXPLODE", "MELT DANGER", "NO REGEN", "GRAVITY WELLS"

    // Death Echo status on active floor
    val floorDeathEcho = MutableStateFlow<DeathEcho?>(null)
    val bossDefeatedMessage = MutableStateFlow<String?>(null)

    // Game loop control
    private var gameLoopJob: Job? = null

    init {
        // Prepare initial synergies in GDD
        _synergyItem1.value = RunItemType.LIFESTEAL_FANGS
        _synergyItem2.value = RunItemType.SOLAR_VEIL
        startNewRun(1)
    }

    fun setScreen(screen: GameScreen) {
        _currentScreen.value = screen
        if (screen == GameScreen.ACTIVE_RUN) {
            // Start run
        } else {
            stopGameLoop()
        }
    }

    fun setGddTab(tab: String) {
        _selectedGddTab.value = tab
    }

    fun setBiomeIndex(index: Int) {
        _selectedBiomeIndex.value = index
    }

    fun setSynergySlots(slot1: RunItemType?, slot2: RunItemType?) {
        _synergyItem1.value = slot1
        _synergyItem2.value = slot2
    }

    // --- Permanent Upgrades in Base Camp (Floor 0) ---
    fun purchaseUpgrade(type: String) {
        viewModelScope.launch {
            repository.updateProgression { current ->
                val cost = when(type) {
                    "health" -> (current.baseHealthLevel + 1) * 20
                    "damage" -> (current.baseDamageLevel + 1) * 25
                    "speed" -> (current.baseSpeedLevel + 1) * 30
                    "dodge" -> (current.baseDodgeLevel + 1) * 35
                    else -> 9999
                }
                if (current.totalScrap >= cost) {
                    val nextScrap = current.totalScrap - cost
                    when (type) {
                        "health" -> current.copy(totalScrap = nextScrap, baseHealthLevel = current.baseHealthLevel + 1)
                        "damage" -> current.copy(totalScrap = nextScrap, baseDamageLevel = current.baseDamageLevel + 1)
                        "speed" -> current.copy(totalScrap = nextScrap, baseSpeedLevel = current.baseSpeedLevel + 1)
                        "dodge" -> current.copy(totalScrap = nextScrap, baseDodgeLevel = current.baseDodgeLevel + 1)
                        else -> current
                    }
                } else {
                    current
                }
            }
        }
    }

    // --- Sandboxed Game Engine Mechanics ---
    fun startNewRun(startFloor: Int = 1) {
        viewModelScope.launch {
            val prog = repository.getProgressionDirect()
            currentFloor.value = startFloor
            currentRunScraps.value = 0
            activeRunItems.value = emptySet()
            projectiles.value = emptyList()
            scrapsInRun.value = emptyList()
            flameTrails.value = emptyList()
            vfxList.value = emptyList()
            bossDefeatedMessage.value = null

            // Calculate starting player health based on permanent baseHealthLevel
            val calculatedMaxHp = 100f + (prog.baseHealthLevel * 10f)
            val calculatedSpeed = 6.0f + (prog.baseSpeedLevel * 0.3f)

            player.value = PlayerState(
                x = 400f,
                y = 600f,
                maxHp = calculatedMaxHp,
                hp = calculatedMaxHp,
                speed = calculatedSpeed
            )

            // Determine floor parameters and boot floor
            setupFloor(startFloor)
            setScreen(GameScreen.ACTIVE_RUN)
            startGameLoop()
        }
    }

    private suspend fun setupFloor(floor: Int) {
        floorStage.value = FloorStage.COMBAT
        projectiles.value = emptyList()
        flameTrails.value = emptyList()

        // Randomly select mod matching current biome
        activeModifierString.value = getModifierForFloor(floor)

        // Fetch Death Echo for this floor (if any matches room database)
        val echo = repository.getDeathEchoForFloor(floor)
        floorDeathEcho.value = echo

        // Generate enemy list
        val enemyList = mutableListOf<Enemy>()
        val baseCount = 3 + floor / 2
        val count = baseCount.coerceAtMost(10)

        // Spawn normal enemies
        for (i in 1..count) {
            val type = if (floor > 10 && Random.nextFloat() > 0.5f) {
                if (Random.nextFloat() > 0.6f) EnemyType.Bomber else EnemyType.Shooter
            } else {
                EnemyType.Scout
            }

            enemyList.add(
                Enemy(
                    type = type,
                    x = Random.nextInt(100, 700).toFloat(),
                    y = Random.nextInt(100, 450).toFloat(),
                    radius = if (type == EnemyType.Bomber) 18f else 15f,
                    hp = 40f + (floor * 5),
                    maxHp = 40f + (floor * 5),
                    speed = 2f + Random.nextFloat() * 1.5f
                )
            )
        }

        // Spawn Corps/Death Echo Boss if found!
        if (echo != null) {
            enemyList.add(
                Enemy(
                    type = EnemyType.BossEcho,
                    x = echo.xRatio * 800f,
                    y = echo.yRatio * 800f,
                    radius = 32f,
                    hp = 180f + (floor * 20),
                    maxHp = 180f + (floor * 20),
                    speed = 1.8f
                )
            )
        }

        enemies.value = enemyList

        // Show floating floor notification
        vfxList.value = listOf(
            GameVfx(
                x = 400f,
                y = 350f,
                text = "FLOOR $floor BEGINS",
                color = Color(0xFF00FF87),
                life = 60
            )
        )
    }

    private fun getModifierForFloor(floor: Int): String {
        return when {
            floor == 99 -> "TOTAL APOCALYPSE"
            floor % 10 == 0 -> "BOSS DUEL"
            floor in 1..10 -> "NORMAL"
            floor in 11..20 -> "ENEMIES EXPLODE"
            floor in 21..30 -> "NO RECOVERY"
            floor in 31..40 -> "BURNING PATHS"
            floor in 41..50 -> "GRAVITY WELLS"
            else -> {
                val pool = listOf("ENEMIES EXPLODE", "NO RECOVERY", "BURNING PATHS", "GRAVITY WELLS", "NORMAL")
                pool[floor % pool.size]
            }
        }
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                // ~60 FPS update tick delay (16ms)
                delay(16)
                updateGameTick()
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // --- Second-to-Second Gameplay Simulator Logic ---
    private fun updateGameTick() {
        val p = player.value
        val currentEnemies = enemies.value
        val currentProjectiles = projectiles.value
        val currentTrails = flameTrails.value.toMutableList()
        val currentVfx = vfxList.value.toMutableList()
        val currentDrops = scrapsInRun.value.toMutableList()

        if (p.hp <= 0) {
            handlePlayerDeath()
            return
        }

        // 1. Update Player State (Cooling, Dodge and Slash timers)
        var nextSlashCooldown = if (p.slashCooldown > 0) p.slashCooldown - 1 else 0
        var nextDodgeCooldown = if (p.dodgeCooldown > 0) p.dodgeCooldown - 1 else 0
        var nextIsSlashing = p.isSlashing
        var nextSlashArc = p.slashArcRemaining
        var nextIsDodging = p.isDodging
        var nextDodgeRem = p.dodgeRemaining

        var px = p.x
        var py = p.y

        // Dodge logic movement
        if (p.isDodging) {
            px += p.dodgeDx * 12f
            py += p.dodgeDy * 12f
            nextDodgeRem--
            if (nextDodgeRem <= 0) {
                nextIsDodging = false
            }
        }

        // Bound player inside 800 x 800 box
        px = px.coerceIn(20f, 780f)
        py = py.coerceIn(20f, 780f)

        // Slash animation frames
        if (p.isSlashing) {
            nextSlashArc--
            if (nextSlashArc <= 0) {
                nextIsSlashing = false
            }
        }

        // Trail emission (Solar Veil synergy item)
        if (activeRunItems.value.contains(RunItemType.SOLAR_VEIL) && !nextIsDodging) {
            if (Random.nextFloat() > 0.82f) {
                currentTrails.add(FlameTrial(px, py))
            }
        }

        // Ticking down trails
        currentTrails.forEach { it.life-- }
        currentTrails.removeAll { it.life <= 0 }

        // 2. Projectile Engine
        val nextProjectiles = mutableListOf<Projectile>()
        currentProjectiles.forEach { proj ->
            val nx = proj.x + proj.dx
            val ny = proj.y + proj.dy

            // Boundary collision
            if (nx in 0f..800f && ny in 0f..800f) {
                // Player collision check for enemy projectiles
                if (!proj.isFriendly) {
                    val dist = sqrt((nx - px) * (nx - px) + (ny - py) * (ny - py))
                    if (dist < p.radius + proj.radius && !nextIsDodging) {
                        // Player hit!
                        val dmg = proj.damage
                        val newHp = (p.hp - dmg).coerceIn(0f, p.maxHp)
                        player.value = p.copy(hp = newHp)
                        currentVfx.add(GameVfx(px, py - 10, "-${dmg.toInt()}", Color(0xFFFF2B55)))
                        // Skip adding projectile
                        return@forEach
                    }
                }
                nextProjectiles.add(proj.copy(x = nx, y = ny))
            }
        }

        // 3. Enemy AI and Movement
        val nextEnemies = mutableListOf<Enemy>()
        val speedFactor = 1.0f

        currentEnemies.forEach { enemy ->
            var ex = enemy.x
            var ey = enemy.y
            val dx = px - ex
            val dy = py - ey
            val dist = sqrt(dx * dx + dy * dy)

            // Dynamic Enemy movement behavior
            if (dist > 5f) {
                val moveSpeed = enemy.speed * speedFactor
                ex += (dx / dist) * moveSpeed
                ey += (dy / dist) * moveSpeed
            }

            // Damage player if in melee contact
            if (dist < p.radius + enemy.radius && !nextIsDodging) {
                if (Random.nextFloat() > 0.95f) { // Melee tick delay
                    val dmg = 10f + (currentFloor.value * 1.5f)
                    val newHp = (player.value.hp - dmg).coerceIn(0f, p.maxHp)
                    player.value = player.value.copy(hp = newHp)
                    currentVfx.add(GameVfx(px, py, "-${dmg.toInt()}", Color(0xFFFF2B55)))
                }
            }

            // Shoot mechanics for Ranged Shooter
            var nextShootCooldown = enemy.shootCooldown
            if (enemy.type == EnemyType.Shooter) {
                if (nextShootCooldown > 0) {
                    nextShootCooldown--
                } else {
                    // Shoot projectile towards player
                    if (dist > 10f) {
                        val projDx = (dx / dist) * 7f
                        val projDy = (dy / dist) * 7f
                        nextProjectiles.add(
                            Projectile(
                                x = ex,
                                y = ey,
                                dx = projDx,
                                dy = projDy,
                                radius = 7f,
                                damage = 12f + currentFloor.value,
                                isFriendly = false
                            )
                        )
                        nextShootCooldown = 75 // frames between shots
                    }
                }
            }

            // Damaged by burning trails (Solar Veil synergy item)
            var currentEnemyHp = enemy.hp
            if (activeRunItems.value.contains(RunItemType.SOLAR_VEIL)) {
                currentTrails.forEach { trail ->
                    val tDist = sqrt((trail.x - ex) * (trail.x - ex) + (trail.y - ey) * (trail.y - ey))
                    if (tDist < enemy.radius + 12f) {
                        if (Random.nextFloat() > 0.96f) { // fire tick rate
                            val dmg = 8f
                            currentEnemyHp -= dmg
                            currentVfx.add(GameVfx(ex, ey - 10, "${dmg.toInt()} 🔥", Color(0xFFFFAA00)))
                        }
                    }
                }
            }

            // Check if Slash catches this enemy
            if (nextIsSlashing && nextSlashArc > 0) {
                val slashRange = 55f + if (activeRunItems.value.contains(RunItemType.COLOSSUS_BLADE)) 40f else 0f
                val sDist = sqrt((ex - px) * (ex - px) + (ey - py) * (ey - py))
                if (sDist < slashRange) {
                    // Calculate angle difference to ensure slash is in general direction, but simplify for action ease
                    if (Random.nextFloat() > 0.85f) { // combat sweep hitbox hit
                        val prog = progression.value
                        val baseDmg = 35f + (prog.baseDamageLevel * 3f)
                        var finalDmg = baseDmg + (if (activeRunItems.value.contains(RunItemType.COLOSSUS_BLADE)) 25f else 0f)

                        // Boss specific bonuses
                        if (enemy.type == EnemyType.BossEcho && activeRunItems.value.contains(RunItemType.COLOSSUS_BLADE)) {
                            finalDmg *= 1.8f
                        }

                        currentEnemyHp -= finalDmg
                        currentVfx.add(GameVfx(ex, ey - 10, "${finalDmg.toInt()}", Color(0xFF00D2FF)))
                    }
                }
            }

            if (currentEnemyHp > 0) {
                nextEnemies.add(enemy.copy(x = ex, y = ey, hp = currentEnemyHp, shootCooldown = nextShootCooldown))
            } else {
                // Enemy killed! Trigger scrap drops and special on-kill abilities
                val dropQty = if (enemy.type == EnemyType.BossEcho) 12 else 1 + Random.nextInt(3)
                currentDrops.add(ScrapDrop(ex, ey, dropQty))

                if (enemy.type == EnemyType.BossEcho) {
                    bossDefeatedMessage.value = "Spectral Echo Reclaimed! +${floorDeathEcho.value?.scrapStored} Scraps!"
                    // Delete the physical Echo from db
                    viewModelScope.launch {
                        floorDeathEcho.value?.let { repository.claimDeathEcho(it) }
                        floorDeathEcho.value = null
                    }
                }

                // Vamp Vamp synergy check
                if (activeRunItems.value.contains(RunItemType.LIFESTEAL_FANGS)) {
                    val healAmt = if (activeRunItems.value.contains(RunItemType.SOLAR_VEIL)) 12f else 6f
                    val nextHp = (player.value.hp + healAmt).coerceAtMost(p.maxHp)
                    player.value = player.value.copy(hp = nextHp)
                    currentVfx.add(GameVfx(px, py - 20, "+${healAmt.toInt()} ❤️", Color(0xFF00FF87)))
                }

                // Nitrolyte Death Explode enemy on death modifier or item synergy!
                if (activeRunItems.value.contains(RunItemType.NITROLYTE_CORE) || activeModifierString.value == "ENEMIES EXPLODE") {
                    currentVfx.add(GameVfx(ex, ey, "💥 BOOM!", Color(0xFFFFAA00)))
                    // Damage nearby enemies and player!
                    val range = 90f
                    // We'll calculate splash on other enemies currently alive
                    // In-line explosion effect
                    nextEnemies.forEachIndexed { idx, other ->
                        val oDist = sqrt((other.x - ex) * (other.x - ex) + (other.y - ey) * (other.y - ey))
                        if (oDist < range) {
                            nextEnemies[idx] = other.copy(hp = other.hp - 35f)
                            currentVfx.add(GameVfx(other.x, other.y - 12, "35 💥", Color(0xFFFFAA00)))
                        }
                    }

                    // Damage player if in range
                    val pDist = sqrt((px - ex) * (px - ex) + (py - ey) * (py - ey))
                    if (pDist < range && !nextIsDodging) {
                        val pDmg = 20f
                        player.value = player.value.copy(hp = (player.value.hp - pDmg).coerceAtLeast(0f))
                        currentVfx.add(GameVfx(px, py - 15, "-${pDmg.toInt()} 💥", Color(0xFFFF2B55)))
                    }
                }
            }
        }

        // 4. Magnetize and pickup scraps
        val finalDrops = mutableListOf<ScrapDrop>()
        currentDrops.forEach { drop ->
            val dxd = px - drop.x
            val dyd = py - drop.y
            val dist = sqrt(dxd * dxd + dyd * dyd)

            if (dist < 40f) { // Pickup
                val recovered = drop.amount
                currentRunScraps.value += recovered
                currentVfx.add(GameVfx(drop.x, drop.y - 10, "+$recovered", Color(0xFF00FF87)))
            } else {
                finalDrops.add(drop)
            }
        }

        // 5. Automatic healing logic if NOT in "NO RECOVERY" modified room
        if (activeModifierString.value != "NO RECOVERY") {
            if (Random.nextFloat() > 0.993f && player.value.hp < p.maxHp) {
                // minor natural regen
                player.value = player.value.copy(hp = (player.value.hp + 2f).coerceAtMost(p.maxHp))
            }
        }

        // 6. Check if combat is cleared -> Exit staircase portal spawns!
        if (nextEnemies.isEmpty() && floorStage.value == FloorStage.COMBAT) {
            floorStage.value = FloorStage.ESCAPE
            currentVfx.add(
                GameVfx(
                    400f,
                    200f,
                    "PORTAL OPENED. DESCENT SECURED!",
                    Color(0xFF00FF87),
                    60
                )
            )
        }

        // Portal exit colission checker
        if (floorStage.value == FloorStage.ESCAPE) {
            val portalX = 400f
            val portalY = 200f
            val dist = sqrt((px - portalX) * (px - portalX) + (py - portalY) * (py - portalY))
            if (dist < 40f) {
                handleFloorClearAndLoot()
            }
        }

        // Ticking VFX lists
        currentVfx.forEach {
            it.life--
            // let float upward slightly
            // vx is 0, vy is -1.5
        }
        currentVfx.removeAll { it.life <= 0 }

        // Update active values
        player.value = p.copy(
            x = px,
            y = py,
            slashCooldown = nextSlashCooldown,
            dodgeCooldown = nextDodgeCooldown,
            isSlashing = nextIsSlashing,
            slashArcRemaining = nextSlashArc,
            isDodging = nextIsDodging,
            dodgeRemaining = nextDodgeRem
        )
        enemies.value = nextEnemies
        projectiles.value = nextProjectiles
        scrapsInRun.value = finalDrops
        flameTrails.value = currentTrails
        vfxList.value = currentVfx
    }

    // --- Action Button Triggers from Composable canvas controller ---
    fun executeSlash(angle: Float) {
        val p = player.value
        if (p.slashCooldown <= 0 && !p.isSlashing && !p.isDodging) {
            player.value = p.copy(
                isSlashing = true,
                slashCooldown = 15, // 0.25 sec cooldown
                slashAngle = angle,
                slashArcRemaining = 8 // ticks active
            )

            // Trigger chain lightning if Tesla Ring is active (Tesla synergy item)
            if (activeRunItems.value.contains(RunItemType.TESLA_RING)) {
                triggerTeslaLightning(p.x, p.y)
            }
        }
    }

    private fun triggerTeslaLightning(px: Float, py: Float) {
        val currentEnemies = enemies.value
        if (currentEnemies.isEmpty()) return

        // Zip strike to up to 3 closest enemies
        val sorted = currentEnemies.sortedBy { e ->
            sqrt((e.x - px) * (e.x - px) + (e.y - py) * (e.y - py))
        }.take(3)

        sorted.forEach { enemy ->
            // Mark lightning damage
            vfxList.value = vfxList.value + GameVfx(enemy.x, enemy.y, "⚡ 15", Color(0xFF00D2FF), life = 25)
            // Hurt enemy
            enemies.value = enemies.value.map {
                if (it.id == enemy.id) it.copy(hp = it.hp - 15f) else it
            }
        }
    }

    fun executeDodge(dx: Float, dy: Float) {
        val p = player.value
        if (p.dodgeCooldown <= 0 && !p.isDodging) {
            // Apply cooldown reduction from permanent dodge stats
            val prog = progression.value
            val cdr = (prog.baseDodgeLevel * 0.05f).coerceAtMost(0.4f)
            val cd = (45f * (1f - cdr)).toInt()

            player.value = p.copy(
                isDodging = true,
                dodgeCooldown = cd,
                dodgeRemaining = 6, // fast frames
                dodgeDx = dx,
                dodgeDy = dy
            )

            // Electro-dash trigger (Tesla Ring + Dodge Synergy item)
            if (activeRunItems.value.contains(RunItemType.TESLA_RING) && activeRunItems.value.contains(RunItemType.DODGE_CHARGER)) {
                // Blast full circle of sparks representing electric surge!
                for (i in 0..7) {
                    val angle = (i * Math.PI / 4).toFloat()
                    projectiles.value = projectiles.value + Projectile(
                        x = p.x,
                        y = p.y,
                        dx = cos(angle) * 8f,
                        dy = sin(angle) * 8f,
                        radius = 8f,
                        damage = 22f,
                        isFriendly = true
                    )
                }
                vfxList.value = vfxList.value + GameVfx(p.x, p.y - 10, "⚡⚡ EMP SHOCK ⚡⚡", Color(0xFF00D2FF), life = 35)
            }
        }
    }

    // --- Progression Level Cleared & Roguelike Item Rewards ---
    private fun handleFloorClearAndLoot() {
        if (currentFloor.value == 99) {
            // BEAT GAME VICTORY!
            viewModelScope.launch {
                repository.updateProgression { current ->
                    current.copy(
                        totalScrap = current.totalScrap + currentRunScraps.value + 500, // Victory bulk sum!
                        deepestFloorReached = 99,
                        runsCompleted = current.runsCompleted + 1
                    )
                }
            }
            stopGameLoop()
            setScreen(GameScreen.VICTORY_DECK)
            return
        }

        stopGameLoop()
        floorStage.value = FloorStage.LOOT_SELECTION

        // Generate 3 unique random card items representing roguelike loot!
        val pool = listOf(
            RunItem("Lifesteal Fangs", "Heal HP upon slaying enemies. Keeps you alive.", RunItemType.LIFESTEAL_FANGS, "Vampiric Inferno (Fangs + Veil): Fire trails steal HP!"),
            RunItem("Solar Veil", "Emits blazing chemical flame trails that melt enemies under foot.", RunItemType.SOLAR_VEIL, "Vampiric Inferno (Fangs + Veil): Fire trails steal HP!"),
            RunItem("Tesla Ring", "Releases arcs of chain lightning shockwaves on attack.", RunItemType.TESLA_RING, "Electrolyte Dash (Ring + Dodge): Dodging discharge!"),
            RunItem("Dodge Charger", "Reduce dodge cool downs by 30%. Yields extra invincibility frames.", RunItemType.DODGE_CHARGER, "Electrolyte Dash (Ring + Dodge): Dodging discharge!"),
            RunItem("Nitrolyte Core", "Enemies erupt in chemical flames on death, exploding surrounding items.", RunItemType.NITROLYTE_CORE, "Nuclear Retaliation (Core + Blade): Exploding cascades!"),
            RunItem("Colossus Blade", "Double your sword's slice sweep width and damage output.", RunItemType.COLOSSUS_BLADE, "Nuclear Retaliation (Core + Blade): Exploding cascades!")
        )

        // Select 3 random different items
        val shuffled = pool.shuffled()
        lootOptions.value = shuffled.take(3)
    }

    fun selectLootChoice(type: RunItemType) {
        // Collect item into active list
        activeRunItems.value = activeRunItems.value + type

        // Advance Floor
        val nextFloor = currentFloor.value + 1
        currentFloor.value = nextFloor

        viewModelScope.launch {
            repository.updateProgression { current ->
                val highestFloor = if (nextFloor > current.deepestFloorReached) nextFloor else current.deepestFloorReached
                current.copy(deepestFloorReached = highestFloor)
            }

            // Generate next stage environment
            setupFloor(nextFloor)
            startGameLoop()
        }
    }

    // --- Permadeath corpse logic (The Death Echo) ---
    private fun handlePlayerDeath() {
        stopGameLoop()
        val deadFloor = currentFloor.value
        val deadXRatio = player.value.x / 800f
        val deadYRatio = player.value.y / 800f
        val deadScraps = currentRunScraps.value

        // Trigger database saving of this Death Echo corpse marker & progression
        viewModelScope.launch {
            if (deadScraps > 0) {
                // Save corpse
                repository.createDeathEcho(
                    floor = deadFloor,
                    xRatio = deadXRatio,
                    yRatio = deadYRatio,
                    scrapStored = deadScraps
                )
            }

            repository.updateProgression { current ->
                current.copy(
                    runsCompleted = current.runsCompleted + 1,
                    totalScrap = current.totalScrap + (deadScraps * 0.10f).toInt() // Keep 10% pity bonus scraps!
                )
            }

            // Reset current floor index
            currentFloor.value = 1
            enemies.value = emptyList()
            projectiles.value = emptyList()
            setScreen(GameScreen.HUB_BASE_CAMP)
        }
    }

    fun skipToFloor(target: Int) {
        viewModelScope.launch {
            setupFloor(target)
            currentFloor.value = target
            startNewRun(target)
        }
    }

    override fun onCleared() {
        stopGameLoop()
        super.onCleared()
    }
}

// ViewModel Factory supporting clean Context parameters
class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = Room.databaseBuilder(
            context.applicationContext,
            GameDatabase::class.java,
            "game_database"
        ).fallbackToDestructiveMigration().build()

        val repository = GameRepository(database.gameDao())

        @Suppress("UNCHECKED_CAST")
        return GameViewModel(repository) as T
    }
}
