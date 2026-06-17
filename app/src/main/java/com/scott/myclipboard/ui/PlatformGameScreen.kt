package com.scott.myclipboard.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class GamePlatform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

private data class GameCoin(
    val x: Float,
    val y: Float,
    val radius: Float = 18f,
    var collected: Boolean = false,
)

private data class GameEnemy(
    val startX: Float,
    val endX: Float,
    var y: Float,
    var x: Float,
    var direction: Float = 1f,
    var defeated: Boolean = false,
    var eatenProgress: Float = 0f,
    var eatenFromX: Float = x,
    var eatenFromY: Float = y,
    var grabbed: Boolean = false,
    var spinTaps: Int = 0,
    var spinAngle: Float = 0f,
    var thrown: Boolean = false,
    var throwVelocityX: Float = 0f,
    var throwVelocityY: Float = 0f,
    var respawnDelay: Float = 0f,
)

private data class GameEggShot(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    val homesToBoss: Boolean = false,
)

private data class BossAttackEgg(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    var progress: Float = 0f,
    var warningTimer: Float = 1.25f,
    var landed: Boolean = false,
    var collected: Boolean = false,
)

private data class GameTree(
    val x: Float,
    val y: Float,
    var appleSpawned: Boolean = false,
)

private data class GameApple(
    val treeX: Float,
    var x: Float,
    var y: Float,
    var age: Float = 0f,
    var collected: Boolean = false,
)

private data class GiantBossState(
    var x: Float,
    var direction: Float = -1f,
    var health: Int = 3,
    var timer: Float = 14f,
    var hitCooldown: Float = 0f,
    var pushTimer: Float = 0f,
    var pushCooldown: Float = 1.8f,
    var shoveCooldown: Float = 0f,
    var deathTimer: Float = 0f,
    var deathFall: Float = 0f,
)

private const val StageOneWorldWidth = 4200f
private const val StageTwoWorldWidth = 4700f
private const val StageThreeWorldWidth = 5000f
private const val StageFourWorldWidth = 4600f
private const val StageFiveWorldWidth = 5200f
private const val StageSixWorldWidth = 5000f
private const val StageSevenWorldWidth = 4200f
private const val StageEightWorldWidth = 5200f
private const val StageNineWorldWidth = 39200f

private fun stageWorldWidth(stage: Int): Float = when (stage) {
    2 -> StageTwoWorldWidth
    3 -> StageThreeWorldWidth
    4 -> StageFourWorldWidth
    5 -> StageFiveWorldWidth
    6 -> StageSixWorldWidth
    7 -> StageSevenWorldWidth
    8 -> StageEightWorldWidth
    9 -> StageNineWorldWidth
    else -> StageOneWorldWidth
}

private fun stageGoalX(stage: Int, worldWidth: Float): Float = when (stage) {
    8 -> 2520f
    else -> worldWidth - 140f
}

private fun stageGoalBaseY(stage: Int): Float = when (stage) {
    8 -> 820f
    else -> 760f
}

private fun isStageOneWarpZone(stage: Int, playerX: Float): Boolean {
    if (stage != 1) return false
    val playerCenterX = playerX + 29f
    return playerCenterX in 500f..720f
}

private fun stagePlatforms(stage: Int): List<GamePlatform> = when (stage) {
    2 -> listOf(
            GamePlatform(0f, 760f, 430f, 72f),
            GamePlatform(530f, 650f, 180f, 42f),
            GamePlatform(820f, 560f, 170f, 42f),
            GamePlatform(1110f, 470f, 190f, 42f),
            GamePlatform(1420f, 590f, 180f, 42f),
            GamePlatform(1740f, 500f, 190f, 42f),
            GamePlatform(2060f, 410f, 180f, 42f),
            GamePlatform(2380f, 530f, 210f, 42f),
            GamePlatform(2730f, 450f, 190f, 42f),
            GamePlatform(3050f, 610f, 220f, 42f),
            GamePlatform(3420f, 520f, 190f, 42f),
            GamePlatform(3750f, 430f, 190f, 42f),
            GamePlatform(4070f, 570f, 210f, 42f),
            GamePlatform(4440f, 760f, 420f, 72f),
        )
    3 -> listOf(
            GamePlatform(0f, 760f, 430f, 72f),
            GamePlatform(520f, 640f, 170f, 46f),
            GamePlatform(800f, 520f, 160f, 46f),
            GamePlatform(1110f, 610f, 180f, 46f),
            GamePlatform(1410f, 460f, 170f, 46f),
            GamePlatform(1720f, 560f, 190f, 46f),
            GamePlatform(2050f, 420f, 170f, 46f),
            GamePlatform(2360f, 520f, 210f, 46f),
            GamePlatform(2720f, 390f, 180f, 46f),
            GamePlatform(3060f, 540f, 190f, 46f),
            GamePlatform(3410f, 450f, 170f, 46f),
            GamePlatform(3740f, 590f, 210f, 46f),
            GamePlatform(4110f, 470f, 180f, 46f),
            GamePlatform(4440f, 620f, 210f, 46f),
            GamePlatform(4780f, 760f, 420f, 72f),
        )
    4 -> listOf(
            GamePlatform(0f, 760f, 560f, 72f),
            GamePlatform(650f, 700f, 260f, 52f),
            GamePlatform(1040f, 640f, 280f, 52f),
            GamePlatform(1460f, 710f, 340f, 52f),
            GamePlatform(1940f, 610f, 260f, 52f),
            GamePlatform(2320f, 690f, 300f, 52f),
            GamePlatform(2760f, 590f, 300f, 52f),
            GamePlatform(3200f, 700f, 330f, 52f),
            GamePlatform(3660f, 620f, 300f, 52f),
            GamePlatform(4100f, 760f, 560f, 72f),
        )
    5 -> listOf(
            GamePlatform(0f, 760f, 520f, 72f),
            GamePlatform(620f, 700f, 240f, 48f),
            GamePlatform(980f, 620f, 220f, 48f),
            GamePlatform(1320f, 540f, 240f, 48f),
            GamePlatform(1690f, 660f, 280f, 48f),
            GamePlatform(2080f, 580f, 260f, 48f),
            GamePlatform(2460f, 470f, 240f, 48f),
            GamePlatform(2820f, 610f, 320f, 48f),
            GamePlatform(3260f, 520f, 280f, 48f),
            GamePlatform(3640f, 650f, 320f, 48f),
            GamePlatform(4080f, 560f, 300f, 48f),
            GamePlatform(4490f, 470f, 260f, 48f),
            GamePlatform(4850f, 760f, 420f, 72f),
        )
    6 -> listOf(
            GamePlatform(0f, 760f, 5200f, 92f),
        )
    7 -> listOf(
            GamePlatform(0f, 760f, 4200f, 92f),
        )
    8 -> listOf(
            GamePlatform(0f, 760f, 620f, 92f),
            GamePlatform(740f, 700f, 520f, 56f),
            GamePlatform(1360f, 620f, 920f, 46f),
            GamePlatform(2140f, 820f, 900f, 78f),
            GamePlatform(3040f, 620f, 1060f, 46f),
            GamePlatform(4320f, 760f, 560f, 92f),
        )
    9 -> listOf(
            GamePlatform(0f, 760f, 5200f, 74f),
            GamePlatform(5480f, 700f, 2600f, 62f),
            GamePlatform(8400f, 620f, 3000f, 60f),
            GamePlatform(11780f, 760f, 4200f, 74f),
            GamePlatform(16320f, 680f, 3000f, 62f),
            GamePlatform(19840f, 600f, 3400f, 58f),
            GamePlatform(23680f, 760f, 4200f, 74f),
            GamePlatform(28240f, 660f, 3400f, 60f),
            GamePlatform(32120f, 580f, 3600f, 58f),
            GamePlatform(36080f, 760f, 2900f, 92f),
        )
    else -> listOf(
            GamePlatform(0f, 760f, 520f, 72f),
            GamePlatform(610f, 700f, 270f, 48f),
            GamePlatform(960f, 620f, 260f, 48f),
            GamePlatform(1320f, 720f, 340f, 52f),
            GamePlatform(1760f, 650f, 320f, 52f),
            GamePlatform(2140f, 760f, 360f, 72f),
            GamePlatform(2580f, 690f, 300f, 48f),
            GamePlatform(3000f, 610f, 280f, 48f),
            GamePlatform(3400f, 700f, 330f, 52f),
            GamePlatform(3860f, 760f, 430f, 72f),
        )
}

private fun stageCoins(stage: Int): List<GameCoin> = when (stage) {
    2 -> listOf(
            GameCoin(595f, 600f),
            GameCoin(875f, 510f),
            GameCoin(1180f, 420f),
            GameCoin(1490f, 540f),
            GameCoin(1810f, 450f),
            GameCoin(2130f, 360f),
            GameCoin(2480f, 480f),
            GameCoin(2820f, 400f),
            GameCoin(3150f, 560f),
            GameCoin(3510f, 470f),
            GameCoin(3840f, 380f),
            GameCoin(4160f, 520f),
        )
    3 -> listOf(
            GameCoin(580f, 585f),
            GameCoin(865f, 465f),
            GameCoin(1185f, 555f),
            GameCoin(1480f, 405f),
            GameCoin(1810f, 505f),
            GameCoin(2130f, 365f),
            GameCoin(2470f, 465f),
            GameCoin(2810f, 335f),
            GameCoin(3150f, 485f),
            GameCoin(3490f, 395f),
            GameCoin(3840f, 535f),
            GameCoin(4200f, 415f),
            GameCoin(4540f, 565f),
        )
    4 -> listOf(
            GameCoin(720f, 650f),
            GameCoin(1120f, 590f),
            GameCoin(1240f, 590f),
            GameCoin(1540f, 660f),
            GameCoin(2030f, 560f),
            GameCoin(2430f, 640f),
            GameCoin(2860f, 540f),
            GameCoin(3300f, 650f),
            GameCoin(3760f, 570f),
            GameCoin(4240f, 710f),
        )
    5 -> listOf(
            GameCoin(690f, 650f),
            GameCoin(1060f, 570f),
            GameCoin(1420f, 490f),
            GameCoin(1770f, 610f),
            GameCoin(2160f, 530f),
            GameCoin(2520f, 420f),
            GameCoin(2920f, 560f),
            GameCoin(3350f, 470f),
            GameCoin(3730f, 600f),
            GameCoin(4170f, 510f),
            GameCoin(4560f, 420f),
        )
    6 -> listOf(
            GameCoin(720f, 700f),
            GameCoin(1180f, 700f),
            GameCoin(1700f, 700f),
            GameCoin(2240f, 700f),
            GameCoin(2780f, 700f),
            GameCoin(3320f, 700f),
            GameCoin(3880f, 700f),
            GameCoin(4460f, 700f),
        )
    7 -> emptyList()
    8 -> listOf(
            GameCoin(820f, 650f),
            GameCoin(980f, 650f),
            GameCoin(1140f, 650f),
            GameCoin(1420f, 570f),
            GameCoin(1560f, 570f),
            GameCoin(1700f, 570f),
            GameCoin(1840f, 570f),
            GameCoin(1980f, 570f),
            GameCoin(2120f, 570f),
            GameCoin(2260f, 570f),
            GameCoin(2260f, 760f),
            GameCoin(2400f, 760f),
            GameCoin(2540f, 760f),
            GameCoin(2680f, 760f),
            GameCoin(2820f, 760f),
            GameCoin(3160f, 570f),
            GameCoin(3400f, 570f),
            GameCoin(3640f, 570f),
            GameCoin(3880f, 570f),
            GameCoin(4500f, 700f),
        )
    9 -> listOf(
            GameCoin(760f, 700f), GameCoin(1420f, 700f), GameCoin(2080f, 700f), GameCoin(2740f, 700f), GameCoin(3400f, 700f),
            GameCoin(4060f, 700f), GameCoin(4720f, 700f), GameCoin(5840f, 640f), GameCoin(6480f, 640f), GameCoin(7120f, 640f),
            GameCoin(7760f, 640f), GameCoin(8680f, 560f), GameCoin(9400f, 560f), GameCoin(10120f, 560f), GameCoin(10840f, 560f),
            GameCoin(12040f, 700f), GameCoin(12880f, 700f), GameCoin(13720f, 700f), GameCoin(14560f, 700f), GameCoin(15400f, 700f),
            GameCoin(16640f, 620f), GameCoin(17520f, 620f), GameCoin(18400f, 620f), GameCoin(20120f, 540f), GameCoin(21080f, 540f),
            GameCoin(22040f, 540f), GameCoin(23000f, 540f), GameCoin(24020f, 700f), GameCoin(24920f, 700f), GameCoin(25820f, 700f),
            GameCoin(26720f, 700f), GameCoin(28620f, 600f), GameCoin(29560f, 600f), GameCoin(30500f, 600f), GameCoin(31440f, 600f),
            GameCoin(32440f, 520f), GameCoin(33420f, 520f), GameCoin(34400f, 520f), GameCoin(35380f, 520f), GameCoin(36680f, 700f),
            GameCoin(37480f, 700f), GameCoin(38280f, 700f),
        )
    else -> listOf(
            GameCoin(690f, 650f),
            GameCoin(1040f, 570f),
            GameCoin(1140f, 570f),
            GameCoin(1430f, 670f),
            GameCoin(1860f, 600f),
            GameCoin(1980f, 600f),
            GameCoin(2280f, 710f),
            GameCoin(2670f, 640f),
            GameCoin(3090f, 560f),
            GameCoin(3190f, 560f),
            GameCoin(3520f, 650f),
            GameCoin(4000f, 710f),
        )
}

private fun stageEnemies(stage: Int): List<GameEnemy> = when (stage) {
    2 -> listOf(
            GameEnemy(1445f, 1580f, 542f, 1445f),
            GameEnemy(2405f, 2570f, 482f, 2405f),
            GameEnemy(3445f, 3590f, 472f, 3445f),
            GameEnemy(4085f, 4250f, 522f, 4085f),
        )
    3 -> listOf(
            GameEnemy(1135f, 1270f, 562f, 1135f),
            GameEnemy(2390f, 2550f, 472f, 2390f),
            GameEnemy(3440f, 3570f, 402f, 3440f),
            GameEnemy(4455f, 4620f, 572f, 4455f),
        )
    4 -> listOf(
        )
    5 -> listOf(
            GameEnemy(1010f, 1170f, 562f, 1010f),
            GameEnemy(2100f, 2310f, 522f, 2100f),
            GameEnemy(3270f, 3490f, 462f, 3270f),
            GameEnemy(4100f, 4330f, 502f, 4100f),
        )
    6 -> listOf(
            GameEnemy(930f, 1150f, 706f, 930f),
            GameEnemy(1930f, 2140f, 706f, 1930f),
            GameEnemy(2860f, 3080f, 706f, 2860f),
            GameEnemy(3810f, 4040f, 706f, 3810f),
        )
    7 -> emptyList()
    8 -> listOf(
            GameEnemy(870f, 1160f, 642f, 870f),
            GameEnemy(1510f, 2060f, 562f, 1510f),
            GameEnemy(2220f, 2920f, 762f, 2220f),
            GameEnemy(3180f, 3960f, 562f, 3180f),
        )
    9 -> listOf(
            GameEnemy(920f, 1480f, 706f, 920f),
            GameEnemy(6120f, 7720f, 646f, 6120f),
            GameEnemy(9020f, 10720f, 566f, 9020f),
            GameEnemy(12460f, 15060f, 706f, 12460f),
            GameEnemy(17020f, 18720f, 626f, 17020f),
            GameEnemy(20480f, 22780f, 546f, 20480f),
            GameEnemy(24260f, 26960f, 706f, 24260f),
            GameEnemy(28840f, 31140f, 606f, 28840f),
            GameEnemy(32740f, 35140f, 526f, 32740f),
            GameEnemy(36760f, 38660f, 706f, 36760f),
        )
    else -> listOf(
            GameEnemy(1380f, 1600f, 672f, 1380f),
            GameEnemy(1810f, 2020f, 602f, 1810f),
            GameEnemy(2640f, 2830f, 642f, 2640f),
            GameEnemy(3440f, 3670f, 652f, 3440f),
        )
}

private fun stageTrees(stage: Int): List<GameTree> = when (stage) {
    6 -> listOf(
        GameTree(840f, 760f),
        GameTree(1560f, 760f),
        GameTree(2450f, 760f),
        GameTree(3360f, 760f),
        GameTree(4260f, 760f),
    )
    else -> emptyList()
}

@Composable
fun PlatformGameScreen(
    modifier: Modifier = Modifier,
) {
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    var jumpRequested by remember { mutableStateOf(false) }
    var jumpHeld by remember { mutableStateOf(false) }
    var jumpCharge by remember { mutableFloatStateOf(0f) }
    var previousJumpHeld by remember { mutableStateOf(false) }
    var tongueHeld by remember { mutableStateOf(false) }
    var tongueCharge by remember { mutableFloatStateOf(0f) }
    var previousTongueHeld by remember { mutableStateOf(false) }
    var playerX by remember { mutableFloatStateOf(80f) }
    var playerY by remember { mutableFloatStateOf(0f) }
    var velocityX by remember { mutableFloatStateOf(0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    var isGrounded by remember { mutableStateOf(false) }
    var facingDirection by remember { mutableFloatStateOf(1f) }
    var tongueRequested by remember { mutableStateOf(false) }
    var tongueTimer by remember { mutableFloatStateOf(0f) }
    var tonguePower by remember { mutableFloatStateOf(1f) }
    var superTongueActive by remember { mutableStateOf(false) }
    var mutationCharge by remember { mutableFloatStateOf(0f) }
    var isTransformed by remember { mutableStateOf(false) }
    var transformTimer by remember { mutableFloatStateOf(0f) }
    var heatRayTimer by remember { mutableFloatStateOf(0f) }
    var coreOrbClaimed by remember { mutableStateOf(false) }
    var transformRequested by remember { mutableStateOf(false) }
    var shieldHits by remember { mutableIntStateOf(0) }
    var rainbowTimer by remember { mutableFloatStateOf(0f) }
    var stage by remember { mutableIntStateOf(1) }
    var worldWidth by remember { mutableFloatStateOf(stageWorldWidth(1)) }
    var cameraX by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var eggAmmo by remember { mutableIntStateOf(0) }
    var bossHealth by remember { mutableIntStateOf(0) }
    var bossAttackTimer by remember { mutableFloatStateOf(0f) }
    var bossHitFlash by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }
    var giantBoss by remember { mutableStateOf<GiantBossState?>(null) }
    var stageEightDragonTimer by remember { mutableFloatStateOf(0f) }
    var stageEightLaserTimer by remember { mutableFloatStateOf(0f) }
    var stageEightDragonArmed by remember { mutableStateOf(false) }
    var stageNineDragonX by remember { mutableFloatStateOf(820f) }
    var warpMenuVisible by remember { mutableStateOf(false) }

    val platforms = remember { mutableStateListOf<GamePlatform>().apply { addAll(stagePlatforms(1)) } }
    val coins = remember {
        mutableStateListOf<GameCoin>().apply { addAll(stageCoins(1)) }
    }
    val enemies = remember {
        mutableStateListOf<GameEnemy>().apply { addAll(stageEnemies(1)) }
    }
    val eggShots = remember { mutableStateListOf<GameEggShot>() }
    val bossEggs = remember { mutableStateListOf<GameCoin>() }
    val bossAttackEggs = remember { mutableStateListOf<BossAttackEgg>() }
    val trees = remember { mutableStateListOf<GameTree>().apply { addAll(stageTrees(1)) } }
    val apples = remember { mutableStateListOf<GameApple>() }
    val infiniteTransition = rememberInfiniteTransition(label = "game-glow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coin-glow",
    )
    val motionPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "character-motion",
    )
    val tryAgainText = stringResource(com.scott.myclipboard.R.string.game_try_again_status)
    val ouchText = stringResource(com.scott.myclipboard.R.string.game_ouch_status)
    val clearText = stringResource(com.scott.myclipboard.R.string.game_clear_status)

    fun loadStage(nextStage: Int) {
        stage = nextStage
        worldWidth = stageWorldWidth(nextStage)
        playerX = 80f
        playerY = 0f
        velocityX = 0f
        velocityY = 0f
        isGrounded = false
        cameraX = 0f
        score = if (nextStage == 4) 20 else 0
        eggAmmo = 0
        bossHealth = if (nextStage == 4) 10 else 0
        bossAttackTimer = 1.4f
        bossHitFlash = 0f
        statusText = ""
        jumpRequested = false
        jumpHeld = false
        jumpCharge = 0f
        previousJumpHeld = false
        tongueRequested = false
        tongueHeld = false
        tongueCharge = 0f
        previousTongueHeld = false
        tongueTimer = 0f
        tonguePower = 1f
        superTongueActive = false
        mutationCharge = 0f
        isTransformed = false
        transformTimer = 0f
        heatRayTimer = 0f
        coreOrbClaimed = false
        transformRequested = false
        shieldHits = 0
        rainbowTimer = if (nextStage == 9) 9999f else 0f
        stageEightDragonTimer = 0f
        stageEightLaserTimer = 0f
        stageEightDragonArmed = nextStage == 8
        stageNineDragonX = if (nextStage == 9) 820f else stageNineDragonX
        platforms.clear()
        platforms.addAll(stagePlatforms(nextStage))
        coins.clear()
        coins.addAll(stageCoins(nextStage))
        enemies.clear()
        enemies.addAll(stageEnemies(nextStage))
        trees.clear()
        trees.addAll(stageTrees(nextStage))
        apples.clear()
        if (nextStage == 7) {
            apples.add(GameApple(420f, 420f, 700f, age = 6f))
            giantBoss = GiantBossState(x = worldWidth - 520f)
            rainbowTimer = 0f
        } else {
        giantBoss = null
        }
        eggShots.clear()
        bossEggs.clear()
        bossAttackEggs.clear()
        warpMenuVisible = false
    }

    fun resetGame() = loadStage(stage)

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (isActive) {
            withFrameNanosCompat { frameTime ->
                if (lastFrame == 0L) {
                    lastFrame = frameTime
                    return@withFrameNanosCompat
                }
                val rawDt = ((frameTime - lastFrame) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrame = frameTime

                val playerWidth = 58f
                val playerHeight = 78f
                val coreOrbX = worldWidth - 240f
                val coreOrbY = 560f
                val rainbowInvincible = rainbowTimer > 0f
                val transformEnabled = stage < 6
                val incomingBossEgg = stage == 4 && bossAttackEggs.any { egg ->
                    !egg.collected &&
                        !egg.landed &&
                        egg.warningTimer <= 0f &&
                        abs(bossAttackEggX(egg) - (playerX + playerWidth / 2f)) < 240f &&
                        abs(bossAttackEggY(egg) - (playerY + playerHeight / 2f)) < 190f
                }
                val dt = rawDt * if (incomingBossEgg) 0.32f else 1f
                if (warpMenuVisible) {
                    velocityX = 0f
                    velocityY = 0f
                    return@withFrameNanosCompat
                }
                if (incomingBossEgg) {
                    statusText = "먹기!"
                }
                val jumpReleasedThisFrame = previousJumpHeld && !jumpHeld
                val tongueReleasedThisFrame = previousTongueHeld && !tongueHeld
                val acceleration = 2200f
                val friction = if (isGrounded) 0.78f else 0.94f
                val gravity = 2350f
                val maxSpeed = if (rainbowInvincible) 620f else 430f
                var resetAfterEnemyCollision = false

                if (leftPressed) velocityX -= acceleration * dt
                if (rightPressed) velocityX += acceleration * dt
                if (!leftPressed && !rightPressed) velocityX *= friction
                velocityX = velocityX.coerceIn(-maxSpeed, maxSpeed)
                if (velocityX < -24f) facingDirection = -1f
                if (velocityX > 24f) facingDirection = 1f

                if (!transformEnabled) {
                    mutationCharge = 0f
                    isTransformed = false
                    transformTimer = 0f
                    heatRayTimer = 0f
                } else if (!isTransformed) {
                    mutationCharge = (mutationCharge + dt / 22f).coerceAtMost(1f)
                    if (mutationCharge >= 0.98f && statusText.isBlank()) {
                        statusText = "변신 가능!"
                    }
                    if (transformRequested && mutationCharge >= 0.98f) {
                        isTransformed = true
                        transformTimer = 12f
                        mutationCharge = 0f
                        tongueCharge = 0f
                        statusText = "고질라 변신!"
                    }
                } else {
                    transformTimer = max(0f, transformTimer - dt)
                    if (transformTimer <= 0f) {
                        isTransformed = false
                        mutationCharge = 0f
                        heatRayTimer = 0f
                        statusText = "변신 해제"
                    }
                }
                rainbowTimer = if (stage == 9) 9999f else max(0f, rainbowTimer - dt)
                if (stage == 8) {
                    val viewportScale = canvasHeight / 900f
                    val viewportWorldWidth = canvasWidth / viewportScale
                    val dragonWorldX = 2520f
                    if (stageEightDragonArmed && dragonWorldX - cameraX <= viewportWorldWidth + 120f) {
                        stageEightDragonArmed = false
                        stageEightDragonTimer = 2.8f
                        stageEightLaserTimer = 0.9f
                        statusText = "드래곤 레이저!"
                    }
                    stageEightDragonTimer = max(0f, stageEightDragonTimer - dt)
                    stageEightLaserTimer = max(0f, stageEightLaserTimer - dt)
                } else if (stage == 9) {
                    val chaseTargetX = min(worldWidth - 840f, playerX + 760f)
                    stageNineDragonX += (chaseTargetX - stageNineDragonX) * min(1f, dt * 2.4f)
                    if (statusText.isBlank()) {
                        statusText = "무지개 각성! 드래곤을 추격해!"
                    }
                }
                transformRequested = false

                giantBoss?.let { boss ->
                    if (stage == 7) {
                        boss.hitCooldown = max(0f, boss.hitCooldown - dt)
                        boss.shoveCooldown = max(0f, boss.shoveCooldown - dt)
                        if (boss.health > 0) {
                            boss.timer = max(0f, boss.timer - dt)
                            boss.pushCooldown = max(0f, boss.pushCooldown - dt)
                            if (boss.pushTimer > 0f) {
                                boss.pushTimer = max(0f, boss.pushTimer - dt)
                                val targetDirection = if (playerX + playerWidth / 2f >= boss.x) 1f else -1f
                                boss.direction = targetDirection
                                boss.x += boss.direction * 280f * dt
                                if (boss.pushTimer <= 0f) {
                                    boss.pushCooldown = 2.15f
                                }
                            } else {
                                boss.x += boss.direction * 110f * dt
                                if (boss.pushCooldown <= 0f) {
                                    boss.pushTimer = 0.78f
                                    boss.pushCooldown = 99f
                                    statusText = "밀어낸다!"
                                }
                            }
                            if (boss.x < 2480f || boss.x > worldWidth - 360f) {
                                boss.direction *= -1f
                                boss.x = boss.x.coerceIn(2480f, worldWidth - 360f)
                            }
                            if (boss.timer <= 0f) {
                                resetGame()
                                statusText = tryAgainText
                                return@withFrameNanosCompat
                            }
                        } else if (boss.deathTimer > 0f) {
                            boss.deathTimer = max(0f, boss.deathTimer - dt)
                            boss.deathFall += 240f * dt
                            boss.x += boss.direction * 44f * dt
                            if (boss.deathTimer <= 0f) {
                                statusText = clearText
                            }
                        }
                    }
                }

                if (stage >= 4 && jumpHeld && isGrounded) {
                    jumpCharge = (jumpCharge + dt / 0.7f).coerceAtMost(1f)
                    if (jumpCharge >= 0.98f) {
                        statusText = "슈퍼 점프 준비!"
                    }
                } else if (stage >= 4 && jumpReleasedThisFrame && jumpCharge > 0f) {
                    val jumpPower = 860f + 520f * jumpCharge
                    velocityY = -jumpPower
                    isGrounded = false
                    statusText = if (jumpCharge >= 0.98f) "슈퍼 점프!" else ""
                    jumpCharge = 0f
                } else if (stage < 4 || (!jumpHeld && !jumpReleasedThisFrame)) {
                    jumpCharge = 0f
                }

                if (jumpRequested) {
                    if (stage >= 4 && isGrounded) {
                        // Late-game stages charge jump while the button is held and launch on release.
                    } else if (isGrounded) {
                        velocityY = -860f
                        isGrounded = false
                    } else if (stage >= 4 && eggAmmo > 0) {
                        eggAmmo -= 1
                        eggShots.add(
                            GameEggShot(
                                x = playerX + if (facingDirection > 0f) playerWidth + 8f else -8f,
                                y = playerY + 28f,
                                velocityX = facingDirection * 760f,
                                velocityY = -160f,
                            )
                        )
                        statusText = "Egg throw"
                    }
                }
                jumpRequested = false
                previousJumpHeld = jumpHeld
                val canChargeTongue = !isTransformed && mutationCharge < 0.98f
                if (canChargeTongue && tongueHeld) {
                    tongueCharge = (tongueCharge + dt / 0.45f).coerceAtMost(1f)
                    if (tongueCharge >= 0.98f) {
                        statusText = "슈퍼 먹기 준비!"
                    }
                } else if (canChargeTongue && tongueReleasedThisFrame) {
                    tongueRequested = true
                } else if (!tongueHeld && !tongueReleasedThisFrame) {
                    tongueCharge = 0f
                }
                val eatPressedThisFrame = tongueRequested
                if (tongueRequested) {
                    if (isTransformed) {
                        heatRayTimer = 0.24f
                        statusText = "방사열선!"
                    } else {
                        superTongueActive = tongueCharge >= 0.98f
                        tonguePower = if (superTongueActive) 1.9f else 1f
                        tongueTimer = if (superTongueActive) 0.28f else 0.18f
                        if (superTongueActive) {
                            statusText = "슈퍼 먹기!"
                        }
                        tongueCharge = 0f
                    }
                }
                tongueRequested = false
                previousTongueHeld = tongueHeld
                tongueTimer = max(0f, tongueTimer - dt)
                heatRayTimer = max(0f, heatRayTimer - dt)
                if (tongueTimer <= 0f) {
                    superTongueActive = false
                    tonguePower = 1f
                }

                val previousBottom = playerY + playerHeight
                playerX = (playerX + velocityX * dt).coerceIn(12f, worldWidth - playerWidth - 12f)
                playerY += velocityY * dt
                velocityY += gravity * dt
                isGrounded = false

                platforms.forEach { platform ->
                    val playerRight = playerX + playerWidth
                    val playerBottom = playerY + playerHeight
                    val overlapsX = playerRight > platform.x && playerX < platform.x + platform.width
                    val crossedTop = previousBottom <= platform.y && playerBottom >= platform.y
                    if (overlapsX && crossedTop && velocityY >= 0f) {
                        playerY = platform.y - playerHeight
                        velocityY = 0f
                        isGrounded = true
                    }
                }

                if (playerY > 940f) {
                    if (isStageOneWarpZone(stage, playerX)) {
                        warpMenuVisible = true
                        statusText = "워프할 스테이지를 골라라!"
                        velocityX = 0f
                        velocityY = 0f
                    } else {
                        resetGame()
                        statusText = tryAgainText
                    }
                }

                if (stage == 5 && !coreOrbClaimed) {
                    val centerX = playerX + playerWidth / 2f
                    val centerY = playerY + playerHeight / 2f
                    if (abs(centerX - coreOrbX) < 60f && abs(centerY - coreOrbY) < 92f) {
                        coreOrbClaimed = true
                        coreOrbClaimed = true
                        mutationCharge = 1f
                        statusText = "코어 확보!"
                    }
                }

                coins.forEach { coin ->
                    if (!coin.collected) {
                        val centerX = playerX + playerWidth / 2f
                        val centerY = playerY + playerHeight / 2f
                        if (abs(centerX - coin.x) < 52f && abs(centerY - coin.y) < 62f) {
                            coin.collected = true
                            score += 1
                            if (stage >= 4) {
                                eggAmmo += 1
                            }
                        }
                    }
                }
                apples.forEach { apple ->
                    if (!apple.collected) {
                        apple.age += dt
                        val centerX = playerX + playerWidth / 2f
                        val centerY = playerY + playerHeight / 2f
                        if (abs(centerX - apple.x) < 52f && abs(centerY - apple.y) < 62f) {
                            apple.collected = true
                            if (apple.age >= 5f) {
                                rainbowTimer = 11f
                                shieldHits = 0
                                statusText = "무지개 황금사과!"
                            } else {
                                shieldHits = max(shieldHits, 1)
                                statusText = "사과 방어막!"
                            }
                        }
                    }
                }
                bossEggs.forEach { egg ->
                    if (!egg.collected) {
                        val centerX = playerX + playerWidth / 2f
                        val centerY = playerY + playerHeight / 2f
                        if (abs(centerX - egg.x) < 52f && abs(centerY - egg.y) < 62f) {
                            egg.collected = true
                            eggAmmo += 1
                        }
                    }
                }
                bossAttackEggs.forEach { egg ->
                    val currentX = bossAttackEggX(egg)
                    val currentY = bossAttackEggY(egg)
                    if (egg.warningTimer > 0f) {
                        egg.warningTimer -= dt
                    } else if (!egg.landed) {
                        egg.progress = (egg.progress + dt / 1.55f).coerceAtMost(1f)
                        val catchable = stage == 4 &&
                            (eatPressedThisFrame || superTongueActive) &&
                            !egg.collected &&
                            abs(currentX - (playerX + playerWidth / 2f)) < (if (superTongueActive) 340f else 220f) &&
                            abs(currentY - (playerY + playerHeight / 2f)) < (if (superTongueActive) 220f else 170f)
                        val rayCatchable = isTransformed &&
                            heatRayTimer > 0f &&
                            abs(currentX - (playerX + playerWidth / 2f)) < 420f &&
                            abs(currentY - (playerY + playerHeight / 2f)) < 220f
                        if (catchable || rayCatchable) {
                            egg.collected = true
                            eggShots.add(
                                GameEggShot(
                                    x = playerX + playerWidth + 10f,
                                    y = playerY + 26f,
                                    velocityX = 860f,
                                    velocityY = -210f,
                                    homesToBoss = true,
                                )
                            )
                            statusText = "반격!"
                            return@forEach
                        }
                        val hitPlayer = Rect(currentX - 18f, currentY - 20f, currentX + 18f, currentY + 20f)
                            .overlaps(Rect(playerX, playerY, playerX + playerWidth, playerY + playerHeight))
                        if (hitPlayer && !isTransformed && !rainbowInvincible) {
                            if (shieldHits > 0) {
                                shieldHits -= 1
                                egg.collected = true
                                statusText = "방어막!"
                            } else {
                                resetAfterEnemyCollision = true
                            }
                        }
                        if (egg.progress >= 1f) {
                            egg.landed = true
                        }
                    } else if (!egg.collected) {
                        val centerX = playerX + playerWidth / 2f
                        val centerY = playerY + playerHeight / 2f
                        if (stage == 4 && ((eatPressedThisFrame || superTongueActive) && abs(centerX - currentX) < (if (superTongueActive) 340f else 220f) && abs(centerY - currentY) < (if (superTongueActive) 220f else 170f) || (isTransformed && heatRayTimer > 0f && abs(centerX - currentX) < 420f && abs(centerY - currentY) < 220f))) {
                            egg.collected = true
                            eggShots.add(
                                GameEggShot(
                                    x = playerX + playerWidth + 10f,
                                    y = playerY + 26f,
                                    velocityX = 860f,
                                    velocityY = -210f,
                                    homesToBoss = true,
                                )
                            )
                            statusText = "반격!"
                            return@forEach
                        }
                        if (abs(centerX - currentX) < 52f && abs(centerY - currentY) < 62f) {
                            egg.collected = true
                            eggAmmo += 1
                        }
                    }
                }
                if (stage == 4 && bossHealth > 0) {
                    bossAttackTimer -= dt
                    bossHitFlash = max(0f, bossHitFlash - dt)
                    if (bossAttackTimer <= 0f) {
                        val bossX = worldWidth - 360f
                        bossAttackEggs.add(
                            BossAttackEgg(
                                startX = bossX - 40f,
                                startY = 520f,
                                targetX = (playerX + if ((bossAttackEggs.size % 2) == 0) 40f else -30f)
                                    .coerceIn(cameraX + 120f, cameraX + canvasWidth / (canvasHeight / 900f) - 120f),
                                targetY = 735f,
                            )
                        )
                        bossAttackEggs.removeAll { it.collected || it.targetX < cameraX - 160f }
                        bossAttackTimer = 2.35f
                        statusText = "궤적을 피해!"
                    }
                }
                if (resetAfterEnemyCollision) {
                    resetGame()
                    statusText = ouchText
                    return@withFrameNanosCompat
                }

                eggShots.forEach { shot ->
                    if (stage == 4 && bossHealth > 0 && shot.homesToBoss) {
                        val bossX = worldWidth - 360f
                        val targetX = bossX
                        val targetY = 560f
                        val dx = targetX - shot.x
                        val dy = targetY - shot.y
                        val distance = max(1f, kotlin.math.hypot(dx, dy))
                        shot.velocityX = dx / distance * 920f
                        shot.velocityY = dy / distance * 920f
                    }
                    shot.x += shot.velocityX * dt
                    shot.y += shot.velocityY * dt
                    if (!shot.homesToBoss) {
                        shot.velocityY += 820f * dt
                    }
                }
                val spentShots = mutableListOf<GameEggShot>()
                if (stage == 4 && bossHealth > 0) {
                    val bossX = worldWidth - 360f
                    val bossRect = Rect(bossX - 78f, 500f, bossX + 78f, 690f)
                    eggShots.forEach { shot ->
                        val hitBoss = Rect(shot.x - 16f, shot.y - 18f, shot.x + 16f, shot.y + 18f)
                            .overlaps(bossRect)
                        if (hitBoss && shot !in spentShots) {
                            spentShots.add(shot)
                            bossHealth = max(0, bossHealth - 1)
                            bossHitFlash = 0.18f
                            statusText = if (bossHealth > 0) "Boss HP: $bossHealth" else clearText
                        }
                    }
                }

                enemies.forEach { enemy ->
                    if (enemy.defeated) {
                        if (enemy.respawnDelay > 0f) {
                            enemy.respawnDelay -= dt
                            return@forEach
                        }
                        val enemyScreenX = (enemy.x - cameraX) * (canvasHeight / 900f)
                        if (enemyScreenX < -90f || enemyScreenX > canvasWidth + 90f) {
                            enemy.defeated = false
                            enemy.eatenProgress = 0f
                            enemy.grabbed = false
                            enemy.thrown = false
                            enemy.spinTaps = 0
                            enemy.respawnDelay = 0f
                            enemy.x = if (enemy.x < cameraX) enemy.startX else enemy.endX
                            enemy.y = enemy.eatenFromY
                            enemy.direction = if (enemy.x < cameraX) 1f else -1f
                        }
                        return@forEach
                    }
                    if (enemy.thrown) {
                        enemy.x += enemy.throwVelocityX * dt
                        enemy.y += enemy.throwVelocityY * dt
                        enemy.throwVelocityY += 520f * dt
                        val enemyScreenX = (enemy.x - cameraX) * (canvasHeight / 900f)
                        if (enemyScreenX < -120f || enemyScreenX > canvasWidth + 120f || enemy.y > 960f) {
                            enemy.defeated = true
                            enemy.thrown = false
                            enemy.respawnDelay = 1.6f
                        }
                        return@forEach
                    }
                    if (enemy.grabbed) {
                        if (eatPressedThisFrame) {
                            enemy.spinTaps += 1
                        }
                        if (enemy.spinTaps == 0) {
                            enemy.x = playerX + if (facingDirection > 0f) 82f else -28f
                            enemy.y = playerY + 24f
                        } else {
                            enemy.spinAngle += (8.5f + enemy.spinTaps * 2.6f) * dt
                            val orbitRadius = (48f + min(enemy.spinTaps, 3) * 10f)
                            enemy.x = playerX + 28f + cos(enemy.spinAngle) * orbitRadius
                            enemy.y = playerY + 28f + sin(enemy.spinAngle) * orbitRadius
                        }
                        if (enemy.spinTaps >= 3) {
                            enemy.grabbed = false
                            enemy.thrown = true
                            enemy.throwVelocityX = facingDirection * 900f
                            enemy.throwVelocityY = -260f
                        }
                        return@forEach
                    }
                    if (enemy.eatenProgress > 0f) {
                        if (stage >= 3 && eatPressedThisFrame) {
                            enemy.eatenProgress = 0f
                            enemy.grabbed = true
                            enemy.spinTaps = 1
                            enemy.spinAngle = 0f
                            enemy.thrown = false
                            statusText = "연타!"
                            return@forEach
                        }
                        enemy.eatenProgress += dt / 0.24f
                        if (enemy.eatenProgress >= 1f) {
                            enemy.defeated = true
                            enemy.eatenProgress = 0f
                        }
                        return@forEach
                    }
                    eggShots.forEach { shot ->
                        val hitByEgg = Rect(shot.x - 16f, shot.y - 18f, shot.x + 16f, shot.y + 18f)
                            .overlaps(Rect(enemy.x, enemy.y, enemy.x + 54f, enemy.y + 54f))
                        if (hitByEgg) {
                            enemy.defeated = true
                            enemy.respawnDelay = 1.0f
                            spentShots.add(shot)
                            score += 2
                        }
                    }
                    enemy.x += enemy.direction * 130f * dt
                    if (enemy.x < enemy.startX || enemy.x > enemy.endX) {
                        enemy.direction *= -1f
                        enemy.x = enemy.x.coerceIn(enemy.startX, enemy.endX)
                    }
                    val heatRayRect = if (isTransformed && heatRayTimer > 0f) {
                        val mouthX = playerX + if (facingDirection > 0f) playerWidth - 4f else 4f
                        Rect(
                            left = if (facingDirection > 0f) mouthX else mouthX - 360f,
                            top = playerY + 6f,
                            right = if (facingDirection > 0f) mouthX + 360f else mouthX,
                            bottom = playerY + 50f,
                        )
                    } else {
                        Rect.Zero
                    }
                    val tongueRect = if (tongueTimer > 0f) {
                        val mouthX = playerX + if (facingDirection > 0f) playerWidth - 8f else 8f
                        val tongueReach = 120f * tonguePower
                        Rect(
                            left = if (facingDirection > 0f) mouthX else mouthX - tongueReach,
                            top = playerY + 18f,
                            right = if (facingDirection > 0f) mouthX + tongueReach else mouthX,
                            bottom = playerY + 42f,
                        )
                    } else {
                        Rect.Zero
                    }
                    if (tongueTimer > 0f && tongueRect.overlaps(Rect(enemy.x, enemy.y, enemy.x + 54f, enemy.y + 54f))) {
                        enemy.eatenProgress = 0.01f
                        enemy.eatenFromX = enemy.x
                        enemy.eatenFromY = enemy.y
                        score += 2
                        velocityY = min(velocityY, -180f)
                        return@forEach
                    }
                    if (heatRayTimer > 0f && heatRayRect.overlaps(Rect(enemy.x, enemy.y, enemy.x + 54f, enemy.y + 54f))) {
                        enemy.defeated = true
                        enemy.respawnDelay = 1.2f
                        score += 2
                        return@forEach
                    }
                    val hitEnemy = Rect(playerX, playerY, playerX + playerWidth, playerY + playerHeight)
                        .overlaps(Rect(enemy.x, enemy.y, enemy.x + 54f, enemy.y + 54f))
                    if (hitEnemy) {
                        if (rainbowInvincible) {
                            enemy.defeated = true
                            enemy.respawnDelay = 1.2f
                            score += 2
                            return@forEach
                        }
                        if (velocityY > 120f && playerY + playerHeight - enemy.y < 36f) {
                            enemy.defeated = true
                            if (stage < 3) {
                                velocityY = -520f
                            }
                            score += 2
                        } else if (!isTransformed) {
                            if (shieldHits > 0) {
                                shieldHits -= 1
                                enemy.defeated = true
                                enemy.respawnDelay = 1.2f
                                statusText = "방어막!"
                                return@forEach
                            }
                            resetAfterEnemyCollision = true
                            return@forEach
                        }
                    }
                }
                giantBoss?.let { boss ->
                    if (stage == 7 && boss.health > 0) {
                        val bossRect = Rect(boss.x - 120f, 520f, boss.x + 120f, 760f)
                        val playerRect = Rect(playerX, playerY, playerX + playerWidth, playerY + playerHeight)
                        if (bossRect.overlaps(playerRect)) {
                            if (rainbowInvincible && boss.hitCooldown <= 0f) {
                                boss.health = max(0, boss.health - 1)
                                boss.hitCooldown = 0.7f
                                velocityX = -boss.direction * 320f
                                velocityY = min(velocityY, -180f)
                                if (boss.health > 0) {
                                    statusText = "Boss HP: ${boss.health}/3"
                                } else {
                                    boss.deathTimer = 1.6f
                                    boss.deathFall = 0f
                                    statusText = "거대 공룡 다운!"
                                }
                            } else if (boss.shoveCooldown <= 0f) {
                                val awayDirection = if (playerX + playerWidth / 2f >= boss.x) 1f else -1f
                                velocityX = awayDirection * 540f
                                velocityY = min(velocityY, -120f)
                                boss.shoveCooldown = 0.6f
                                statusText = "밀려났다!"
                            }
                        }
                    }
                }
                trees.forEach { tree ->
                    if (!tree.appleSpawned && tongueTimer > 0f) {
                        val mouthX = playerX + if (facingDirection > 0f) playerWidth - 8f else 8f
                        val tongueReach = 120f * tonguePower
                        val tongueRect = Rect(
                            left = if (facingDirection > 0f) mouthX else mouthX - tongueReach,
                            top = playerY + 18f,
                            right = if (facingDirection > 0f) mouthX + tongueReach else mouthX,
                            bottom = playerY + 52f,
                        )
                        val treeRect = Rect(tree.x - 34f, tree.y - 160f, tree.x + 34f, tree.y)
                        if (tongueRect.overlaps(treeRect)) {
                            tree.appleSpawned = true
                            apples.add(GameApple(tree.x, tree.x, tree.y - 30f))
                            statusText = "사과!"
                        }
                    }
                }
                eggShots.removeAll { shot ->
                    shot in spentShots ||
                        shot.x < cameraX - 80f ||
                        shot.x > cameraX + canvasWidth / (canvasHeight / 900f) + 120f ||
                        shot.y > 940f
                }

                if (resetAfterEnemyCollision) {
                    resetGame()
                    statusText = ouchText
                    return@withFrameNanosCompat
                }

                val goalX = stageGoalX(stage, worldWidth)
                apples.removeAll { it.collected }
                val bossDefeated = giantBoss?.health == 0 && (giantBoss?.deathTimer ?: 0f) <= 0f
                val isNearGoal = playerX > goalX - 200f
                val hasEnoughScoreToAdvance = score >= 20
                val stageObjectiveCleared =
                    coins.all { it.collected } ||
                        (stage == 4 && bossHealth <= 0) ||
                        (stage in 5..6) ||
                        (stage == 8) ||
                        (stage == 7 && bossDefeated)
                if (stage in 1..6 && isNearGoal && !hasEnoughScoreToAdvance) {
                    statusText = "20점수를 모으지못했군! 20점수되어야 스테이지를넘길수있다! 크하하! 다시버튼을 누르고다시해라!"
                } else if (stage == 7 && bossDefeated && !isNearGoal) {
                    statusText = clearText
                } else if (playerX > goalX - 70f && stageObjectiveCleared && (stage == 7 || hasEnoughScoreToAdvance)) {
                    if (stage == 1) {
                        loadStage(2)
                        statusText = "Stage 2"
                    } else if (stage == 2) {
                        loadStage(3)
                        statusText = "Stage 3"
                    } else if (stage == 3) {
                        loadStage(4)
                        statusText = "Stage 4"
                    } else if (stage == 4) {
                        loadStage(5)
                        statusText = "Stage 5"
                    } else if (stage == 5) {
                        loadStage(6)
                        statusText = "Stage 6"
                    } else if (stage == 6) {
                        loadStage(7)
                        statusText = "Stage 7"
                    } else if (stage == 7) {
                        loadStage(8)
                        statusText = "Stage 8"
                    } else if (stage == 8) {
                        loadStage(9)
                        statusText = "Stage 9"
                    } else {
                        statusText = clearText
                    }
                }

                val viewportScale = canvasHeight / 900f
                val maxCamera = max(0f, worldWidth - canvasWidth / viewportScale)
                cameraX = (playerX - 80f).coerceIn(0f, maxCamera)
            }
        }

    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF071522)),
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            canvasWidth = size.width
            canvasHeight = size.height
            val scale = size.height / 900f
            worldWidth = stageWorldWidth(stage)
            if (stage == 3) {
                drawSpace(glow, cameraX, scale)
            } else if (stage == 9) {
                drawRainbowRoadSky(glow, cameraX, scale)
            } else if (stage == 8) {
                drawStormPacific(glow, cameraX, scale)
            } else if (stage >= 6) {
                drawMeadow(glow, cameraX, scale)
            } else if (stage == 5) {
                drawReactorCore(glow, cameraX, scale)
            } else if (stage == 4) {
                drawFlowerKingdom(glow, cameraX, scale)
            } else {
                drawModernSky(glow)
                drawParallax(cameraX, scale)
            }
            platforms.forEach { drawPlatform(it, cameraX, scale, stage) }
            trees.forEach { drawTree(it, cameraX, scale) }
            coins.filterNot { it.collected }.forEach { drawCoin(it, cameraX, scale, glow) }
            apples.filterNot { it.collected }.forEach { drawApple(it, cameraX, scale, glow) }
            bossEggs.filterNot { it.collected }.forEach { drawCoin(it, cameraX, scale, glow) }
            bossAttackEggs.filterNot { it.collected }.forEach { drawBossAttackEgg(it, cameraX, scale, glow) }
            eggShots.forEach { drawEggShot(it, cameraX, scale, glow) }
            enemies.forEach { enemy ->
                if ((!enemy.defeated || enemy.eatenProgress > 0f) && enemy.x <= enemy.endX + 260f) {
                    drawEnemy(
                        enemy = enemy,
                        cameraX = cameraX,
                        scale = scale,
                        motionPhase = motionPhase,
                        playerX = playerX,
                        playerY = playerY,
                        facingDirection = facingDirection,
                    )
                }
            }
            if (stage == 8) {
                drawGoldenDragon(cameraX, scale, glow, stageEightDragonTimer, stageEightLaserTimer > 0f)
            } else if (stage == 9) {
                drawFinalGoldenDragon(stageNineDragonX, cameraX, scale, glow, motionPhase)
            }
            drawGoal(stageGoalX(stage, worldWidth), stageGoalBaseY(stage), cameraX, scale, glow)
            if (stage in 1..6 && playerX > stageGoalX(stage, worldWidth) - 200f && score < 20) {
                drawTauntBat(
                    goalX = stageGoalX(stage, worldWidth) - 80f,
                    cameraX = cameraX,
                    scale = scale,
                    glow = glow,
                    message = "20점수를 모으지못했군!\n20점수되어야 스테이지를넘길수있다!\n크하하!\n다시버튼을 누르고다시해라!",
                )
            }
            if (stage == 5 && !coreOrbClaimed) {
                drawCoreOrb(worldWidth - 240f, 560f, cameraX, scale, glow)
            }
            if (stage == 7) {
                giantBoss?.let { drawGiantBoss(it, cameraX, scale, motionPhase) }
            }
            if (stage == 4 && bossHealth > 0) {
                drawFlowerBoss(worldWidth - 360f, cameraX, scale, motionPhase, bossHealth, bossHitFlash)
            }
            drawPlayer(
                playerX = playerX,
                playerY = playerY,
                cameraX = cameraX,
                scale = scale,
                facingDirection = facingDirection,
                tongueTimer = tongueTimer,
                tonguePower = tonguePower,
                isTransformed = isTransformed,
                heatRayTimer = heatRayTimer,
                rainbowTimer = rainbowTimer,
                runSpeed = abs(velocityX),
                isGrounded = isGrounded,
                verticalVelocity = velocityY,
                motionPhase = motionPhase,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color(0xB80B1726),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(com.scott.myclipboard.R.string.game_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = stringResource(com.scott.myclipboard.R.string.game_score, score),
                    color = Color(0xFFE6F6FF),
                    fontSize = 13.sp,
                )
                Text(
                    text = if (stage == 9) "Rainbow INF" else if (rainbowTimer > 0f) "Rainbow ${(rainbowTimer * 10f).toInt() / 10f}s" else "Shield: $shieldHits",
                    color = Color(0xFFE6F6FF),
                    fontSize = 13.sp,
                )
                if (stage == 7) {
                    Text(
                        text = "Boss: ${giantBoss?.health ?: 0}/3  Time: ${((giantBoss?.timer ?: 0f) * 10f).toInt() / 10f}s",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                }
                if (stage < 6) {
                    Text(
                        text = if (isTransformed) "Form: Godzilla ${(transformTimer * 10f).toInt() / 10f}s" else if (mutationCharge >= 0.98f) "Form: READY" else "Form: ${(mutationCharge * 100f).toInt()}%",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(8.dp)
                            .background(Color(0x403A516A), RoundedCornerShape(999.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((if (isTransformed) transformTimer / 12f else mutationCharge).coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(
                                    if (isTransformed) Color(0xFF66C7FF) else Color(0xFF2F915F),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }
                if (stage >= 4) {
                    Text(
                        text = "Ammo: $eggAmmo",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Boss: $bossHealth/10",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Text(
                        text = if (jumpCharge >= 0.98f) "Jump: MAX" else "Jump: ${(jumpCharge * 100f).toInt()}%",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(8.dp)
                            .background(Color(0x403A516A), RoundedCornerShape(999.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(jumpCharge.coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(
                                    if (jumpCharge >= 0.98f) Color(0xFFFFD166) else Color(0xFFE85D75),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }
                run {
                    Text(
                        text = if (tongueCharge >= 0.98f) "Tongue: MAX" else "Tongue: ${(tongueCharge * 100f).toInt()}%",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(8.dp)
                            .background(Color(0x403A516A), RoundedCornerShape(999.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(tongueCharge.coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(
                                    if (tongueCharge >= 0.98f) Color(0xFF39A66B) else Color(0xFF7FDCA1),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }
                Text(
                    text = "Stage $stage",
                    color = Color(0xFFE6F6FF),
                    fontSize = 13.sp,
                )
                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        color = Color(0xFFFFD166),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                HoldButton(
                    label = "<",
                    onPressedChange = { leftPressed = it },
                )
                Spacer(modifier = Modifier.width(10.dp))
                HoldButton(
                    label = ">",
                    onPressedChange = { rightPressed = it },
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC243B53)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(com.scott.myclipboard.R.string.game_reset_action))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (stage < 6) {
                        Button(
                            onClick = { transformRequested = true },
                            modifier = Modifier.size(width = 82.dp, height = 58.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D89D6)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "변신",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    HoldActionButton(
                        label = if (isTransformed) "열선" else stringResource(com.scott.myclipboard.R.string.game_tongue_action),
                        modifier = Modifier.size(width = 82.dp, height = 58.dp),
                        containerColor = if (isTransformed) Color(0xFF3D89D6) else Color(0xFF39A66B),
                        onPress = {
                            if (isTransformed) {
                                tongueRequested = true
                            } else {
                                tongueHeld = true
                            }
                        },
                        onRelease = {
                            tongueHeld = false
                        },
                    )
                    HoldActionButton(
                        label = stringResource(com.scott.myclipboard.R.string.game_jump_action),
                        modifier = Modifier.size(width = 82.dp, height = 58.dp),
                        containerColor = Color(0xFFE85D75),
                        onPress = {
                            jumpRequested = true
                            jumpHeld = true
                        },
                        onRelease = { jumpHeld = false },
                    )
                }
            }
        }

        if (warpMenuVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp),
                color = Color(0xEE0C1727),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "비밀 워프",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "어느 스테이지로 갈지 골라줘",
                        color = Color(0xFFE6F6FF),
                        fontSize = 13.sp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(1..4, 5..8, 9..9).forEach { rowStages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowStages.forEach { targetStage ->
                                    Button(
                                        onClick = {
                                            loadStage(targetStage)
                                            statusText = "비밀 워프! Stage $targetStage"
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A4965)),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Stage $targetStage")
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            warpMenuVisible = false
                            resetGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A3140)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("취소")
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldButton(
    label: String,
    onPressedChange: (Boolean) -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose { onPressedChange(false) }
    }
    Surface(
        modifier = Modifier
            .size(64.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onPressedChange(event.changes.any { it.pressed })
                    }
                }
            },
        color = Color(0xCC16283D),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun HoldActionButton(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose { onRelease() }
    }
    Surface(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                var wasPressed = false
                while (true) {
                    val event = awaitPointerEvent()
                    val isPressed = event.changes.any { it.pressed }
                    if (isPressed && !wasPressed) {
                        onPress()
                    } else if (!isPressed && wasPressed) {
                        onRelease()
                    }
                    wasPressed = isPressed
                }
            }
        },
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private suspend inline fun withFrameNanosCompat(crossinline onFrame: (Long) -> Unit) {
    androidx.compose.runtime.withFrameNanos { onFrame(it) }
}

private fun DrawScope.drawModernSky(glow: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF55C7F7),
                Color(0xFF84DCCF),
                Color(0xFFF8E9A1),
            ),
        ),
    )
    drawCircle(
        color = Color(0xFFFFF5B8).copy(alpha = 0.62f + glow * 0.14f),
        radius = size.minDimension * 0.16f,
        center = Offset(size.width * 0.78f, size.height * 0.18f),
    )
}

private fun DrawScope.drawSpace(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070817),
                Color(0xFF171039),
                Color(0xFF2A1457),
            ),
        ),
    )
    repeat(38) { index ->
        val x = ((index * 137f - cameraX * 0.18f * scale) % (size.width + 80f)) - 40f
        val y = 36f + ((index * 83f) % (size.height * 0.74f))
        val radius = (1.4f + (index % 4) * 0.75f) * scale
        drawCircle(
            color = Color.White.copy(alpha = 0.42f + glow * 0.28f),
            radius = radius,
            center = Offset(x, y),
        )
    }
    drawCircle(
        color = Color(0xFF8DDBFF).copy(alpha = 0.28f),
        radius = 92f * scale,
        center = Offset(size.width * 0.78f, size.height * 0.18f),
    )
    drawCircle(
        color = Color(0xFFB695FF).copy(alpha = 0.52f),
        radius = 62f * scale,
        center = Offset(size.width * 0.78f, size.height * 0.18f),
    )
    drawOval(
        color = Color(0x88F6D365),
        topLeft = Offset(size.width * 0.78f - 92f * scale, size.height * 0.18f - 18f * scale),
        size = Size(184f * scale, 36f * scale),
        style = Stroke(width = 5f * scale),
    )
}

private fun DrawScope.drawFlowerKingdom(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF7DD7FF),
                Color(0xFFFFD1E8),
                Color(0xFFFFF0A6),
            ),
        ),
    )
    drawCircle(
        color = Color(0xFFFFF5B8).copy(alpha = 0.7f + glow * 0.1f),
        radius = size.minDimension * 0.13f,
        center = Offset(size.width * 0.8f, size.height * 0.17f),
    )
    val hillShift = (cameraX * 0.12f * scale) % (size.width + 360f)
    repeat(4) { index ->
        drawOval(
            color = Color(0x6652B86E),
            topLeft = Offset(index * 360f - hillShift, size.height * 0.62f),
            size = Size(360f, 220f),
        )
    }
    val flowerShift = (cameraX * 0.35f * scale) % (size.width + 180f)
    repeat(9) { index ->
        val x = index * 145f - flowerShift
        val y = size.height * 0.72f + (index % 2) * 30f
        drawLine(Color(0xFF328E45), Offset(x, y + 30f), Offset(x, y + 70f), strokeWidth = 4f * scale)
        repeat(6) { petal ->
            val angle = petal * 1.047f
            drawCircle(
                color = Color(0xFFE85D75),
                radius = 11f * scale,
                center = Offset(x + cos(angle) * 15f * scale, y + sin(angle) * 15f * scale),
            )
        }
        drawCircle(Color(0xFFFFD166), 9f * scale, Offset(x, y))
    }
}

private fun DrawScope.drawMeadow(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFB7E47C), Color(0xFF8FD36E), Color(0xFF74B85A)),
        ),
    )
    val hillShift = (cameraX * 0.08f * scale) % (size.width + 260f)
    repeat(5) { index ->
        drawOval(
            color = Color(0x554E9648),
            topLeft = Offset(index * 280f - hillShift, size.height * 0.62f),
            size = Size(280f, 180f),
        )
    }
}

private fun DrawScope.drawReactorCore(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF08131D), Color(0xFF173040), Color(0xFF0C1017)),
        ),
    )
    repeat(7) { index ->
        val x = (index * 220f - (cameraX * 0.22f * scale) % (size.width + 220f))
        drawRect(
            color = Color(0x2237B4D6),
            topLeft = Offset(x, size.height * 0.1f),
            size = Size(26f * scale, size.height * 0.75f),
        )
    }
    drawCircle(
        color = Color(0x334EDBFF).copy(alpha = 0.24f + glow * 0.12f),
        radius = size.minDimension * 0.18f,
        center = Offset(size.width * 0.82f, size.height * 0.24f),
    )
    repeat(4) { ring ->
        drawCircle(
            color = Color(0x664EDBFF),
            radius = (70f + ring * 28f) * scale,
            center = Offset(size.width * 0.82f, size.height * 0.24f),
            style = Stroke(width = 3f * scale),
        )
    }
}

private fun DrawScope.drawStormPacific(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF18304F), Color(0xFF21597B), Color(0xFF5B8DB0)),
        ),
    )
    val cycloneCenter = Offset(size.width * 0.74f, size.height * 0.2f)
    repeat(5) { ring ->
        drawArc(
            color = Color.White.copy(alpha = 0.1f + glow * 0.04f),
            startAngle = 15f + ring * 16f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(cycloneCenter.x - (170f - ring * 18f) * scale, cycloneCenter.y - (110f - ring * 12f) * scale),
            size = Size((340f - ring * 36f) * scale, (220f - ring * 24f) * scale),
            style = Stroke(width = 7f * scale),
        )
    }
    repeat(6) { index ->
        val waveX = index * 220f - (cameraX * 0.34f * scale) % (size.width + 220f)
        drawOval(
            color = Color(0x6644B1D8),
            topLeft = Offset(waveX, size.height * 0.72f + (index % 2) * 16f * scale),
            size = Size(260f * scale, 120f * scale),
        )
    }
}

private fun DrawScope.drawRainbowRoadSky(glow: Float, cameraX: Float, scale: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF120B2B), Color(0xFF271753), Color(0xFF3E2C77)),
        ),
    )
    repeat(42) { index ->
        val x = ((index * 173f - cameraX * 0.14f * scale) % (size.width + 120f)) - 60f
        val y = 30f + ((index * 97f) % (size.height * 0.68f))
        drawCircle(
            color = Color.White.copy(alpha = 0.34f + glow * 0.24f),
            radius = (1.6f + (index % 4) * 0.8f) * scale,
            center = Offset(x, y),
        )
    }
    val stripeColors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF7FD45D),
        Color(0xFF4EDBFF), Color(0xFF8B7CFF), Color(0xFFFF8FAB),
    )
    stripeColors.forEachIndexed { index, color ->
        drawRect(
            color = color.copy(alpha = 0.2f + glow * 0.04f),
            topLeft = Offset(0f, size.height * 0.7f + index * 20f * scale),
            size = Size(size.width, 16f * scale),
        )
    }
}

private fun DrawScope.drawGoldenDragon(cameraX: Float, scale: Float, glow: Float, dragonTimer: Float, laserActive: Boolean) {
    if (dragonTimer <= 0f) return
    val presence = (dragonTimer / 2.8f).coerceIn(0f, 1f)
    val exitShift = (1f - presence) * 340f * scale
    val dragonX = (2520f - cameraX) * scale + exitShift
    val dragonY = 220f * scale
    val tail = Path().apply {
        moveTo(dragonX - 148f * scale, dragonY + 22f * scale)
        quadraticTo(dragonX - 228f * scale, dragonY + 72f * scale, dragonX - 202f * scale, dragonY + 118f * scale)
        lineTo(dragonX - 154f * scale, dragonY + 68f * scale)
        close()
    }
    drawPath(tail, Color(0xFFFFC94A))
    drawOval(
        color = Color(0xFFFFD166),
        topLeft = Offset(dragonX - 116f * scale, dragonY - 4f * scale),
        size = Size(186f * scale, 90f * scale),
    )
    val belly = Path().apply {
        moveTo(dragonX - 38f * scale, dragonY + 24f * scale)
        quadraticTo(dragonX + 10f * scale, dragonY + 62f * scale, dragonX + 62f * scale, dragonY + 30f * scale)
        lineTo(dragonX + 44f * scale, dragonY + 58f * scale)
        quadraticTo(dragonX - 10f * scale, dragonY + 76f * scale, dragonX - 56f * scale, dragonY + 34f * scale)
        close()
    }
    drawPath(belly, Color(0xFFFFE6A6))
    val leftWing = Path().apply {
        moveTo(dragonX - 34f * scale, dragonY + 8f * scale)
        lineTo(dragonX - 154f * scale, dragonY - 122f * scale)
        lineTo(dragonX - 92f * scale, dragonY + 30f * scale)
        close()
    }
    val rightWing = Path().apply {
        moveTo(dragonX + 18f * scale, dragonY + 6f * scale)
        lineTo(dragonX + 146f * scale, dragonY - 108f * scale)
        lineTo(dragonX + 72f * scale, dragonY + 34f * scale)
        close()
    }
    drawPath(leftWing, Color(0xD9FFB703))
    drawPath(rightWing, Color(0xCCFFC94A))
    drawCircle(Color(0xFFFFD166), 34f * scale, Offset(dragonX + 116f * scale, dragonY + 10f * scale))
    val jaw = Path().apply {
        moveTo(dragonX + 108f * scale, dragonY + 28f * scale)
        lineTo(dragonX + 176f * scale, dragonY + 22f * scale)
        lineTo(dragonX + 132f * scale, dragonY + 48f * scale)
        close()
    }
    drawPath(jaw, Color(0xFFFFC94A))
    drawCircle(Color(0xFF101820), 5f * scale, Offset(dragonX + 126f * scale, dragonY + 4f * scale))
    val hornA = Path().apply {
        moveTo(dragonX + 98f * scale, dragonY - 8f * scale)
        lineTo(dragonX + 112f * scale, dragonY - 34f * scale)
        lineTo(dragonX + 122f * scale, dragonY - 4f * scale)
        close()
    }
    val hornB = Path().apply {
        moveTo(dragonX + 122f * scale, dragonY - 6f * scale)
        lineTo(dragonX + 142f * scale, dragonY - 28f * scale)
        lineTo(dragonX + 144f * scale, dragonY + 2f * scale)
        close()
    }
    drawPath(hornA, Color(0xFFFFF2C2))
    drawPath(hornB, Color(0xFFFFF2C2))
    repeat(4) { index ->
        val spikeX = dragonX - 52f * scale + index * 28f * scale
        val spike = Path().apply {
            moveTo(spikeX, dragonY + 4f * scale)
            lineTo(spikeX + 10f * scale, dragonY - 16f * scale)
            lineTo(spikeX + 18f * scale, dragonY + 6f * scale)
            close()
        }
        drawPath(spike, Color(0xFFFFB703))
    }
    if (laserActive) {
        drawLine(
            color = Color(0x99FFF1A8).copy(alpha = 0.6f + glow * 0.2f),
            start = Offset(dragonX + 172f * scale, dragonY + 24f * scale),
            end = Offset((2520f - cameraX) * scale, 620f * scale),
            strokeWidth = 10f * scale,
        )
    }
}

private fun DrawScope.drawFinalGoldenDragon(dragonWorldX: Float, cameraX: Float, scale: Float, glow: Float, motionPhase: Float) {
    val x = (dragonWorldX - cameraX) * scale
    val y = 260f * scale
    val bob = sin(motionPhase * 6.28318f) * 10f * scale
    val leftWing = Path().apply {
        moveTo(x - 40f * scale, y + 40f * scale)
        lineTo(x - 250f * scale, y - 140f * scale + bob)
        lineTo(x - 150f * scale, y + 70f * scale)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + 80f * scale, y + 30f * scale)
        lineTo(x + 300f * scale, y - 120f * scale - bob)
        lineTo(x + 170f * scale, y + 90f * scale)
        close()
    }
    drawPath(leftWing, Color(0xCCFFB703))
    drawPath(rightWing, Color(0xCCFFD166))
    val tail = Path().apply {
        moveTo(x - 220f * scale, y + 120f * scale)
        quadraticTo(x - 360f * scale, y + 190f * scale, x - 290f * scale, y + 260f * scale)
        lineTo(x - 190f * scale, y + 180f * scale)
        close()
    }
    drawPath(tail, Color(0xFFFFC94A))
    drawOval(
        color = Color(0xFFFFD166),
        topLeft = Offset(x - 180f * scale, y + 10f * scale),
        size = Size(320f * scale, 150f * scale),
    )
    drawOval(
        color = Color(0xFFFFE6A6),
        topLeft = Offset(x - 46f * scale, y + 56f * scale),
        size = Size(142f * scale, 76f * scale),
    )
    drawCircle(Color(0xFFFFD166), 60f * scale, Offset(x + 176f * scale, y + 52f * scale))
    val jaw = Path().apply {
        moveTo(x + 158f * scale, y + 82f * scale)
        lineTo(x + 286f * scale, y + 70f * scale)
        lineTo(x + 208f * scale, y + 122f * scale)
        close()
    }
    drawPath(jaw, Color(0xFFFFC94A))
    drawCircle(Color(0xFF101820), 8f * scale, Offset(x + 194f * scale, y + 36f * scale))
    repeat(7) { index ->
        val spikeX = x - 104f * scale + index * 38f * scale
        val spike = Path().apply {
            moveTo(spikeX, y + 18f * scale)
            lineTo(spikeX + 14f * scale, y - 18f * scale)
            lineTo(spikeX + 24f * scale, y + 16f * scale)
            close()
        }
        drawPath(spike, Color(0xFFFF8C42))
    }
    drawCircle(
        color = Color(0x33FFD166).copy(alpha = 0.18f + glow * 0.14f),
        radius = 120f * scale,
        center = Offset(x + 120f * scale, y + 80f * scale),
    )
}

private fun DrawScope.drawParallax(cameraX: Float, scale: Float) {
    val farShift = (cameraX * 0.15f * scale) % (size.width + 240f)
    repeat(4) { index ->
        val baseX = index * 420f - farShift
        drawMountain(baseX, size.height * 0.68f, 260f, 230f, Color(0x663B8EA5))
        drawMountain(baseX + 150f, size.height * 0.70f, 230f, 180f, Color(0x5567B99A))
    }

    val cloudShift = (cameraX * 0.28f * scale) % (size.width + 180f)
    repeat(5) { index ->
        val x = index * 290f - cloudShift
        val y = 92f + (index % 3) * 54f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.72f),
            topLeft = Offset(x, y),
            size = Size(150f, 34f),
            cornerRadius = CornerRadius(22f, 22f),
        )
        drawCircle(Color.White.copy(alpha = 0.78f), 34f, Offset(x + 36f, y + 8f))
        drawCircle(Color.White.copy(alpha = 0.74f), 42f, Offset(x + 88f, y - 6f))
    }
}

private fun DrawScope.drawMountain(
    x: Float,
    baseY: Float,
    width: Float,
    height: Float,
    color: Color,
) {
    val path = Path().apply {
        moveTo(x, baseY)
        lineTo(x + width * 0.5f, baseY - height)
        lineTo(x + width, baseY)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawPlatform(platform: GamePlatform, cameraX: Float, scale: Float, stage: Int) {
    val x = (platform.x - cameraX) * scale
    val y = platform.y * scale
    val width = platform.width * scale
    val height = platform.height * scale
    if (stage == 2) {
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(x + width * 0.12f, y + height * 0.82f),
            size = Size(width * 0.76f, height * 0.32f),
        )
        drawRoundRect(
            color = Color(0xFFF9F1D0),
            topLeft = Offset(x + width * 0.43f, y + height * 0.45f),
            size = Size(width * 0.14f, height * 1.1f),
            cornerRadius = CornerRadius(20f * scale, 20f * scale),
        )
        drawOval(
            color = Color(0xFFE85D75),
            topLeft = Offset(x, y - height * 0.32f),
            size = Size(width, height * 1.45f),
        )
        drawOval(
            color = Color(0xFFFFF6F6),
            topLeft = Offset(x + width * 0.12f, y - height * 0.08f),
            size = Size(width * 0.18f, height * 0.45f),
        )
        drawOval(
            color = Color(0xFFFFF6F6),
            topLeft = Offset(x + width * 0.42f, y - height * 0.22f),
            size = Size(width * 0.2f, height * 0.52f),
        )
        drawOval(
            color = Color(0xFFFFF6F6),
            topLeft = Offset(x + width * 0.72f, y + height * 0.02f),
            size = Size(width * 0.18f, height * 0.42f),
        )
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(x + width * 0.08f, y + height * 0.18f),
            end = Offset(x + width * 0.92f, y + height * 0.18f),
            strokeWidth = 3f * scale,
        )
        return
    }
    if (stage == 3) {
        drawOval(
            color = Color(0x55000000),
            topLeft = Offset(x + width * 0.12f, y + height * 0.72f),
            size = Size(width * 0.76f, height * 0.45f),
        )
        drawOval(
            color = Color(0xFF8E8A93),
            topLeft = Offset(x, y - height * 0.35f),
            size = Size(width, height * 1.55f),
        )
        drawOval(
            color = Color(0xFFB8B3BD),
            topLeft = Offset(x + width * 0.12f, y - height * 0.17f),
            size = Size(width * 0.25f, height * 0.45f),
        )
        drawOval(
            color = Color(0xFF6F6B74),
            topLeft = Offset(x + width * 0.46f, y + height * 0.02f),
            size = Size(width * 0.18f, height * 0.34f),
        )
        drawOval(
            color = Color(0xFF5B5760),
            topLeft = Offset(x + width * 0.7f, y - height * 0.1f),
            size = Size(width * 0.2f, height * 0.42f),
        )
        drawLine(
            color = Color(0x99D8D5E0),
            start = Offset(x + width * 0.08f, y + height * 0.22f),
            end = Offset(x + width * 0.9f, y + height * 0.05f),
            strokeWidth = 3f * scale,
        )
        return
    }
    if (stage == 4) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF74D65D), Color(0xFF2C8A4C)),
                startY = y,
                endY = y + height * 0.5f,
            ),
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(14f * scale, 14f * scale),
        )
        drawRoundRect(
            color = Color(0xFF9B6B43),
            topLeft = Offset(x, y + height * 0.45f),
            size = Size(width, height * 0.55f),
            cornerRadius = CornerRadius(8f * scale, 8f * scale),
        )
        repeat((platform.width / 86f).toInt().coerceAtLeast(1)) { index ->
            val flowerX = x + (index * 86f + 32f) * scale
            val flowerY = y - 8f * scale
            drawCircle(Color(0xFFE85D75), 7f * scale, Offset(flowerX - 7f * scale, flowerY))
            drawCircle(Color(0xFFFF8DA1), 7f * scale, Offset(flowerX + 7f * scale, flowerY))
            drawCircle(Color(0xFFFFD166), 5f * scale, Offset(flowerX, flowerY))
        }
        return
    }
    if (stage == 5) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF9BB1C8), Color(0xFF47596D)),
                startY = y,
                endY = y + height,
            ),
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(10f * scale, 10f * scale),
        )
        repeat((platform.width / 70f).toInt().coerceAtLeast(1)) { index ->
            val stripeX = x + (18f + index * 70f) * scale
            drawLine(
                color = Color(0xAA0E1820),
                start = Offset(stripeX, y + 6f * scale),
                end = Offset(stripeX + 24f * scale, y + height - 6f * scale),
                strokeWidth = 3f * scale,
            )
        }
        return
    }
    if (stage == 6) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF7FD45D), Color(0xFF52983F)),
            ),
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(10f * scale, 10f * scale),
        )
        drawLine(
            color = Color(0x88C8F08A),
            start = Offset(x, y + height * 0.18f),
            end = Offset(x + width, y + height * 0.18f),
            strokeWidth = 4f * scale,
        )
        return
    }
    if (stage == 8) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB9C9D7), Color(0xFF5E7286)),
            ),
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(10f * scale, 10f * scale),
        )
        repeat((platform.width / 68f).toInt().coerceAtLeast(1)) { index ->
            val rivetX = x + (24f + index * 68f) * scale
            drawCircle(Color(0xFF23384C), 3.5f * scale, Offset(rivetX, y + 10f * scale))
            drawCircle(Color(0xFF23384C), 3.5f * scale, Offset(rivetX, y + height - 10f * scale))
        }
        return
    }
    if (stage == 9) {
        val rainbow = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFD166),
            Color(0xFF7FD45D),
            Color(0xFF4EDBFF),
            Color(0xFF8B7CFF),
            Color(0xFFFF8FAB),
        )
        rainbow.forEachIndexed { index, color ->
            drawRect(
                color = color,
                topLeft = Offset(x, y + height * index / rainbow.size),
                size = Size(width, height / rainbow.size + 1f),
            )
        }
        drawLine(
            color = Color.White.copy(alpha = 0.44f),
            start = Offset(x, y + 12f * scale),
            end = Offset(x + width, y + 12f * scale),
            strokeWidth = 3f * scale,
        )
        return
    }
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF38B000), Color(0xFF1F7A38)),
            startY = y,
            endY = y + height * 0.48f,
        ),
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f * scale, 12f * scale),
    )
    drawRoundRect(
        color = Color(0xFF785034),
        topLeft = Offset(x, y + height * 0.42f),
        size = Size(width, height * 0.58f),
        cornerRadius = CornerRadius(8f * scale, 8f * scale),
    )
    repeat((platform.width / 70f).toInt()) { index ->
        drawLine(
            color = Color(0x553A281B),
            start = Offset(x + index * 70f * scale, y + height * 0.48f),
            end = Offset(x + (index * 70f + 38f) * scale, y + height),
            strokeWidth = 3f * scale,
        )
    }
}

private fun DrawScope.drawCoin(coin: GameCoin, cameraX: Float, scale: Float, glow: Float) {
    val center = Offset((coin.x - cameraX) * scale, coin.y * scale)
    drawCircle(
        color = Color(0x55FFFFFF).copy(alpha = 0.16f + glow * 0.14f),
        radius = coin.radius * 2.2f * scale,
        center = center,
    )
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - 15f * scale, center.y - 20f * scale),
        size = Size(30f * scale, 40f * scale),
    )
    drawCircle(
        color = Color(0xFF39A66B),
        radius = 5f * scale,
        center = center.copy(x = center.x - 6f * scale, y = center.y - 6f * scale),
    )
    drawCircle(
        color = Color(0xFFE85D75),
        radius = 4.5f * scale,
        center = center.copy(x = center.x + 7f * scale, y = center.y + 3f * scale),
    )
    drawOval(
        color = Color(0xFF7FDCA1),
        topLeft = Offset(center.x - 15f * scale, center.y - 20f * scale),
        size = Size(30f * scale, 40f * scale),
        style = Stroke(width = 3f * scale),
    )
}

private fun DrawScope.drawTree(tree: GameTree, cameraX: Float, scale: Float) {
    val trunkX = (tree.x - cameraX) * scale
    val baseY = tree.y * scale
    drawRoundRect(
        color = Color(0xFF825634),
        topLeft = Offset(trunkX - 16f * scale, baseY - 110f * scale),
        size = Size(32f * scale, 110f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale),
    )
    repeat(3) { index ->
        drawCircle(
            color = Color(0xFF4D9E49),
            radius = (34f - index * 2f) * scale,
            center = Offset(trunkX + (index - 1) * 22f * scale, baseY - 126f * scale - (index % 2) * 16f * scale),
        )
    }
}

private fun DrawScope.drawApple(apple: GameApple, cameraX: Float, scale: Float, glow: Float) {
    val center = Offset((apple.x - cameraX) * scale, apple.y * scale)
    if (apple.age >= 5f) {
        val rainbow = listOf(
            Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926),
            Color(0xFF1982C4), Color(0xFF6A4C93)
        )
        rainbow.forEachIndexed { index, color ->
            drawArc(
                color = color.copy(alpha = 0.78f),
                startAngle = index * 72f,
                sweepAngle = 72f,
                useCenter = true,
                topLeft = Offset(center.x - 18f * scale, center.y - 18f * scale),
                size = Size(36f * scale, 36f * scale),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.22f + glow * 0.14f), 24f * scale, center)
    } else {
        drawCircle(Color(0xFFFF6B6B).copy(alpha = 0.24f + glow * 0.1f), 22f * scale, center)
        drawCircle(Color(0xFFE64848), 16f * scale, center)
    }
    drawLine(
        color = Color(0xFF6B4A2C),
        start = Offset(center.x, center.y - 10f * scale),
        end = Offset(center.x + 2f * scale, center.y - 24f * scale),
        strokeWidth = 3f * scale,
    )
    drawOval(
        color = Color(0xFF7FD45D),
        topLeft = Offset(center.x + 2f * scale, center.y - 24f * scale),
        size = Size(12f * scale, 7f * scale),
    )
}

private fun DrawScope.drawGiantBoss(boss: GiantBossState, cameraX: Float, scale: Float, motionPhase: Float) {
    val x = (boss.x - cameraX) * scale
    val deathProgress = if (boss.health <= 0) 1f - (boss.deathTimer / 1.6f).coerceIn(0f, 1f) else 0f
    val pushLean = if (boss.pushTimer > 0f) boss.direction * 22f * scale else 0f
    val y = (650f + boss.deathFall) * scale
    val bob = sin(motionPhase * 6.28318f) * 6f * scale * (1f - deathProgress)
    val alpha = (1f - deathProgress * 0.78f).coerceIn(0f, 1f)
    val squash = 1f - deathProgress * 0.34f
    drawOval(
        color = Color(0x55000000).copy(alpha = alpha),
        topLeft = Offset(x - 130f * scale, y + 68f * scale),
        size = Size(260f * scale, 28f * scale),
    )
    val tail = Path().apply {
        moveTo(x - 72f * scale - pushLean, y + 32f * scale)
        lineTo(x - 188f * scale - pushLean, y - 8f * scale)
        lineTo(x - 112f * scale - pushLean, y + 56f * scale)
        close()
    }
    drawPath(tail, Color(0xFF5F7E4A).copy(alpha = alpha))
    drawOval(
        Color(0xFF6C8F4E).copy(alpha = alpha),
        Offset(x - 74f * scale - pushLean * 0.5f, y - 10f * scale + bob),
        Size(148f * scale, 86f * scale * squash),
    )
    drawCircle(Color(0xFF6C8F4E).copy(alpha = alpha), 52f * scale * squash, Offset(x + 78f * scale + pushLean, y - 8f * scale + bob))
    drawOval(Color(0xFFEAF5D8).copy(alpha = alpha), Offset(x - 26f * scale, y + 8f * scale + bob), Size(72f * scale, 46f * scale * squash))
    repeat(6) { index ->
        val spike = Path().apply {
            val sx = x - 36f * scale + index * 24f * scale
            moveTo(sx, y - 8f * scale + bob)
            lineTo(sx + 10f * scale, y - 28f * scale + bob)
            lineTo(sx + 20f * scale, y - 8f * scale + bob)
            close()
        }
        drawPath(spike, Color(0xFFE85D75).copy(alpha = alpha))
    }
    drawCircle(Color.White.copy(alpha = alpha), 14f * scale, Offset(x + 96f * scale + pushLean, y - 24f * scale + bob))
    drawCircle(Color(0xFF101820).copy(alpha = alpha), 6f * scale, Offset(x + 100f * scale + pushLean, y - 24f * scale + bob))
    drawRoundRect(
        color = Color(0xFF7A1F30).copy(alpha = alpha),
        topLeft = Offset(x + 110f * scale + pushLean, y - 2f * scale + bob),
        size = Size(42f * scale, 12f * scale),
        cornerRadius = CornerRadius(6f * scale, 6f * scale),
    )
    if (deathProgress > 0f) {
        repeat(4) { index ->
            val sparkleX = x - 56f * scale + index * 34f * scale
            drawCircle(
                color = Color(0xFFFFD166).copy(alpha = (0.8f - deathProgress * 0.5f).coerceAtLeast(0f)),
                radius = (8f + index) * scale * (1f - deathProgress * 0.45f),
                center = Offset(sparkleX, y - 22f * scale - index * 8f * scale),
            )
        }
    }
}

private fun DrawScope.drawEggShot(shot: GameEggShot, cameraX: Float, scale: Float, glow: Float) {
    val center = Offset((shot.x - cameraX) * scale, shot.y * scale)
    drawCircle(
        color = Color(0x33FFFFFF).copy(alpha = 0.14f + glow * 0.1f),
        radius = 26f * scale,
        center = center,
    )
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - 14f * scale, center.y - 17f * scale),
        size = Size(28f * scale, 34f * scale),
    )
    drawCircle(Color(0xFF39A66B), 4.5f * scale, Offset(center.x - 5f * scale, center.y - 5f * scale))
    drawCircle(Color(0xFFE85D75), 4f * scale, Offset(center.x + 6f * scale, center.y + 4f * scale))
}

private fun bossAttackEggX(egg: BossAttackEgg): Float {
    val t = egg.progress.coerceIn(0f, 1f)
    return egg.startX + (egg.targetX - egg.startX) * t
}

private fun bossAttackEggY(egg: BossAttackEgg): Float {
    val t = egg.progress.coerceIn(0f, 1f)
    val arc = sin(t * 3.14159f) * 180f
    return egg.startY + (egg.targetY - egg.startY) * t - arc
}

private fun DrawScope.drawBossAttackEgg(egg: BossAttackEgg, cameraX: Float, scale: Float, glow: Float) {
    if (egg.warningTimer > 0f) {
        val start = Offset((egg.startX - cameraX) * scale, egg.startY * scale)
        var previous = start
        repeat(16) { index ->
            val t = (index + 1) / 16f
            val x = (egg.startX + (egg.targetX - egg.startX) * t - cameraX) * scale
            val y = (egg.startY + (egg.targetY - egg.startY) * t - sin(t * 3.14159f) * 180f) * scale
            val next = Offset(x, y)
            drawLine(
                color = Color(0xCCFFD166),
                start = previous,
                end = next,
                strokeWidth = 3f * scale,
            )
            previous = next
        }
        drawCircle(
            color = Color(0x66E85D75),
            radius = 22f * scale,
            center = Offset((egg.targetX - cameraX) * scale, egg.targetY * scale),
            style = Stroke(width = 4f * scale),
        )
        return
    }
    val center = Offset((bossAttackEggX(egg) - cameraX) * scale, bossAttackEggY(egg) * scale)
    drawCircle(
        color = Color(0x44FFD166).copy(alpha = 0.22f + glow * 0.12f),
        radius = 30f * scale,
        center = center,
    )
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - 16f * scale, center.y - 20f * scale),
        size = Size(32f * scale, 40f * scale),
    )
    drawCircle(Color(0xFFE85D75), 5f * scale, Offset(center.x - 5f * scale, center.y - 6f * scale))
    drawCircle(Color(0xFF39A66B), 5f * scale, Offset(center.x + 7f * scale, center.y + 4f * scale))
}

private fun DrawScope.drawFlowerBoss(
    bossX: Float,
    cameraX: Float,
    scale: Float,
    motionPhase: Float,
    health: Int,
    hitFlash: Float,
) {
    val x = (bossX - cameraX) * scale
    val y = 560f * scale
    val sway = sin(motionPhase * 6.28318f) * 8f * scale
    drawLine(
        color = Color(0xFF2F915F),
        start = Offset(x, y + 130f * scale),
        end = Offset(x + sway, y + 10f * scale),
        strokeWidth = 16f * scale,
    )
    repeat(2) { side ->
        val dir = if (side == 0) -1f else 1f
        drawOval(
            color = Color(0xFF39A66B),
            topLeft = Offset(x + dir * 58f * scale - 30f * scale, y + 70f * scale),
            size = Size(60f * scale, 28f * scale),
        )
    }
    val petalColor = if (hitFlash > 0f) Color.White else Color(0xFFE85D75)
    repeat(10) { index ->
        val angle = index * 0.628f + motionPhase * 0.25f
        drawOval(
            color = petalColor,
            topLeft = Offset(
                x + sway + cos(angle) * 58f * scale - 28f * scale,
                y + sin(angle) * 58f * scale - 24f * scale,
            ),
            size = Size(56f * scale, 48f * scale),
        )
    }
    drawCircle(Color(0xFFFFD166), 58f * scale, Offset(x + sway, y))
    drawCircle(Color(0xFF101820), 6f * scale, Offset(x + sway - 18f * scale, y - 8f * scale))
    drawCircle(Color(0xFF101820), 6f * scale, Offset(x + sway + 18f * scale, y - 8f * scale))
    drawRoundRect(
        color = Color(0xFF7A1F30),
        topLeft = Offset(x + sway - 24f * scale, y + 18f * scale),
        size = Size(48f * scale, 10f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale),
    )
    drawRoundRect(
        color = Color(0xAA101820),
        topLeft = Offset(x - 74f * scale, y - 100f * scale),
        size = Size(148f * scale, 14f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale),
    )
    drawRoundRect(
        color = Color(0xFFE85D75),
        topLeft = Offset(x - 70f * scale, y - 96f * scale),
        size = Size(140f * (health / 10f) * scale, 6f * scale),
        cornerRadius = CornerRadius(6f * scale, 6f * scale),
    )
}

private fun DrawScope.drawEnemy(
    enemy: GameEnemy,
    cameraX: Float,
    scale: Float,
    motionPhase: Float,
    playerX: Float,
    playerY: Float,
    facingDirection: Float,
) {
    val pull = enemy.eatenProgress.coerceIn(0f, 1f)
    val targetX = playerX + if (facingDirection > 0f) 48f else 4f
    val targetY = playerY + 22f
    val worldX = if (pull > 0f) enemy.eatenFromX + (targetX - enemy.eatenFromX) * pull else enemy.x
    val worldY = if (pull > 0f) enemy.eatenFromY + (targetY - enemy.eatenFromY) * pull else enemy.y
    val shrink = if (pull > 0f) (1f - pull * 0.55f).coerceAtLeast(0.32f) else 1f
    val x = (worldX - cameraX) * scale
    val bounce = sin((motionPhase * 6.28318f) + enemy.startX * 0.01f) * 3f * scale
    val y = worldY * scale + if (pull > 0f || enemy.grabbed || enemy.thrown) 0f else bounce
    val enemyScale = scale * shrink
    if (enemy.grabbed) {
        drawLine(
            color = Color(0x88E85D75),
            start = Offset((targetX - cameraX) * scale, targetY * scale),
            end = Offset(x + 27f * enemyScale, y + 27f * enemyScale),
            strokeWidth = 5f * scale,
        )
        drawCircle(
            color = Color(0x44FFD166),
            radius = (42f + enemy.spinTaps * 8f) * scale,
            center = Offset((playerX + 28f - cameraX) * scale, (playerY + 28f) * scale),
            style = Stroke(width = 3f * scale),
        )
    }
    drawOval(
        color = Color(0x55000000),
        topLeft = Offset(x + 5f * enemyScale, y + 47f * enemyScale),
        size = Size(45f * enemyScale, 10f * enemyScale),
    )
    drawRoundRect(
        color = Color(0xFFD34D4D),
        topLeft = Offset(x, y),
        size = Size(54f * enemyScale, 54f * enemyScale),
        cornerRadius = CornerRadius(22f * enemyScale, 22f * enemyScale),
    )
    drawRoundRect(
        color = Color(0xFFFFE6D0),
        topLeft = Offset(x + 12f * enemyScale, y + 14f * enemyScale),
        size = Size(30f * enemyScale, 22f * enemyScale),
        cornerRadius = CornerRadius(12f * enemyScale, 12f * enemyScale),
    )
    drawCircle(Color(0xFF101820), 3.5f * enemyScale, Offset(x + 21f * enemyScale, y + 24f * enemyScale))
    drawCircle(Color(0xFF101820), 3.5f * enemyScale, Offset(x + 33f * enemyScale, y + 24f * enemyScale))
    drawRoundRect(
        color = Color(0xFF7A1F30),
        topLeft = Offset(x + 8f * enemyScale, y + 6f * enemyScale),
        size = Size(38f * enemyScale, 10f * enemyScale),
        cornerRadius = CornerRadius(10f * enemyScale, 10f * enemyScale),
    )
}

private fun DrawScope.drawGoal(goalX: Float, baseWorldY: Float, cameraX: Float, scale: Float, glow: Float) {
    val x = (goalX - cameraX) * scale
    val baseY = baseWorldY * scale
    drawLine(
        color = Color.White,
        start = Offset(x, baseY),
        end = Offset(x, baseY - 220f * scale),
        strokeWidth = 8f * scale,
    )
    val flag = Path().apply {
        moveTo(x, baseY - 210f * scale)
        lineTo(x + 96f * scale, baseY - 178f * scale)
        lineTo(x, baseY - 146f * scale)
        close()
    }
    drawPath(flag, Color(0xFF39A66B).copy(alpha = 0.82f + glow * 0.12f))
    drawCircle(Color(0xFFFFD166), 18f * scale, Offset(x + 64f * scale, baseY - 178f * scale))
    drawCircle(Color.White, 7f * scale, Offset(x + 64f * scale, baseY - 178f * scale))
}

private fun DrawScope.drawTauntBat(goalX: Float, cameraX: Float, scale: Float, glow: Float, message: String) {
    val x = (goalX - cameraX) * scale
    val y = 250f * scale
    val flap = sin(glow * 6.28318f) * 10f * scale
    drawOval(
        color = Color(0xCC1A1430),
        topLeft = Offset(x - 30f * scale, y - 12f * scale),
        size = Size(60f * scale, 34f * scale),
    )
    val leftWing = Path().apply {
        moveTo(x - 8f * scale, y - 4f * scale)
        quadraticTo(x - 52f * scale, y - 34f * scale - flap, x - 62f * scale, y + 8f * scale)
        quadraticTo(x - 38f * scale, y + 2f * scale, x - 8f * scale, y + 10f * scale)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + 8f * scale, y - 4f * scale)
        quadraticTo(x + 52f * scale, y - 34f * scale + flap, x + 62f * scale, y + 8f * scale)
        quadraticTo(x + 38f * scale, y + 2f * scale, x + 8f * scale, y + 10f * scale)
        close()
    }
    drawPath(leftWing, Color(0xFF23183C))
    drawPath(rightWing, Color(0xFF23183C))
    drawCircle(Color(0xFFFFD166), 4f * scale, Offset(x - 10f * scale, y - 3f * scale))
    drawCircle(Color(0xFFFFD166), 4f * scale, Offset(x + 10f * scale, y - 3f * scale))

    val bubbleTopLeft = Offset(x - 168f * scale, y - 146f * scale)
    drawRoundRect(
        color = Color(0xEAFEF5FF),
        topLeft = bubbleTopLeft,
        size = Size(336f * scale, 92f * scale),
        cornerRadius = CornerRadius(18f * scale, 18f * scale),
    )
    val pointer = Path().apply {
        moveTo(x + 42f * scale, y - 54f * scale)
        lineTo(x + 18f * scale, y - 22f * scale)
        lineTo(x + 70f * scale, y - 44f * scale)
        close()
    }
    drawPath(pointer, Color(0xEAFEF5FF))
    val paint = AndroidPaint().apply {
        color = android.graphics.Color.rgb(34, 28, 52)
        textSize = 18f * scale
        isAntiAlias = true
    }
    message.split('\n').forEachIndexed { index, line ->
        drawContext.canvas.nativeCanvas.drawText(
            line,
            bubbleTopLeft.x + 16f * scale,
            bubbleTopLeft.y + (28f + index * 24f) * scale,
            paint,
        )
    }
}

private fun DrawScope.drawCoreOrb(orbX: Float, orbY: Float, cameraX: Float, scale: Float, glow: Float) {
    val center = Offset((orbX - cameraX) * scale, orbY * scale)
    drawCircle(Color(0x334EDBFF).copy(alpha = 0.22f + glow * 0.16f), 54f * scale, center)
    drawCircle(Color(0xFF7BE6FF), 26f * scale, center)
    drawCircle(Color.White, 10f * scale, center)
    repeat(3) { ring ->
        drawCircle(
            color = Color(0x884EDBFF),
            radius = (34f + ring * 14f + glow * 4f) * scale,
            center = center,
            style = Stroke(width = 3f * scale),
        )
    }
}

private fun DrawScope.drawPlayer(
    playerX: Float,
    playerY: Float,
    cameraX: Float,
    scale: Float,
    facingDirection: Float,
    tongueTimer: Float,
    tonguePower: Float,
    isTransformed: Boolean,
    heatRayTimer: Float,
    rainbowTimer: Float,
    runSpeed: Float,
    isGrounded: Boolean,
    verticalVelocity: Float,
    motionPhase: Float,
) {
    val runningAmount = if (isGrounded) (runSpeed / 430f).coerceIn(0f, 1f) else 0.35f
    val stride = sin(motionPhase * 6.28318f)
    val counterStride = sin(motionPhase * 6.28318f + 3.14159f)
    val jumpPose = if (isGrounded) 0f else 1f
    val risingPose = if (!isGrounded && verticalVelocity < 0f) 1f else 0f
    val bodyBob = if (isGrounded) abs(stride) * -4f * runningAmount * scale else -3f * scale
    val tailWag = (stride * 7f * runningAmount + jumpPose * if (risingPose > 0f) -7f else 5f) * scale
    val headBob = if (isGrounded) {
        counterStride * 2.5f * runningAmount * scale
    } else {
        if (risingPose > 0f) -5f * scale else 3f * scale
    }
    val footLiftA = if (isGrounded) max(0f, stride) * 7f * runningAmount * scale else 13f * scale
    val footLiftB = if (isGrounded) max(0f, -stride) * 7f * runningAmount * scale else 10f * scale
    val footTuckA = if (isGrounded) 0f else 7f * scale
    val footTuckB = if (isGrounded) 0f else -6f * scale
    val x = (playerX - cameraX) * scale
    val y = playerY * scale + bodyBob
    val facing = facingDirection
    val mouth = Offset(x + (if (facing > 0f) 51f else 7f) * scale, y + 23f * scale + headBob)
    if (isTransformed && heatRayTimer > 0f) {
        val beamLength = 360f * scale
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xAA8BE8FF), Color(0xFF4EDBFF), Color.White),
            ),
            topLeft = Offset(if (facing > 0f) mouth.x else mouth.x - beamLength, mouth.y - 10f * scale),
            size = Size(beamLength, 20f * scale),
            cornerRadius = CornerRadius(10f * scale, 10f * scale),
        )
    }
    if (tongueTimer > 0f) {
        val tongueDuration = if (tonguePower > 1f) 0.28f else 0.18f
        val tongueLength = (95f * tonguePower + 30f * tonguePower * (tongueTimer / tongueDuration)) * scale
        drawRoundRect(
            color = Color(0xFFE85D75),
            topLeft = Offset(
                x = if (facing > 0f) mouth.x else mouth.x - tongueLength,
                y = mouth.y - 5f * scale,
            ),
            size = Size(tongueLength, 10f * scale),
            cornerRadius = CornerRadius(8f * scale, 8f * scale),
        )
        drawCircle(
            color = Color(0xFFFF8DA1),
            radius = 8f * scale,
            center = Offset(mouth.x + facing * tongueLength, mouth.y),
        )
    }
    drawOval(
        color = Color(0x44000000),
        topLeft = Offset(x + 8f * scale, y + 70f * scale),
        size = Size(48f * scale, 10f * scale),
    )
    val tail = Path().apply {
        if (facing > 0f) {
            moveTo(x + 14f * scale, y + 43f * scale)
            lineTo(x - 18f * scale, y + 31f * scale + tailWag)
            lineTo(x + 8f * scale, y + 57f * scale)
        } else {
            moveTo(x + 44f * scale, y + 43f * scale)
            lineTo(x + 76f * scale, y + 31f * scale + tailWag)
            lineTo(x + 50f * scale, y + 57f * scale)
        }
        close()
    }
    val rainbowBody = rainbowTimer > 0f
    val bodyGreen = if (rainbowBody) Color(0xFFFFD166) else if (isTransformed) Color(0xFF2A3643) else Color(0xFF39A66B)
    val bellyColor = if (rainbowBody) Color(0xFFFFF6D6) else if (isTransformed) Color(0xFF8EA1B1) else Color(0xFFE9FFE7)
    drawPath(tail, if (rainbowBody) Color(0xFFFF8FAB) else if (isTransformed) Color(0xFF22303C) else Color(0xFF2F915F))
    drawOval(
        color = bodyGreen,
        topLeft = Offset(x + 10f * scale, y + 26f * scale),
        size = Size(38f * scale, 46f * scale),
    )
    drawOval(
        color = bellyColor,
        topLeft = Offset(x + 17f * scale, y + 37f * scale),
        size = Size(23f * scale, 29f * scale),
    )
    drawCircle(
        color = bodyGreen,
        radius = 23f * scale,
        center = Offset(x + 30f * scale, y + 22f * scale + headBob),
    )
    repeat(if (isTransformed) 5 else 4) { index ->
        val spikeX = x + (16f + index * 8f) * scale
        val spikeY = y + (2f + (index % 2) * 2f) * scale + headBob * 0.45f
        val spike = Path().apply {
            moveTo(spikeX, spikeY + 12f * scale)
            lineTo(spikeX + 5f * scale, spikeY)
            lineTo(spikeX + 10f * scale, spikeY + 12f * scale)
            close()
        }
        drawPath(spike, if (rainbowBody) Color(0xFF6ED7FF) else if (isTransformed) Color(0xFF6ED7FF) else Color(0xFFE85D75))
    }
    drawOval(
        color = if (rainbowBody) Color(0xFFFF8FAB) else if (isTransformed) Color(0xFF70879A) else Color(0xFF7FDCA1),
        topLeft = Offset(x + (if (facing > 0f) 39f else -5f) * scale, y + 12f * scale + headBob),
        size = Size(29f * scale, 20f * scale),
    )
    drawCircle(Color.White, 7f * scale, Offset(x + (30f + facing * 9f) * scale, y + 12f * scale + headBob))
    drawCircle(Color(0xFF101820), 3f * scale, Offset(x + (32f + facing * 10f) * scale, y + 12f * scale + headBob))
    drawRoundRect(
        color = if (rainbowBody) Color(0xFF6ED7FF) else if (isTransformed) Color(0xFF4EDBFF) else Color(0xFFE85D75),
        topLeft = Offset(x + (if (facing > 0f) 57f else -7f) * scale, y + 24f * scale + headBob),
        size = Size(11f * scale, 5f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale),
    )
    drawRoundRect(
        color = if (rainbowBody) Color(0xFFFFCA3A) else if (isTransformed) Color(0xFF22303C) else Color(0xFF2F915F),
        topLeft = Offset(
            x + (if (facing > 0f) 39f else 13f) * scale,
            y + (if (isGrounded) 44f else 39f) * scale,
        ),
        size = Size(16f * scale, 6f * scale),
        cornerRadius = CornerRadius(5f * scale, 5f * scale),
    )
    drawRoundRect(
        color = Color(0xFF101820),
        topLeft = Offset(x + 8f * scale + footTuckA, y + 68f * scale - footLiftA),
        size = Size(18f * scale, 9f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale),
    )
    drawRoundRect(
        color = Color(0xFF101820),
        topLeft = Offset(x + 35f * scale + footTuckB, y + 68f * scale - footLiftB),
        size = Size(18f * scale, 9f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale),
    )
}
