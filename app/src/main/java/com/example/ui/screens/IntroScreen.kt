package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundEffects
import com.example.ui.theme.AstralGold
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class IntroStage {
    COMPANY_NAME, // Step 1: Avenix Technology (2.0s)
    GAME_NAME,    // Step 2: DreamDash Jungle Fly-Through & Title Reveal
    LOADING       // Step 3: Enter Jungle -> Loading -> Main Menu
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IntroScreen(
    soundEffects: SoundEffects,
    onComplete: () -> Unit
) {
    var stage by remember { mutableStateOf(IntroStage.COMPANY_NAME) }
    var isTransitioningForward by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Stop music on disposal to prevent leaks
    DisposableEffect(Unit) {
        onDispose {
            soundEffects.stopJungleAdventureMusic()
        }
    }

    // Step 1: Avenix Technology (2 seconds)
    LaunchedEffect(Unit) {
        soundEffects.playJungleAmbience()
        delay(2200L)
        if (stage == IntroStage.COMPANY_NAME) {
            stage = IntroStage.GAME_NAME
        }
    }

    // Step 2: DreamDash Game Title and Music Start
    LaunchedEffect(stage) {
        if (stage == IntroStage.GAME_NAME) {
            soundEffects.startJungleAdventureMusic()
        }
    }

    fun handleEnterJungle() {
        if (isTransitioningForward) return
        isTransitioningForward = true
        soundEffects.playEnterJungleSound()
        scope.launch {
            // Camera dives forward into jungle for 650ms before loading
            delay(650L)
            stage = IntroStage.LOADING
        }
    }

    fun handleSkip() {
        soundEffects.stopJungleAdventureMusic()
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cinematic_intro_screen")
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(550)) with fadeOut(animationSpec = tween(450))
            },
            label = "intro_stage_switcher"
        ) { currentStage ->
            when (currentStage) {
                IntroStage.COMPANY_NAME -> {
                    AvenixTechnologyIntro(
                        onNext = { stage = IntroStage.GAME_NAME },
                        onSkip = { handleSkip() }
                    )
                }

                IntroStage.GAME_NAME -> {
                    DreamDashTitleScene(
                        isZoomingForward = isTransitioningForward,
                        soundEffects = soundEffects,
                        onEnterJungle = { handleEnterJungle() },
                        onSkip = { handleSkip() }
                    )
                }

                IntroStage.LOADING -> {
                    JungleLoadingScreen(
                        onFinished = {
                            soundEffects.stopJungleAdventureMusic()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * STEP 1: AVENIX TECHNOLOGY INTRO
 * Dark jungle background with subtle green and gold lighting, glowing particles,
 * professional technology emblem, smooth animation onto the screen, and jungle ambience.
 * Stays visible for ~2 seconds.
 */
@Composable
private fun AvenixTechnologyIntro(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avenix_lights")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val particleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_rotation"
    )

    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.90f) }

    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
        entryScale.animateTo(1f, animationSpec = tween(850, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("avenix_technology_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNext() }
    ) {
        // Dark Jungle Background
        Image(
            painter = painterResource(id = R.drawable.img_dark_jungle_intro),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Vignette & Subtle Emerald/Gold Lighting Flare Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Center Darkening for maximum contrast on technology emblem
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.70f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.70f
                ),
                center = center,
                radius = size.minDimension * 0.70f
            )

            // Subtle Emerald & Gold Bioluminescent Lighting Halos
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.24f * pulse),
                        Color(0xFF10B981).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.65f
                ),
                center = center,
                radius = size.minDimension * 0.65f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.28f * pulse),
                        Color(0xFFF59E0B).copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.40f
                ),
                center = center,
                radius = size.minDimension * 0.40f
            )

            // Orbiting ambient glowing spore particles
            rotate(particleRotation, pivot = center) {
                val particleCount = 20
                for (i in 0 until particleCount) {
                    val angle = (i * (360f / particleCount)) * (Math.PI / 180f).toFloat()
                    val dist = size.minDimension * (0.28f + (i % 4) * 0.07f)
                    val px = center.x + cos(angle) * dist
                    val py = center.y + sin(angle) * dist
                    val pColor = if (i % 2 == 0) Color(0xFF00E676) else Color(0xFFFFD700)
                    drawCircle(
                        color = pColor.copy(alpha = 0.35f + (i % 3) * 0.15f),
                        radius = 2.5f + (i % 3) * 1.5f,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Top skip button
        SkipIntroButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            onSkip = onSkip
        )

        // Center Content: Technology Emblem & Avenix Technology
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .alpha(entryAlpha.value)
                .scale(entryScale.value)
        ) {
            // Professional Technology Emblem
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF00E676),
                        ambientColor = Color(0xFFFFD700)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Outer Hexagonal Technology Crest
                    val hexPath = Path().apply {
                        val r = w * 0.46f
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60 - 30).toDouble())
                            val x = center.x + (r * cos(a)).toFloat()
                            val y = center.y + (r * sin(a)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }

                    // Emerald & Gold Gradient Stroke
                    drawPath(
                        path = hexPath,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFF00E676),
                                Color(0xFFFFD700),
                                Color(0xFF10B981),
                                Color(0xFFFFD700)
                            ),
                            center = center
                        ),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Stylized Geometric 'A' Technology Emblem
                    val emblemPath = Path().apply {
                        moveTo(center.x, h * 0.22f)
                        lineTo(w * 0.26f, h * 0.76f)
                        lineTo(w * 0.38f, h * 0.76f)
                        lineTo(center.x, h * 0.44f)
                        lineTo(w * 0.62f, h * 0.76f)
                        lineTo(w * 0.74f, h * 0.76f)
                        close()
                    }

                    drawPath(
                        path = emblemPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFF00E676))
                        )
                    )

                    // Pulsing Core Power Node
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = 4.5.dp.toPx() * pulse,
                        center = Offset(center.x, h * 0.58f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Company Name: AVENIX TECHNOLOGY
            Text(
                text = "AVENIX TECHNOLOGY",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .shadow(16.dp, spotColor = Color(0xFF00E676), ambientColor = Color(0xFFFFD700))
                    .testTag("avenix_technology_heading")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtle gold divider and subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(32.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFFD700)))
                        )
                )

                Text(
                    text = "NEXT-GEN JUNGLE ADVENTURES",
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = Color(0xFFFFD700).copy(alpha = 0.92f),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(32.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color.Transparent))
                        )
                )
            }
        }
    }
}

/**
 * STEP 2: DREAMDASH GAME TITLE & STEP 3: ENTER JUNGLE BUTTON
 * - 3D tropical jungle with tall trees, thick vegetation, vines, canopy sunlight,
 *   flowing river, distant waterfall, ancient stone ruins, wooden bridge, jungle pathway.
 * - Butterflies, colorful birds, floating particles, moving forest mist.
 * - Cinematic camera fly-through reveal.
 * - African-inspired background music.
 * - Large glowing "ENTER JUNGLE" button that zooms camera into the game!
 */
@Composable
private fun DreamDashTitleScene(
    isZoomingForward: Boolean,
    soundEffects: SoundEffects,
    onEnterJungle: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jungle_life")

    // Butterfly fluttering movement
    val butterflyTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "butterfly_flight"
    )

    // Mist drift
    val mistOffset by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mist_drift"
    )

    // Button pulse
    val buttonGlow by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_glow"
    )

    // Cinematic Camera Fly-Through entry animation:
    // Starts zoomed out/tilted, smoothly swoops forward into the lush ruins
    val cameraScale = remember { Animatable(1.22f) }
    val cameraOffsetY = remember { Animatable(-30f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(1.10f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Camera fly-through over 1.4s
        launch {
            cameraScale.animateTo(1.0f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
        }
        launch {
            cameraOffsetY.animateTo(0f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
        }
        // Reveal title at 600ms
        delay(500L)
        launch {
            titleAlpha.animateTo(1.0f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        }
        launch {
            titleScale.animateTo(1.0f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        }
        // Reveal ENTER JUNGLE button at 1200ms
        delay(600L)
        launch {
            buttonAlpha.animateTo(1.0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
    }

    // Forward zoom transition when ENTER JUNGLE is tapped
    val diveScale by animateFloatAsState(
        targetValue = if (isZoomingForward) 2.2f else 1.0f,
        animationSpec = tween(600, easing = FastOutLinearInEasing),
        label = "dive_scale"
    )

    val diveAlpha by animateFloatAsState(
        targetValue = if (isZoomingForward) 0f else 1f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "dive_alpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dreamdash_title_scene")
    ) {
        val screenWidth = maxWidth

        // 3D Tropical Jungle Background (featuring river, waterfall, stone ruins, bridge, and canopy)
        Image(
            painter = painterResource(id = R.drawable.img_jungle_ruins_waterfall),
            contentDescription = "Realistic 3D Jungle with waterfall, river, ancient ruins, and canopy",
            modifier = Modifier
                .fillMaxSize()
                .scale(cameraScale.value * diveScale)
                .offset(y = cameraOffsetY.value.dp),
            contentScale = ContentScale.Crop
        )

        // Dynamic 3D Environment Layer: Sunlight God-rays, Moving Mist, Fluttering Butterflies, Birds, Spores
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(diveScale)
        ) {
            val w = size.width
            val h = size.height

            // 1. Moving Atmospheric Forest Mist Layer
            val mistX = w * mistOffset
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    startX = mistX - w * 0.4f,
                    endX = mistX + w * 0.4f
                ),
                topLeft = Offset(0f, h * 0.28f),
                size = Size(w, h * 0.45f)
            )

            // 2. Sunlight Canopy God-Rays
            val sunOrigin = Offset(w * 0.65f, 0f)
            val rayCount = 5
            for (i in 0 until rayCount) {
                val rayAngle = 0.55f + i * 0.18f
                val rayPath = Path().apply {
                    moveTo(sunOrigin.x, sunOrigin.y)
                    val bottomX1 = sunOrigin.x - (h * 0.6f * cos(rayAngle))
                    val bottomX2 = bottomX1 + 90f
                    lineTo(bottomX1, h * 0.85f)
                    lineTo(bottomX2, h * 0.85f)
                    close()
                }
                drawPath(
                    path = rayPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFE082).copy(alpha = 0.22f),
                            Color(0xFFFFD54F).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.85f
                    )
                )
            }

            // 3. Fluttering Colorful Butterflies (Morpho Blue & Golden Swallowtail)
            // Butterfly 1: Blue Morpho
            val b1X = w * (0.25f + sin(butterflyTime) * 0.18f)
            val b1Y = h * (0.50f + cos(butterflyTime * 1.5f) * 0.08f)
            val wingFlap = abs(sin(butterflyTime * 8f))
            drawButterfly(this, Offset(b1X, b1Y), wingFlap, Color(0xFF00E5FF), Color(0xFF0D47A1))

            // Butterfly 2: Golden Swallowtail
            val b2X = w * (0.65f + cos(butterflyTime * 0.8f + 1f) * 0.16f)
            val b2Y = h * (0.62f + sin(butterflyTime * 1.2f + 1f) * 0.06f)
            val wingFlap2 = abs(cos(butterflyTime * 9f))
            drawButterfly(this, Offset(b2X, b2Y), wingFlap2, Color(0xFFFFD700), Color(0xFFFF6D00))

            // 4. Distant Colorful Birds gliding across canopy
            val birdProg = (butterflyTime / 6.283f)
            val birdX = w * (1.1f - birdProg * 1.3f)
            val birdY = h * (0.16f + sin(birdProg * 4f) * 0.02f)
            drawBirdSilhouette(this, Offset(birdX, birdY), Color(0xFF1B3820).copy(alpha = 0.75f))

            // 5. Ambient glowing fireflies / spores
            for (i in 0 until 18) {
                val seed = i * 37
                val px = w * ((seed % 100) / 100f)
                val py = h * ((seed * 7 % 100) / 100f)
                val pColor = if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFF00E676)
                drawCircle(
                    color = pColor.copy(alpha = 0.35f + (i % 3) * 0.15f),
                    radius = 2.5f + (i % 4) * 1.2f,
                    center = Offset(px, py)
                )
            }

            // 6. Cinematic Top & Bottom Vignettes for maximum contrast
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
        }

        // Top Navigation Controls: Presenter Watermark, Audio Mute Toggle, Skip Button
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(diveAlpha),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avenix Technology Watermark Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AVENIX TECHNOLOGY",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sound toggle button
                IconButton(
                    onClick = {
                        soundEffects.soundEnabled = !soundEffects.soundEnabled
                        if (soundEffects.soundEnabled) {
                            soundEffects.startJungleAdventureMusic()
                        } else {
                            soundEffects.stopJungleAdventureMusic()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (soundEffects.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Toggle Audio",
                        tint = if (soundEffects.soundEnabled) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Skip button
                SkipIntroButton(onSkip = onSkip)
            }
        }

        // Center: DREAMDASH Title & Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .alpha(titleAlpha.value * diveAlpha)
                .scale(titleScale.value)
        ) {
            // Main Game Name: DREAMDASH (Large, bold, 3D embossed style)
            Box(contentAlignment = Alignment.Center) {
                // Background Soft Neon Glow
                Text(
                    text = "DREAMDASH",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (screenWidth < 380.dp) 46.sp else 56.sp,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF00E676).copy(alpha = 0.75f),
                    modifier = Modifier
                        .offset(x = 0.dp, y = 4.dp)
                        .blur(10.dp)
                )

                // Foreground Crisp Embossed Text
                Text(
                    text = "DREAMDASH",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (screenWidth < 380.dp) 46.sp else 56.sp,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier
                        .shadow(
                            elevation = 24.dp,
                            spotColor = Color(0xFFFFD700),
                            ambientColor = Color(0xFF00E676)
                        )
                        .testTag("dreamdash_title_heading")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle: THE ULTIMATE JUNGLE ADVENTURE
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xCC071E0E),
                                Color(0xE60D3318),
                                Color(0xCC071E0E)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.5f),
                                Color(0xFFFFD700),
                                Color(0xFF00E676),
                                Color(0xFFFFD700).copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 9.dp)
                    .testTag("jungle_adventure_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌴", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "THE ULTIMATE JUNGLE ADVENTURE",
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (screenWidth < 380.dp) 11.sp else 13.sp,
                        letterSpacing = 2.5.sp,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🌴", fontSize = 14.sp)
                }
            }
        }

        // Bottom: Large Glowing "ENTER JUNGLE" Button (STEP 3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 36.dp, start = 28.dp, end = 28.dp)
                .alpha(buttonAlpha.value * diveAlpha)
        ) {
            Button(
                onClick = onEnterJungle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(buttonGlow)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color(0xFF00E676),
                        ambientColor = Color(0xFFFFD700)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFF00E676),
                                Color(0xFFFFD700)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .testTag("enter_jungle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xDD0A2A14)
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ENTER JUNGLE",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 3.sp,
                            color = Color.White
                        )
                        Text(
                            text = "TAP TO BEGIN EXPEDITION",
                            fontFamily = MontserratFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFFD700).copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * STEP 3 — JUNGLE LOADING SCREEN
 * Displays authentic loading progression into the jungle environment,
 * then opens the existing Main Menu.
 */
@Composable
private fun JungleLoadingScreen(
    onFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var tipIndex by remember { mutableIntStateOf(0) }

    val tips = remember {
        listOf(
            "TIP: Double tap anywhere to activate your high-tech hoverboard!",
            "TIP: Swipe down in mid-air to perform a rapid dive slide under vines!",
            "TIP: Open Mystery Boxes to unlock rare character outfits and pets!",
            "TIP: Collect Golden Tiki Relics to boost your shield and magnet duration!",
            "TIP: Rescue jungle animal companions in the Sanctuary for passive coin bonuses!"
        )
    }

    val loadingStages = remember {
        listOf(
            "Initializing Avenix Jungle Engine...",
            "Loading 3D Character Rigs & Shaders...",
            "Synthesizing Tropical Canopy & Rapids...",
            "Synchronizing Player Artifacts & Relics...",
            "Jungle Expedition Ready — Welcome to DreamDash!"
        )
    }

    val currentStatus = when {
        progress < 0.22f -> loadingStages[0]
        progress < 0.48f -> loadingStages[1]
        progress < 0.72f -> loadingStages[2]
        progress < 0.92f -> loadingStages[3]
        else -> loadingStages[4]
    }

    // Smooth loading curve over ~1.8 seconds
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 1800L
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            val frac = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            progress = frac
            if (frac >= 1f) break
            delay(16L)
        }
        delay(120L)
        onFinished()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(900L)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "load_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jungle_loading_screen")
    ) {
        // Jungle Background with moody focus tint
        Image(
            painter = painterResource(id = R.drawable.img_jungle_ruins_waterfall),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Text(
                text = "AVENIX TECHNOLOGY",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 5.sp,
                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "DREAMDASH",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 42.sp,
                letterSpacing = 3.sp,
                color = Color.White,
                modifier = Modifier.shadow(12.dp, spotColor = Color(0xFF00E676))
            )

            Text(
                text = "THE ULTIMATE JUNGLE ADVENTURE",
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF00E676),
                modifier = Modifier.padding(top = 4.dp, bottom = 36.dp)
            )

            // Status & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentStatus,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Loading Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E676),
                                    Color(0xFFFFD700),
                                    Color(0xFF00E676)
                                )
                            )
                        )
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    val sx = size.width * shimmerOffset
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            startX = sx - 45f,
                            endX = sx + 45f
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Cycling Game Tips Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x80132B1A))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tips[tipIndex],
                        fontFamily = MontserratFontFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SkipIntroButton(
    modifier: Modifier = Modifier,
    onSkip: () -> Unit
) {
    Surface(
        onClick = onSkip,
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier.testTag("skip_intro_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "SKIP",
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Intro",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun drawButterfly(
    drawScope: DrawScope,
    center: Offset,
    wingFlap: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val wingW = 12f * wingFlap
    val wingH = 14f

    // Left Wing
    val leftWing = Path().apply {
        moveTo(center.x, center.y)
        quadraticBezierTo(center.x - wingW * 1.3f, center.y - wingH * 1.1f, center.x - wingW, center.y)
        quadraticBezierTo(center.x - wingW * 0.9f, center.y + wingH * 0.7f, center.x, center.y)
        close()
    }
    drawScope.drawPath(leftWing, primaryColor)

    // Right Wing
    val rightWing = Path().apply {
        moveTo(center.x, center.y)
        quadraticBezierTo(center.x + wingW * 1.3f, center.y - wingH * 1.1f, center.x + wingW, center.y)
        quadraticBezierTo(center.x + wingW * 0.9f, center.y + wingH * 0.7f, center.x, center.y)
        close()
    }
    drawScope.drawPath(rightWing, primaryColor)

    // Body
    drawScope.drawLine(
        color = secondaryColor,
        start = Offset(center.x, center.y - wingH * 0.5f),
        end = Offset(center.x, center.y + wingH * 0.5f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
}

private fun drawBirdSilhouette(
    drawScope: DrawScope,
    center: Offset,
    color: Color
) {
    val birdPath = Path().apply {
        moveTo(center.x - 14f, center.y - 4f)
        quadraticBezierTo(center.x - 7f, center.y - 10f, center.x, center.y)
        quadraticBezierTo(center.x + 7f, center.y - 10f, center.x + 14f, center.y - 4f)
        quadraticBezierTo(center.x + 7f, center.y - 6f, center.x, center.y + 2f)
        quadraticBezierTo(center.x - 7f, center.y - 6f, center.x - 14f, center.y - 4f)
        close()
    }
    drawScope.drawPath(birdPath, color)
}
