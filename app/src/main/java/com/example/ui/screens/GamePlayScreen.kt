package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.engine.GameEngine
import com.example.ui.hud.GameHud
import com.example.ui.renderer.GameCanvasRenderer
import com.example.ui.theme.VoidDark
import kotlin.math.abs

@Composable
fun GamePlayScreen(
    engine: GameEngine,
    soundEnabled: Boolean,
    showTactileControls: Boolean,
    onGameOver: (GameEngine) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onQuit: () -> Unit
) {
    val isGameOver by engine.isGameOver.collectAsState()
    val isPaused by engine.isPaused.collectAsState()

    val renderer = remember { GameCanvasRenderer() }
    var totalTime by remember { mutableFloatStateOf(0f) }
    var notifiedGameOver by remember(engine) { mutableStateOf(false) }

    BackHandler {
        if (!isGameOver) {
            if (isPaused) onResume() else onPause()
        }
    }

    // 60FPS Game Loop (continues animating post-crash Game-Over animation when defeated)
    LaunchedEffect(engine, isPaused) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { currentNano ->
                val dt = ((currentNano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                lastNano = currentNano
                if (!isPaused) {
                    totalTime += dt
                    engine.update(dt)
                    if (engine.isGameOver.value && !notifiedGameOver) {
                        notifiedGameOver = true
                        onGameOver(engine)
                    }
                }
            }
        }
    }

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var swipedThisGesture by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .pointerInput(isGameOver, isPaused) {
                if (isGameOver || isPaused) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                        swipedThisGesture = false
                    },
                    onDragEnd = { swipedThisGesture = false },
                    onDragCancel = { swipedThisGesture = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (swipedThisGesture) return@detectDragGestures

                        dragX += dragAmount.x
                        dragY += dragAmount.y

                        val threshold = 35f
                        if (abs(dragX) > threshold || abs(dragY) > threshold) {
                            if (abs(dragX) > abs(dragY)) {
                                if (dragX > 0) engine.switchLane(1) else engine.switchLane(-1)
                            } else {
                                if (dragY < 0) engine.jump() else engine.slide()
                            }
                            swipedThisGesture = true
                        }
                    }
                )
            }
    ) {
        // 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
        ) {
            renderer.render(
                drawScope = this,
                engine = engine,
                timeSeconds = totalTime
            )
        }

        // Top HUD & Optional Tactile Controls
        GameHud(
            engine = engine,
            showTactileButtons = showTactileControls,
            onPause = onPause
        )

        // Pause Overlay
        if (isPaused && !isGameOver) {
            PauseOverlay(
                soundEnabled = soundEnabled,
                onResume = onResume,
                onRestart = onRestart,
                onToggleSound = onToggleSound,
                onQuit = onQuit
            )
        }
    }
}
