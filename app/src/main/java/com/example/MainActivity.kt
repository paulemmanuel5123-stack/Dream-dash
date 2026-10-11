package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GamePhase
import com.example.ui.screens.AnimalGalleryScreen
import com.example.ui.screens.CharacterShopScreen
import com.example.ui.screens.GameOverDialog
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.IntroScreen
import com.example.ui.screens.LeaderboardDialog
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.MysteryBoxesScreen
import com.example.ui.screens.UpgradeShopDialog
import com.example.ui.theme.FantasyDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FantasyDarkBg
                ) {
                    DreamDashApp()
                }
            }
        }
    }
}

@Composable
fun DreamDashApp(
    viewModel: GameViewModel = viewModel()
) {
    val phase by viewModel.gamePhase.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val topRuns by viewModel.topRuns.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()
    val isNewHighScore by viewModel.isNewHighScore.collectAsState()
    val showTactileControls by viewModel.showTactileControls.collectAsState()

    val showShop by viewModel.showShopDialog.collectAsState()
    val showLeaderboard by viewModel.showLeaderboardDialog.collectAsState()
    val missionsProgress by viewModel.missionsProgress.collectAsState()

    BackHandler(enabled = phase != GamePhase.MENU) {
        when (phase) {
            GamePhase.INTRO -> viewModel.finishIntro()
            GamePhase.CHARACTER_SHOP -> viewModel.returnToMenu()
            GamePhase.MYSTERY_BOXES -> viewModel.returnToMenu()
            GamePhase.ANIMAL_GALLERY -> viewModel.returnToMenu()
            GamePhase.MISSIONS -> viewModel.returnToMenu()
            GamePhase.PLAYING -> viewModel.pauseGame()
            GamePhase.PAUSED -> viewModel.returnToMenu()
            GamePhase.GAME_OVER -> viewModel.returnToMenu()
            else -> {}
        }
    }

    when (phase) {
        GamePhase.INTRO -> {
            IntroScreen(
                soundEffects = viewModel.soundEffects,
                onComplete = { viewModel.finishIntro() }
            )
        }

        GamePhase.MENU -> {
            MainMenuScreen(
                profile = profile,
                soundEnabled = viewModel.soundEffects.soundEnabled,
                hapticsEnabled = viewModel.soundEffects.hapticsEnabled,
                onStartGame = { viewModel.startGame() },
                onSelectCharacter = { viewModel.selectCharacter(it) },
                onOpenShop = { viewModel.openCharacterShop() },
                onOpenLeaderboard = { viewModel.showLeaderboardDialog.value = true },
                onOpenMysteryBoxes = { viewModel.openMysteryBoxes() },
                onOpenAnimalGallery = { viewModel.openAnimalGallery() },
                onOpenMissions = { viewModel.openMissions() },
                onToggleSound = { viewModel.toggleSound(it) },
                onToggleHaptics = { viewModel.toggleHaptics(it) }
            )
        }

        GamePhase.MYSTERY_BOXES -> {
            MysteryBoxesScreen(
                profile = profile,
                onOpenBox = { rarity -> viewModel.openMysteryBox(rarity) },
                onBack = { viewModel.returnToMenu() }
            )
        }

        GamePhase.ANIMAL_GALLERY -> {
            AnimalGalleryScreen(
                profile = profile,
                onSelectPreferredAnimal = { viewModel.selectPreferredAnimal(it) },
                onBack = { viewModel.returnToMenu() }
            )
        }

        GamePhase.MISSIONS -> {
            MissionsScreen(
                profile = profile,
                missionsProgress = missionsProgress,
                onClaimReward = { viewModel.claimMissionReward(it) },
                onBack = { viewModel.returnToMenu() }
            )
        }

        GamePhase.CHARACTER_SHOP -> {
            CharacterShopScreen(
                profile = profile,
                onBuyCharacter = { viewModel.buyCharacter(it) },
                onSelectCharacter = { viewModel.selectCharacter(it) },
                onBuyHoverboard = { viewModel.buyHoverboard(it) },
                onSelectHoverboard = { viewModel.selectHoverboard(it) },
                onUpgradeRelic = { viewModel.buyUpgrade(it) },
                onBack = { viewModel.returnToMenu() },
                onStartRunWithSelected = { viewModel.startGame() }
            )
        }

        GamePhase.PLAYING,
        GamePhase.PAUSED,
        GamePhase.GAME_OVER -> {
            Box(modifier = Modifier.fillMaxSize()) {
                activeEngine?.let { engine ->
                    GamePlayScreen(
                        engine = engine,
                        soundEnabled = viewModel.soundEffects.soundEnabled,
                        showTactileControls = showTactileControls,
                        onGameOver = { viewModel.finishRun(it) },
                        onPause = { viewModel.pauseGame() },
                        onResume = { viewModel.resumeGame() },
                        onRestart = { viewModel.startGame() },
                        onToggleSound = { viewModel.toggleSound(it) },
                        onQuit = { viewModel.returnToMenu() }
                    )

                    if (phase == GamePhase.GAME_OVER) {
                        GameOverDialog(
                            engine = engine,
                            isNewHighScore = isNewHighScore,
                            onRestart = { viewModel.startGame() },
                            onHome = { viewModel.returnToMenu() }
                        )
                    }
                }
            }
        }
    }

    if (showShop) {
        UpgradeShopDialog(
            profile = profile,
            onUpgrade = { viewModel.buyUpgrade(it) },
            onDismiss = { viewModel.showShopDialog.value = false }
        )
    }

    if (showLeaderboard) {
        LeaderboardDialog(
            profile = profile,
            runs = topRuns,
            onDismiss = { viewModel.showLeaderboardDialog.value = false }
        )
    }
}
