package com.example.ui.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.model.CharacterAnimState
import com.example.model.CharacterInfo
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-body 3D-styled articulated character renderer for all 6 original DreamDash heroes:
 * 1. Hunter Boy (Kael)
 * 2. Fantasy Adventurer Girl (Lyra)
 * 3. Shadow Runner (Vex)
 * 4. Crystal Warrior (Seraphina)
 * 5. Forest Guardian (Thorne)
 * 6. Sky Runner (Zephyr)
 *
 * Supports all 7 animations:
 * IDLE, RUNNING, JUMPING, LANDING, SLIDING, LANE_CHANGE, GAME_OVER.
 */
object Character3DRenderer {

    fun drawCharacter(
        drawScope: DrawScope,
        character: CharacterInfo,
        animState: CharacterAnimState,
        timeSeconds: Float,
        centerX: Float,
        groundY: Float,
        scale: Float = 1f,
        elevationPx: Float = 0f,
        laneTiltDir: Float = 0f,
        frontFacingFactor: Float = 0.35f
    ) {
        val baseY = groundY - elevationPx

        // 1. Ground Shadow & Magical Pedestal Ring
        val shadowScale = (1f - (elevationPx / (220f * scale)).coerceIn(0f, 0.65f))
        val shadowW = 74f * scale * shadowScale * (if (animState == CharacterAnimState.SLIDING) 1.3f else 1f)
        val shadowH = 18f * scale * shadowScale
        drawScope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.52f * shadowScale),
                    character.primaryColor.copy(alpha = 0.22f * shadowScale),
                    Color.Transparent
                ),
                center = Offset(centerX, groundY - 4f * scale),
                radius = shadowW * 0.7f
            ),
            topLeft = Offset(centerX - shadowW * 0.65f, groundY - shadowH * 0.6f),
            size = Size(shadowW * 1.3f, shadowH * 1.2f)
        )

        // Landing shockwave ring effect
        if (animState == CharacterAnimState.LANDING) {
            val landPulse = (timeSeconds * 4.5f) % 1f
            val ringW = (65f + landPulse * 55f) * scale
            val ringH = (16f + landPulse * 14f) * scale
            drawScope.drawOval(
                color = character.primaryColor.copy(alpha = (1f - landPulse) * 0.65f),
                topLeft = Offset(centerX - ringW / 2f, groundY - ringH / 2f),
                size = Size(ringW, ringH),
                style = Stroke(width = 3f * scale)
            )
        }

        // Lane-Change 3D Dodge Afterimages
        if (animState == CharacterAnimState.LANE_CHANGE) {
            val dir = if (abs(laneTiltDir) > 0.05f) laneTiltDir else sin(timeSeconds * 6f)
            for (ghostIdx in 2 downTo 1) {
                val offsetX = -dir * ghostIdx * 18f * scale
                drawScope.withTransform({
                    translate(left = offsetX, top = 0f)
                }) {
                    drawGhostSilhouette(
                        drawScope = this,
                        character = character,
                        centerX = centerX,
                        baseY = baseY,
                        scale = scale,
                        alpha = 0.18f / ghostIdx
                    )
                }
            }
        }

        // 2. Compute 7-State Articulated Pose Kinematics
        val pose = computePose(
            animState = animState,
            time = timeSeconds,
            scale = scale,
            laneTiltDir = laneTiltDir,
            frontFacingFactor = frontFacingFactor
        )

        drawScope.withTransform({
            translate(left = pose.bodyOffsetX, top = pose.bodyOffsetY)
            rotate(degrees = pose.bodyTiltDeg, pivot = Offset(centerX, baseY - 48f * scale))
        }) {
            val hipX = centerX
            val hipY = baseY - (56f * scale) + pose.hipDropY
            val torsoTopX = hipX + pose.torsoLeanX
            val torsoTopY = hipY - (42f * scale) + pose.chestBreathY
            val headX = torsoTopX + pose.headOffsetX
            val headY = torsoTopY - (21f * scale) + pose.headBobY

            // --- BACK LAYER (Behind Torso when facing front-quarter) ---
            if (frontFacingFactor >= 0.5f) {
                drawBackEquipmentAndHairBehind(
                    drawScope = this,
                    character = character,
                    animState = animState,
                    time = timeSeconds,
                    hipX = hipX,
                    hipY = hipY,
                    torsoTopX = torsoTopX,
                    torsoTopY = torsoTopY,
                    headX = headX,
                    headY = headY,
                    scale = scale
                )
            }

            // --- BACK ARM ---
            drawArticulatedArm(
                drawScope = this,
                character = character,
                isFrontArm = false,
                shoulderX = torsoTopX + (14f * scale * pose.shoulderTurn),
                shoulderY = torsoTopY + 7f * scale,
                swingAngleDeg = pose.rightArmSwingDeg,
                elbowBendDeg = pose.rightElbowBendDeg,
                scale = scale
            )

            // --- LEGS & FANTASY BOOTS ---
            drawArticulatedLegAndBoot(
                drawScope = this,
                character = character,
                isFrontLeg = false,
                hipX = hipX + 9f * scale,
                hipY = hipY,
                thighAngleDeg = pose.rightThighDeg,
                kneeBendDeg = pose.rightKneeDeg,
                bootAngleDeg = pose.rightBootDeg,
                scale = scale
            )

            drawArticulatedLegAndBoot(
                drawScope = this,
                character = character,
                isFrontLeg = true,
                hipX = hipX - 9f * scale,
                hipY = hipY,
                thighAngleDeg = pose.leftThighDeg,
                kneeBendDeg = pose.leftKneeDeg,
                bootAngleDeg = pose.leftBootDeg,
                scale = scale
            )

            // --- TORSO, OUTFIT, ARMOR & BELT ACCESSORIES ---
            drawTorsoAndOutfit(
                drawScope = this,
                character = character,
                animState = animState,
                time = timeSeconds,
                hipX = hipX,
                hipY = hipY,
                torsoTopX = torsoTopX,
                torsoTopY = torsoTopY,
                scale = scale
            )

            // --- FRONT ARM ---
            drawArticulatedArm(
                drawScope = this,
                character = character,
                isFrontArm = true,
                shoulderX = torsoTopX - (15f * scale * pose.shoulderTurn),
                shoulderY = torsoTopY + 7f * scale,
                swingAngleDeg = pose.leftArmSwingDeg,
                elbowBendDeg = pose.leftElbowBendDeg,
                scale = scale
            )

            // --- HEAD, HAIRSTYLE, HELM/CROWN & CONFIDENT FACIAL EXPRESSION ---
            drawHeadHairAndExpression(
                drawScope = this,
                character = character,
                animState = animState,
                time = timeSeconds,
                headX = headX,
                headY = headY,
                scale = scale,
                frontFacingFactor = frontFacingFactor
            )

            // --- FOREGROUND BACK GEAR (When in Gameplay Runner Over-the-Shoulder View) ---
            if (frontFacingFactor < 0.5f) {
                drawBackEquipmentAndHairBehind(
                    drawScope = this,
                    character = character,
                    animState = animState,
                    time = timeSeconds,
                    hipX = hipX,
                    hipY = hipY,
                    torsoTopX = torsoTopX,
                    torsoTopY = torsoTopY,
                    headX = headX,
                    headY = headY,
                    scale = scale
                )
            }

            // --- FLOATING MAGICAL COMPANION / RELIC ---
            drawFloatingMagicalAccessory(
                drawScope = this,
                character = character,
                time = timeSeconds,
                torsoTopX = torsoTopX,
                torsoTopY = torsoTopY,
                headX = headX,
                headY = headY,
                scale = scale,
                animState = animState
            )

            // --- SPECIAL ANIMATION OVERLAYS ---
            drawAnimationEffects(
                drawScope = this,
                character = character,
                animState = animState,
                time = timeSeconds,
                centerX = centerX,
                baseY = baseY,
                headX = headX,
                headY = headY,
                scale = scale
            )
        }
    }

    private fun isBoyStyle(id: String): Boolean {
        return id in setOf("kael", "shadow_runner", "forest_guardian")
    }

    private data class RigPose(
        val bodyOffsetX: Float = 0f,
        val bodyOffsetY: Float = 0f,
        val bodyTiltDeg: Float = 0f,
        val hipDropY: Float = 0f,
        val torsoLeanX: Float = 0f,
        val chestBreathY: Float = 0f,
        val headOffsetX: Float = 0f,
        val headBobY: Float = 0f,
        val shoulderTurn: Float = 1f,
        val leftThighDeg: Float = 0f,
        val leftKneeDeg: Float = 0f,
        val leftBootDeg: Float = 0f,
        val rightThighDeg: Float = 0f,
        val rightKneeDeg: Float = 0f,
        val rightBootDeg: Float = 0f,
        val leftArmSwingDeg: Float = 0f,
        val leftElbowBendDeg: Float = 0f,
        val rightArmSwingDeg: Float = 0f,
        val rightElbowBendDeg: Float = 0f
    )

    private fun computePose(
        animState: CharacterAnimState,
        time: Float,
        scale: Float,
        laneTiltDir: Float,
        frontFacingFactor: Float
    ): RigPose {
        return when (animState) {
            CharacterAnimState.IDLE -> {
                val breath = sin(time * 2.7f)
                val sway = cos(time * 1.4f)
                RigPose(
                    bodyOffsetX = sway * 2.5f * scale,
                    bodyOffsetY = 0f,
                    bodyTiltDeg = sway * 1.8f,
                    hipDropY = abs(breath) * 2.5f * scale,
                    torsoLeanX = sway * 2f * scale,
                    chestBreathY = -breath * 3.2f * scale,
                    headOffsetX = sway * 1.5f * scale,
                    headBobY = -breath * 2.2f * scale,
                    shoulderTurn = 0.95f,
                    leftThighDeg = -6f + breath * 2f,
                    leftKneeDeg = 8f,
                    leftBootDeg = 0f,
                    rightThighDeg = 8f - breath * 2f,
                    rightKneeDeg = 10f,
                    rightBootDeg = 0f,
                    leftArmSwingDeg = -16f + breath * 5f,
                    leftElbowBendDeg = -28f,
                    rightArmSwingDeg = 18f - breath * 6f,
                    rightElbowBendDeg = -36f
                )
            }

            CharacterAnimState.RUNNING -> {
                val phase = time * 11.5f
                val stride = sin(phase)
                val counter = cos(phase)
                val bounce = -abs(sin(phase)) * 9f * scale
                val bankDeg = (laneTiltDir * 16f).coerceIn(-20f, 20f)
                RigPose(
                    bodyOffsetX = 0f,
                    bodyOffsetY = bounce,
                    bodyTiltDeg = bankDeg + (if (frontFacingFactor > 0.5f) 6f else 3f),
                    hipDropY = 3f * scale,
                    torsoLeanX = 5f * scale + stride * 2f * scale,
                    chestBreathY = counter * 2f * scale,
                    headOffsetX = 3f * scale,
                    headBobY = abs(cos(phase)) * 3f * scale,
                    shoulderTurn = 0.85f + stride * 0.15f,
                    leftThighDeg = stride * 42f,
                    leftKneeDeg = (if (stride < 0f) -stride * 58f else 18f),
                    leftBootDeg = stride * 18f,
                    rightThighDeg = -stride * 42f,
                    rightKneeDeg = (if (-stride < 0f) stride * 58f else 18f),
                    rightBootDeg = -stride * 18f,
                    leftArmSwingDeg = -stride * 48f,
                    leftElbowBendDeg = -52f - abs(stride) * 15f,
                    rightArmSwingDeg = stride * 48f,
                    rightElbowBendDeg = -52f - abs(stride) * 15f
                )
            }

            CharacterAnimState.JUMPING -> {
                val hover = sin(time * 6f) * 4f * scale
                val previewArc = if (frontFacingFactor > 0.5f) -18f * scale + hover else hover
                RigPose(
                    bodyOffsetX = 0f,
                    bodyOffsetY = previewArc,
                    bodyTiltDeg = -8f + (laneTiltDir * 12f),
                    hipDropY = -6f * scale,
                    torsoLeanX = 4f * scale,
                    chestBreathY = -4f * scale,
                    headOffsetX = 2f * scale,
                    headBobY = -4f * scale,
                    shoulderTurn = 0.95f,
                    leftThighDeg = -48f,
                    leftKneeDeg = 68f,
                    leftBootDeg = -22f,
                    rightThighDeg = 32f,
                    rightKneeDeg = 42f,
                    rightBootDeg = 28f,
                    leftArmSwingDeg = -115f + sin(time * 8f) * 8f,
                    leftElbowBendDeg = -25f,
                    rightArmSwingDeg = 110f - cos(time * 8f) * 8f,
                    rightElbowBendDeg = -25f
                )
            }

            CharacterAnimState.LANDING -> {
                val pulse = abs(sin(time * 7f))
                val crouch = (14f + pulse * 6f) * scale
                RigPose(
                    bodyOffsetX = 0f,
                    bodyOffsetY = 0f,
                    bodyTiltDeg = 12f,
                    hipDropY = crouch,
                    torsoLeanX = 9f * scale,
                    chestBreathY = 5f * scale,
                    headOffsetX = 5f * scale,
                    headBobY = 4f * scale,
                    shoulderTurn = 1f,
                    leftThighDeg = -38f,
                    leftKneeDeg = 64f,
                    leftBootDeg = -14f,
                    rightThighDeg = 28f,
                    rightKneeDeg = 54f,
                    rightBootDeg = -10f,
                    leftArmSwingDeg = -58f,
                    leftElbowBendDeg = -22f,
                    rightArmSwingDeg = 64f,
                    rightElbowBendDeg = -20f
                )
            }

            CharacterAnimState.SLIDING -> {
                val shimmer = sin(time * 18f) * 1.5f * scale
                RigPose(
                    bodyOffsetX = 0f,
                    bodyOffsetY = shimmer,
                    bodyTiltDeg = -38f,
                    hipDropY = 30f * scale,
                    torsoLeanX = -10f * scale,
                    chestBreathY = 4f * scale,
                    headOffsetX = 10f * scale,
                    headBobY = 6f * scale,
                    shoulderTurn = 0.9f,
                    leftThighDeg = -58f,
                    leftKneeDeg = 12f,
                    leftBootDeg = -25f,
                    rightThighDeg = 24f,
                    rightKneeDeg = 82f,
                    rightBootDeg = 35f,
                    leftArmSwingDeg = 42f,
                    leftElbowBendDeg = -15f,
                    rightArmSwingDeg = -68f,
                    rightElbowBendDeg = -30f
                )
            }

            CharacterAnimState.LANE_CHANGE -> {
                val rawDir = if (abs(laneTiltDir) > 0.05f) laneTiltDir else sin(time * 5.5f)
                val dir = if (rawDir >= 0f) 1f else -1f
                val hop = -abs(cos(time * 10f)) * 8f * scale
                RigPose(
                    bodyOffsetX = rawDir * 12f * scale,
                    bodyOffsetY = hop,
                    bodyTiltDeg = dir * 22f,
                    hipDropY = 2f * scale,
                    torsoLeanX = dir * 8f * scale,
                    chestBreathY = -2f * scale,
                    headOffsetX = dir * 5f * scale,
                    headBobY = 0f,
                    shoulderTurn = 0.92f,
                    leftThighDeg = -dir * 36f,
                    leftKneeDeg = 32f,
                    leftBootDeg = -dir * 16f,
                    rightThighDeg = dir * 40f,
                    rightKneeDeg = 26f,
                    rightBootDeg = dir * 18f,
                    leftArmSwingDeg = -65f,
                    leftElbowBendDeg = -34f,
                    rightArmSwingDeg = 65f,
                    rightElbowBendDeg = -34f
                )
            }

            CharacterAnimState.GAME_OVER -> {
                val sway = sin(time * 4.5f)
                RigPose(
                    bodyOffsetX = sway * 3.5f * scale,
                    bodyOffsetY = 0f,
                    bodyTiltDeg = 8f + sway * 4f,
                    hipDropY = 24f * scale,
                    torsoLeanX = 6f * scale + sway * 3f * scale,
                    chestBreathY = 5f * scale,
                    headOffsetX = sway * 4f * scale,
                    headBobY = 6f * scale,
                    shoulderTurn = 0.95f,
                    leftThighDeg = -42f,
                    leftKneeDeg = 78f,
                    leftBootDeg = 10f,
                    rightThighDeg = 30f,
                    rightKneeDeg = 85f,
                    rightBootDeg = 35f,
                    leftArmSwingDeg = -82f + sway * 10f,
                    leftElbowBendDeg = -75f,
                    rightArmSwingDeg = 35f,
                    rightElbowBendDeg = -20f
                )
            }
        }
    }

    private fun drawArticulatedLegAndBoot(
        drawScope: DrawScope,
        character: CharacterInfo,
        isFrontLeg: Boolean,
        hipX: Float,
        hipY: Float,
        thighAngleDeg: Float,
        kneeBendDeg: Float,
        bootAngleDeg: Float,
        scale: Float
    ) {
        val thighLen = 23f * scale
        val calfLen = 22f * scale
        val thighRad = (thighAngleDeg * PI / 180.0).toFloat()
        val kneeX = hipX + sin(thighRad) * thighLen
        val kneeY = hipY + cos(thighRad) * thighLen

        val calfDeg = thighAngleDeg + kneeBendDeg * 0.65f
        val calfRad = (calfDeg * PI / 180.0).toFloat()
        val ankleX = kneeX + sin(calfRad) * calfLen
        val ankleY = kneeY + cos(calfRad) * calfLen

        val shade = if (isFrontLeg) 1f else 0.78f

        val basePants = when (character.id) {
            "kael" -> Color(0xFF2D3B32)
            "lyra" -> Color(0xFF231F48)
            "shadow_runner" -> Color(0xFF1A122B)
            "crystal_warrior" -> Color(0xFF153252)
            "forest_guardian" -> Color(0xFF1E3524)
            "sky_runner" -> Color(0xFF2A344B)
            else -> Color(0xFF2D3B32)
        }
        val pantsColor = Color(basePants.red * shade, basePants.green * shade, basePants.blue * shade, 1f)

        drawScope.drawLine(
            color = pantsColor,
            start = Offset(hipX, hipY),
            end = Offset(kneeX, kneeY),
            strokeWidth = 11.5f * scale,
            cap = StrokeCap.Round
        )

        drawScope.drawCircle(
            color = character.accentColor,
            radius = 5.5f * scale,
            center = Offset(kneeX, kneeY)
        )

        drawScope.drawLine(
            color = pantsColor,
            start = Offset(kneeX, kneeY),
            end = Offset(ankleX, ankleY),
            strokeWidth = 9.5f * scale,
            cap = StrokeCap.Round
        )

        val baseBoot = when (character.id) {
            "kael" -> Color(0xFF5D4037)
            "lyra" -> Color(0xFF311B92)
            "shadow_runner" -> Color(0xFF24143D)
            "crystal_warrior" -> Color(0xFF0277BD)
            "forest_guardian" -> Color(0xFF3E2723)
            "sky_runner" -> Color(0xFFF57F17)
            else -> Color(0xFF5D4037)
        }
        val bootMainColor = Color(baseBoot.red * shade, baseBoot.green * shade, baseBoot.blue * shade, 1f)
        val bootCuffColor = character.primaryColor
        val buckleColor = character.secondaryColor

        val midCalfX = (kneeX + ankleX) * 0.5f
        val midCalfY = (kneeY + ankleY) * 0.5f

        drawScope.drawLine(
            color = bootMainColor,
            start = Offset(midCalfX, midCalfY),
            end = Offset(ankleX, ankleY),
            strokeWidth = 11f * scale,
            cap = StrokeCap.Round
        )

        drawScope.drawRoundRect(
            color = bootCuffColor,
            topLeft = Offset(midCalfX - 6.5f * scale, midCalfY - 3f * scale),
            size = Size(13f * scale, 5.5f * scale),
            cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale)
        )

        val toeDir = if (bootAngleDeg >= -5f) 1f else 0.7f
        val footW = 16.5f * scale
        val footH = 8.5f * scale
        val footLeft = ankleX - 5f * scale
        val footTop = ankleY - 3f * scale

        drawScope.drawRoundRect(
            color = bootMainColor,
            topLeft = Offset(footLeft, footTop),
            size = Size(footW * toeDir.coerceAtLeast(0.85f), footH),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )

        drawScope.drawRoundRect(
            color = Color(0xFF1E1410),
            topLeft = Offset(footLeft - 1f * scale, footTop + footH - 2.8f * scale),
            size = Size(footW * toeDir.coerceAtLeast(0.85f) + 2f * scale, 3.2f * scale),
            cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale)
        )

        drawScope.drawCircle(
            color = buckleColor,
            radius = 2.4f * scale,
            center = Offset(ankleX + 1.5f * scale, ankleY - 1f * scale)
        )
    }

    private fun drawArticulatedArm(
        drawScope: DrawScope,
        character: CharacterInfo,
        isFrontArm: Boolean,
        shoulderX: Float,
        shoulderY: Float,
        swingAngleDeg: Float,
        elbowBendDeg: Float,
        scale: Float
    ) {
        val upperLen = 17f * scale
        val foreLen = 16f * scale

        val upperRad = (swingAngleDeg * PI / 180.0).toFloat()
        val elbowX = shoulderX + sin(upperRad) * upperLen
        val elbowY = shoulderY + cos(upperRad) * upperLen

        val foreRad = ((swingAngleDeg + elbowBendDeg) * PI / 180.0).toFloat()
        val handX = elbowX + sin(foreRad) * foreLen
        val handY = elbowY + cos(foreRad) * foreLen

        val shade = if (isFrontArm) 1f else 0.82f
        val baseSleeve = when (character.id) {
            "kael" -> Color(0xFF1B5E20)
            "lyra" -> Color(0xFF1A237E)
            "shadow_runner" -> Color(0xFF311B92)
            "crystal_warrior" -> Color(0xFF01579B)
            "forest_guardian" -> Color(0xFF2E7D32)
            "sky_runner" -> Color(0xFFFFB300)
            else -> Color(0xFF1B5E20)
        }
        val sleeveColor = Color(baseSleeve.red * shade, baseSleeve.green * shade, baseSleeve.blue * shade, 1f)

        drawScope.drawLine(
            color = sleeveColor,
            start = Offset(shoulderX, shoulderY),
            end = Offset(elbowX, elbowY),
            strokeWidth = 9.2f * scale,
            cap = StrokeCap.Round
        )

        // Shoulder Pauldron
        if (isFrontArm) {
            drawScope.drawOval(
                color = character.accentColor,
                topLeft = Offset(shoulderX - 8f * scale, shoulderY - 5f * scale),
                size = Size(15f * scale, 11f * scale)
            )
            drawScope.drawCircle(
                color = character.primaryColor,
                radius = 2.6f * scale,
                center = Offset(shoulderX - 1f * scale, shoulderY)
            )
        }

        drawScope.drawLine(
            color = character.primaryColor.copy(alpha = 0.88f),
            start = Offset(elbowX, elbowY),
            end = Offset(handX, handY),
            strokeWidth = 8f * scale,
            cap = StrokeCap.Round
        )

        val gloveColor = if (isBoyStyle(character.id)) Color(0xFF3E2723) else Color(0xFFE0F7FA)
        drawScope.drawCircle(
            color = gloveColor,
            radius = 5.2f * scale,
            center = Offset(handX, handY)
        )
        drawScope.drawCircle(
            color = character.secondaryColor,
            radius = 2f * scale,
            center = Offset(handX, handY)
        )
    }

    private fun drawTorsoAndOutfit(
        drawScope: DrawScope,
        character: CharacterInfo,
        animState: CharacterAnimState,
        time: Float,
        hipX: Float,
        hipY: Float,
        torsoTopX: Float,
        torsoTopY: Float,
        scale: Float
    ) {
        val torsoW = 34f * scale
        val waistW = 27f * scale
        val boyStyle = isBoyStyle(character.id)

        if (boyStyle) {
            // Split Ranger / Guardian / Shadow Tunic Coattails
            val flutter = sin(time * 9f) * 4f * scale
            val tailPath = Path().apply {
                moveTo(hipX - waistW / 2f, hipY - 4f * scale)
                lineTo(hipX + waistW / 2f, hipY - 4f * scale)
                lineTo(hipX + waistW * 0.62f + flutter, hipY + 14f * scale)
                lineTo(hipX + 2f * scale, hipY + 5f * scale)
                lineTo(hipX - waistW * 0.62f + flutter * 0.5f, hipY + 14f * scale)
                close()
            }
            drawScope.drawPath(
                path = tailPath,
                brush = Brush.verticalGradient(
                    colors = listOf(character.accentColor, character.primaryColor),
                    startY = hipY - 4f * scale,
                    endY = hipY + 14f * scale
                )
            )

            // 3D Shaded Tunic Torso
            val torsoPath = Path().apply {
                moveTo(torsoTopX - torsoW / 2f, torsoTopY)
                lineTo(torsoTopX + torsoW / 2f, torsoTopY)
                lineTo(hipX + waistW / 2f, hipY)
                lineTo(hipX - waistW / 2f, hipY)
                close()
            }
            drawScope.drawPath(
                path = torsoPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(character.accentColor, character.primaryColor, character.secondaryColor, character.accentColor),
                    startX = torsoTopX - torsoW / 2f,
                    endX = torsoTopX + torsoW / 2f
                )
            )

            // Diagonal Bandolier / Chest Strap
            drawScope.drawLine(
                color = Color(0xFF3E2723),
                start = Offset(torsoTopX - torsoW * 0.38f, torsoTopY + 3f * scale),
                end = Offset(hipX + waistW * 0.38f, hipY - 3f * scale),
                strokeWidth = 5.5f * scale,
                cap = StrokeCap.Round
            )

            // Chest Emblem / Compass
            val compassX = (torsoTopX + hipX) * 0.5f - 2f * scale
            val compassY = (torsoTopY + hipY) * 0.46f
            drawScope.drawCircle(
                color = character.secondaryColor,
                radius = 5f * scale,
                center = Offset(compassX, compassY)
            )
            drawScope.drawCircle(
                color = character.primaryColor,
                radius = 2.6f * scale,
                center = Offset(compassX, compassY)
            )

            // Utility Waist Belt & Buckle
            drawScope.drawRoundRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(hipX - waistW * 0.54f, hipY - 5f * scale),
                size = Size(waistW * 1.08f, 7f * scale),
                cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale)
            )
            drawScope.drawRoundRect(
                color = character.secondaryColor,
                topLeft = Offset(hipX - 4f * scale, hipY - 5.5f * scale),
                size = Size(8f * scale, 8f * scale),
                cornerRadius = CornerRadius(2f * scale, 2f * scale)
            )
        } else {
            // Girl Adventurer / Crystal Warrior / Sky Runner Silhouette
            val sway = sin(time * 8.5f) * 6f * scale

            val sashPath = Path().apply {
                moveTo(hipX - waistW * 0.45f, hipY)
                quadraticTo(
                    hipX - waistW * 0.8f + sway,
                    hipY + 14f * scale,
                    hipX - waistW * 0.65f + sway * 1.3f,
                    hipY + 25f * scale
                )
                lineTo(hipX - waistW * 0.35f + sway, hipY + 22f * scale)
                close()
            }
            drawScope.drawPath(
                path = sashPath,
                brush = Brush.verticalGradient(
                    colors = listOf(character.primaryColor, character.secondaryColor.copy(alpha = 0.85f)),
                    startY = hipY,
                    endY = hipY + 25f * scale
                )
            )

            val skortPath = Path().apply {
                moveTo(hipX - waistW * 0.5f, hipY - 3f * scale)
                lineTo(hipX + waistW * 0.5f, hipY - 3f * scale)
                lineTo(hipX + waistW * 0.64f, hipY + 12f * scale)
                lineTo(hipX - waistW * 0.64f, hipY + 12f * scale)
                close()
            }
            drawScope.drawPath(
                path = skortPath,
                brush = Brush.verticalGradient(
                    colors = listOf(character.accentColor, Color(0xFF1A237E)),
                    startY = hipY - 3f * scale,
                    endY = hipY + 12f * scale
                )
            )
            drawScope.drawLine(
                color = AstralGold,
                start = Offset(hipX - waistW * 0.6f, hipY + 10.5f * scale),
                end = Offset(hipX + waistW * 0.6f, hipY + 10.5f * scale),
                strokeWidth = 2f * scale
            )

            val bodicePath = Path().apply {
                moveTo(torsoTopX - torsoW * 0.48f, torsoTopY)
                lineTo(torsoTopX + torsoW * 0.48f, torsoTopY)
                lineTo(hipX + waistW * 0.48f, hipY)
                lineTo(hipX - waistW * 0.48f, hipY)
                close()
            }
            drawScope.drawPath(
                path = bodicePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(character.accentColor, character.primaryColor, character.secondaryColor, character.accentColor),
                    startX = torsoTopX - torsoW * 0.5f,
                    endX = torsoTopX + torsoW * 0.5f
                )
            )

            val amuletX = torsoTopX
            val amuletY = torsoTopY + 10f * scale
            drawScope.drawCircle(color = AstralGold, radius = 5.2f * scale, center = Offset(amuletX, amuletY))
            drawScope.drawCircle(color = character.primaryColor, radius = 3.5f * scale, center = Offset(amuletX, amuletY))

            drawScope.drawRoundRect(
                color = AstralGold,
                topLeft = Offset(hipX - waistW * 0.52f, hipY - 3.5f * scale),
                size = Size(waistW * 1.04f, 4.5f * scale),
                cornerRadius = CornerRadius(2f * scale, 2f * scale)
            )
        }
    }

    private fun drawBackEquipmentAndHairBehind(
        drawScope: DrawScope,
        character: CharacterInfo,
        animState: CharacterAnimState,
        time: Float,
        hipX: Float,
        hipY: Float,
        torsoTopX: Float,
        torsoTopY: Float,
        headX: Float,
        headY: Float,
        scale: Float
    ) {
        val bounce = sin(time * 10f) * 2.5f * scale
        val packX = torsoTopX - 2f * scale
        val packY = torsoTopY + 8f * scale + bounce * 0.5f

        when (character.id) {
            "kael" -> {
                // Bow + Quiver + Small Adventure Backpack
                val bowPath = Path().apply {
                    moveTo(packX - 24f * scale, packY + 30f * scale)
                    quadraticTo(packX + 6f * scale, packY - 4f * scale, packX + 26f * scale, packY - 22f * scale)
                }
                drawScope.drawPath(bowPath, color = Color(0xFF8D6E63), style = Stroke(width = 5f * scale, cap = StrokeCap.Round))
                drawScope.drawCircle(color = AstralGold, radius = 3.2f * scale, center = Offset(packX + 26f * scale, packY - 22f * scale))
                drawScope.drawCircle(color = AstralGold, radius = 3.2f * scale, center = Offset(packX - 24f * scale, packY + 30f * scale))
                drawScope.drawLine(
                    color = StarlightCyan.copy(alpha = 0.75f),
                    start = Offset(packX + 26f * scale, packY - 22f * scale),
                    end = Offset(packX - 24f * scale, packY + 30f * scale),
                    strokeWidth = 1.6f * scale
                )

                val quiverTopX = torsoTopX + 15f * scale
                val quiverTopY = torsoTopY - 6f * scale + bounce * 0.4f
                drawScope.drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(quiverTopX, quiverTopY),
                    end = Offset(packX + 2f * scale, packY + 20f * scale),
                    strokeWidth = 9.5f * scale,
                    cap = StrokeCap.Round
                )
                for (a in -1..1) {
                    val ax = quiverTopX + a * 3.2f * scale
                    val ay = quiverTopY - 9f * scale + abs(a) * 2f * scale
                    drawScope.drawLine(Color(0xFFD7CCC8), Offset(quiverTopX + a * 1.5f * scale, quiverTopY), Offset(ax, ay), 2f * scale)
                    drawScope.drawCircle(color = DreamEmerald, radius = 2.6f * scale, center = Offset(ax, ay))
                }

                val bagW = 24f * scale
                val bagH = 26f * scale
                drawScope.drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF795548), Color(0xFF4E342E)), packY, packY + bagH),
                    topLeft = Offset(packX - bagW / 2f, packY),
                    size = Size(bagW, bagH),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )
                drawScope.drawCircle(color = AstralGold, radius = 2.6f * scale, center = Offset(packX, packY + 9.5f * scale))
            }

            "shadow_runner" -> {
                // Twin Crossed Amethyst Shadow-Blades + Flowing Twilight Scarf
                for (dir in listOf(-1f, 1f)) {
                    drawScope.drawLine(
                        color = MagicPurpleLight,
                        start = Offset(packX - dir * 22f * scale, packY + 26f * scale),
                        end = Offset(packX + dir * 24f * scale, packY - 22f * scale),
                        strokeWidth = 4.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawCircle(
                        color = Color(0xFFE040FB),
                        radius = 3.5f * scale,
                        center = Offset(packX + dir * 24f * scale, packY - 22f * scale)
                    )
                }
                val scarfWave = sin(time * 9.5f) * 12f * scale
                val scarfPath = Path().apply {
                    moveTo(torsoTopX, torsoTopY + 4f * scale)
                    quadraticTo(
                        torsoTopX + 24f * scale + scarfWave,
                        torsoTopY + 18f * scale,
                        torsoTopX + 34f * scale + scarfWave * 1.3f,
                        torsoTopY + 40f * scale
                    )
                }
                drawScope.drawPath(scarfPath, color = Color(0xFFE040FB), style = Stroke(width = 6f * scale, cap = StrokeCap.Round))
            }

            "crystal_warrior" -> {
                // Glowing Starlight Crystal Greatsword on Back + Braided Ponytail
                drawScope.drawLine(
                    color = Color(0xFF80D8FF),
                    start = Offset(packX - 18f * scale, packY + 34f * scale),
                    end = Offset(packX + 24f * scale, packY - 28f * scale),
                    strokeWidth = 6.5f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = AstralGold,
                    start = Offset(packX + 12f * scale, packY - 22f * scale),
                    end = Offset(packX + 26f * scale, packY - 12f * scale),
                    strokeWidth = 4f * scale,
                    cap = StrokeCap.Round
                )
                drawBraidedPonytail(drawScope, headX, headY, time, scale, animState, Color(0xFFB3E5FC), StarlightCyan)
            }

            "forest_guardian" -> {
                // Blossoming Heartwood Nature Staff with Glowing Emerald-Amber Orb
                drawScope.drawLine(
                    color = Color(0xFF6D4C41),
                    start = Offset(packX - 20f * scale, packY + 32f * scale),
                    end = Offset(packX + 22f * scale, packY - 26f * scale),
                    strokeWidth = 5.5f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawCircle(
                    color = DreamEmerald,
                    radius = 8f * scale,
                    center = Offset(packX + 24f * scale, packY - 28f * scale)
                )
                drawScope.drawCircle(
                    color = AstralGold,
                    radius = 4.2f * scale,
                    center = Offset(packX + 24f * scale, packY - 28f * scale)
                )
            }

            "sky_runner" -> {
                // Luminous Aether Feather Wings on Back!
                val wingFlap = sin(time * 8f) * 8f * scale
                for (side in listOf(-1f, 1f)) {
                    val wingPath = Path().apply {
                        moveTo(packX, packY + 6f * scale)
                        quadraticTo(
                            packX + side * 34f * scale,
                            packY - 26f * scale + wingFlap,
                            packX + side * 42f * scale,
                            packY + 4f * scale + wingFlap
                        )
                        quadraticTo(
                            packX + side * 22f * scale,
                            packY + 22f * scale,
                            packX,
                            packY + 14f * scale
                        )
                        close()
                    }
                    drawScope.drawPath(
                        path = wingPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(AstralGold, StarlightCyan.copy(alpha = 0.85f)),
                            startY = packY - 24f * scale,
                            endY = packY + 24f * scale
                        )
                    )
                }
                drawBraidedPonytail(drawScope, headX, headY, time, scale, animState, Color(0xFFFFF8E1), AstralGold)
            }

            else -> {
                // Lyra's Braided High Ponytail & Starlight Hairpin
                drawBraidedPonytail(drawScope, headX, headY, time, scale, animState, Color(0xFFD1C4E9), StarlightCyan)
            }
        }
    }

    private fun drawBraidedPonytail(
        drawScope: DrawScope,
        headX: Float,
        headY: Float,
        time: Float,
        scale: Float,
        animState: CharacterAnimState,
        mainColor: Color,
        tipColor: Color
    ) {
        val hairWave = sin(time * 8f) * 9f * scale
        val isSlideOrJump = (animState == CharacterAnimState.SLIDING || animState == CharacterAnimState.JUMPING)
        val tailLift = if (isSlideOrJump) -10f * scale else 0f

        val crownX = headX + 10f * scale
        val crownY = headY - 10f * scale
        drawScope.drawCircle(color = AstralGold, radius = 5f * scale, center = Offset(crownX, crownY))
        drawScope.drawCircle(color = tipColor, radius = 2.5f * scale, center = Offset(crownX, crownY))

        val segCount = 5
        for (i in 0 until segCount) {
            val t = i / segCount.toFloat()
            val bx = crownX + (8f + i * 5.5f) * scale + sin(time * 7.5f - i * 0.7f) * (3f + i * 2f) * scale
            val by = crownY + (6f + i * 9.5f) * scale + tailLift * t
            val segRadius = (8.5f - i * 1.1f).coerceAtLeast(3.5f) * scale
            val segColor = if (i >= segCount - 2) tipColor else mainColor
            drawScope.drawCircle(color = segColor, radius = segRadius, center = Offset(bx, by))
        }

        val ribbonPath = Path().apply {
            moveTo(crownX, crownY)
            quadraticTo(
                crownX + 20f * scale + hairWave,
                crownY + 18f * scale,
                crownX + 26f * scale + hairWave * 1.3f,
                crownY + 38f * scale + tailLift
            )
        }
        drawScope.drawPath(ribbonPath, color = tipColor.copy(alpha = 0.8f), style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round))
    }

    private fun drawHeadHairAndExpression(
        drawScope: DrawScope,
        character: CharacterInfo,
        animState: CharacterAnimState,
        time: Float,
        headX: Float,
        headY: Float,
        scale: Float,
        frontFacingFactor: Float
    ) {
        val headRadius = 15.5f * scale
        val boyStyle = isBoyStyle(character.id)

        drawScope.drawLine(
            color = Color(0xFFE5A982),
            start = Offset(headX, headY + headRadius * 0.7f),
            end = Offset(headX, headY + headRadius * 1.35f),
            strokeWidth = 8.5f * scale,
            cap = StrokeCap.Round
        )

        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFDFC4), Color(0xFFF5BFA0), Color(0xFFD99B78)),
                center = Offset(headX - 4f * scale, headY - 4f * scale),
                radius = headRadius * 1.3f
            ),
            radius = headRadius,
            center = Offset(headX, headY)
        )

        val faceShiftX = if (frontFacingFactor >= 0.5f) 1.5f * scale else 4.5f * scale
        val leftEyeX = headX - 4.5f * scale + faceShiftX * 0.5f
        val rightEyeX = headX + 4.5f * scale + faceShiftX * 0.5f
        val eyeY = headY - 1f * scale

        val isBlinking = (animState == CharacterAnimState.IDLE && (time % 3.4f) > 3.22f)

        if (animState == CharacterAnimState.GAME_OVER) {
            val strokeW = 2f * scale
            drawScope.drawLine(Color(0xFF2D1B14), Offset(leftEyeX - 2.5f * scale, eyeY - 2f * scale), Offset(leftEyeX + 1.5f * scale, eyeY), strokeW)
            drawScope.drawLine(Color(0xFF2D1B14), Offset(leftEyeX + 1.5f * scale, eyeY), Offset(leftEyeX - 2.5f * scale, eyeY + 2f * scale), strokeW)

            drawScope.drawLine(Color(0xFF2D1B14), Offset(rightEyeX + 2.5f * scale, eyeY - 2f * scale), Offset(rightEyeX - 1.5f * scale, eyeY), strokeW)
            drawScope.drawLine(Color(0xFF2D1B14), Offset(rightEyeX - 1.5f * scale, eyeY), Offset(rightEyeX + 2.5f * scale, eyeY + 2f * scale), strokeW)

            drawScope.drawCircle(
                color = Color(0xFF8D4A43),
                radius = 2.6f * scale,
                center = Offset(headX + faceShiftX * 0.5f, headY + 5.5f * scale)
            )
        } else if (isBlinking) {
            drawScope.drawLine(Color(0xFF2D1B14), Offset(leftEyeX - 2.5f * scale, eyeY), Offset(leftEyeX + 2.5f * scale, eyeY), 2f * scale)
            drawScope.drawLine(Color(0xFF2D1B14), Offset(rightEyeX - 2.5f * scale, eyeY), Offset(rightEyeX + 2.5f * scale, eyeY), 2f * scale)
        } else {
            val irisColor = character.primaryColor
            listOf(leftEyeX, rightEyeX).forEach { ex ->
                drawScope.drawCircle(color = Color.White, radius = 3.1f * scale, center = Offset(ex, eyeY))
                drawScope.drawCircle(color = irisColor, radius = 2.2f * scale, center = Offset(ex + 0.4f * scale, eyeY))
                drawScope.drawCircle(color = Color(0xFF1A121B), radius = 1.2f * scale, center = Offset(ex + 0.5f * scale, eyeY))
                drawScope.drawCircle(color = Color.White, radius = 0.8f * scale, center = Offset(ex - 0.5f * scale, eyeY - 0.8f * scale))
            }

            val smilePath = Path().apply {
                val mx = headX + faceShiftX * 0.5f
                val my = headY + 4.8f * scale
                moveTo(mx - 3.8f * scale, my - 0.5f * scale)
                quadraticTo(mx, my + 3.4f * scale, mx + 4.2f * scale, my - 1.0f * scale)
            }
            drawScope.drawPath(smilePath, color = Color(0xFF7B3F3A), style = Stroke(width = 1.9f * scale, cap = StrokeCap.Round))
        }

        // Hair Colors per Character
        val hairColors = when (character.id) {
            "kael" -> listOf(Color(0xFF795548), Color(0xFF4E342E))
            "lyra" -> listOf(Color(0xFFF3E5F5), Color(0xFF7E57C2))
            "shadow_runner" -> listOf(Color(0xFFEDE7F6), Color(0xFF5E35B1))
            "crystal_warrior" -> listOf(Color(0xFFE1F5FE), Color(0xFF0288D1))
            "forest_guardian" -> listOf(Color(0xFF81C784), Color(0xFF1B5E20))
            "sky_runner" -> listOf(Color(0xFFFFF8E1), Color(0xFFFFB300))
            else -> listOf(Color(0xFF795548), Color(0xFF4E342E))
        }

        if (boyStyle) {
            val hairPath = Path().apply {
                moveTo(headX - headRadius * 1.05f, headY - 2f * scale)
                quadraticTo(headX - headRadius * 0.8f, headY - headRadius * 1.35f, headX, headY - headRadius * 1.25f)
                quadraticTo(headX + headRadius * 0.9f, headY - headRadius * 1.35f, headX + headRadius * 1.05f, headY - 2f * scale)
                lineTo(headX + 6f * scale, headY - 7f * scale)
                lineTo(headX + 2f * scale, headY - 3.5f * scale)
                lineTo(headX - 4f * scale, headY - 7.5f * scale)
                lineTo(headX - 9f * scale, headY - 4f * scale)
                close()
            }
            drawScope.drawPath(
                path = hairPath,
                brush = Brush.verticalGradient(hairColors, headY - headRadius * 1.3f, headY)
            )
            // Forest Guardian Golden Antler Crown Circlet
            if (character.id == "forest_guardian") {
                drawScope.drawLine(AstralGold, Offset(headX - 12f * scale, headY - 14f * scale), Offset(headX - 18f * scale, headY - 24f * scale), 3f * scale, StrokeCap.Round)
                drawScope.drawLine(AstralGold, Offset(headX + 12f * scale, headY - 14f * scale), Offset(headX + 18f * scale, headY - 24f * scale), 3f * scale, StrokeCap.Round)
            }
        } else {
            val hairPath = Path().apply {
                moveTo(headX - headRadius * 1.08f, headY + 1f * scale)
                quadraticTo(headX - headRadius * 0.9f, headY - headRadius * 1.32f, headX, headY - headRadius * 1.25f)
                quadraticTo(headX + headRadius * 0.95f, headY - headRadius * 1.3f, headX + headRadius * 1.08f, headY)
                quadraticTo(headX + 4f * scale, headY - 7.5f * scale, headX - 2f * scale, headY - 4.5f * scale)
                quadraticTo(headX - 8f * scale, headY - 7.5f * scale, headX - headRadius * 1.08f, headY + 1f * scale)
                close()
            }
            drawScope.drawPath(
                path = hairPath,
                brush = Brush.verticalGradient(hairColors, headY - headRadius * 1.3f, headY + 2f * scale)
            )
        }
    }

    private fun drawFloatingMagicalAccessory(
        drawScope: DrawScope,
        character: CharacterInfo,
        time: Float,
        torsoTopX: Float,
        torsoTopY: Float,
        headX: Float,
        headY: Float,
        scale: Float,
        animState: CharacterAnimState
    ) {
        if (character.id == "kael") return

        val orbitX = torsoTopX - 28f * scale + cos(time * 3.2f) * 6f * scale
        val orbitY = torsoTopY - 14f * scale + sin(time * 4.2f) * 7f * scale

        drawScope.drawCircle(
            color = character.primaryColor.copy(alpha = 0.28f),
            radius = 13f * scale,
            center = Offset(orbitX, orbitY)
        )

        drawScope.drawCircle(
            color = character.secondaryColor,
            radius = 9f * scale,
            center = Offset(orbitX, orbitY),
            style = Stroke(width = 2f * scale)
        )

        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, character.primaryColor, character.accentColor),
                center = Offset(orbitX, orbitY),
                radius = 6f * scale
            ),
            radius = 5.2f * scale,
            center = Offset(orbitX, orbitY)
        )

        // Sky Runner & Crystal Warrior Floating Crown / Halo above head
        if (character.id == "sky_runner" || character.id == "crystal_warrior") {
            val crownY = headY - 22f * scale + sin(time * 4f) * 2.5f * scale
            drawScope.drawOval(
                color = character.primaryColor,
                topLeft = Offset(headX - 14f * scale, crownY - 4f * scale),
                size = Size(28f * scale, 8f * scale),
                style = Stroke(width = 2.4f * scale)
            )
        }

        if (animState != CharacterAnimState.IDLE) {
            for (m in 1..2) {
                drawScope.drawCircle(
                    color = character.primaryColor.copy(alpha = 0.6f / m),
                    radius = (3.2f / m) * scale,
                    center = Offset(
                        orbitX - m * 5f * scale,
                        orbitY + sin(time * 8f + m) * 4f * scale
                    )
                )
            }
        }
    }

    private fun drawAnimationEffects(
        drawScope: DrawScope,
        character: CharacterInfo,
        animState: CharacterAnimState,
        time: Float,
        centerX: Float,
        baseY: Float,
        headX: Float,
        headY: Float,
        scale: Float
    ) {
        when (animState) {
            CharacterAnimState.SLIDING -> {
                for (i in 0..3) {
                    val sparkPhase = (time * 12f + i * 0.25f) % 1f
                    val sx = centerX - (15f + sparkPhase * 38f) * scale
                    val sy = baseY - (2f + sin(sparkPhase * 3.14f) * 14f) * scale
                    drawScope.drawCircle(
                        color = if (i % 2 == 0) AstralGold else character.primaryColor,
                        radius = (3.8f * (1f - sparkPhase)) * scale,
                        center = Offset(sx, sy)
                    )
                }
            }

            CharacterAnimState.JUMPING -> {
                for (i in -1..1 step 2) {
                    val wx = centerX + i * 22f * scale
                    val wy = baseY - 18f * scale
                    drawScope.drawLine(
                        color = character.primaryColor.copy(alpha = 0.55f),
                        start = Offset(wx, wy - 14f * scale),
                        end = Offset(wx - i * 4f * scale, wy + 12f * scale),
                        strokeWidth = 2.5f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            CharacterAnimState.GAME_OVER -> {
                val haloY = headY - 22f * scale
                drawScope.drawOval(
                    color = AstralGold.copy(alpha = 0.45f),
                    topLeft = Offset(headX - 20f * scale, haloY - 5f * scale),
                    size = Size(40f * scale, 10f * scale),
                    style = Stroke(width = 1.8f * scale)
                )
                for (s in 0 until 3) {
                    val angle = time * 5.5f + (s * 2.094f)
                    val sx = headX + cos(angle) * 19f * scale
                    val sy = haloY + sin(angle) * 4.5f * scale
                    drawScope.drawCircle(
                        color = if (s % 2 == 0) AstralGold else StarlightCyan,
                        radius = 4.2f * scale,
                        center = Offset(sx, sy)
                    )
                }
            }

            else -> {}
        }
    }

    private fun drawGhostSilhouette(
        drawScope: DrawScope,
        character: CharacterInfo,
        centerX: Float,
        baseY: Float,
        scale: Float,
        alpha: Float
    ) {
        val ghostColor = character.primaryColor.copy(alpha = alpha)
        drawScope.drawCircle(
            color = ghostColor,
            radius = 14f * scale,
            center = Offset(centerX, baseY - 112f * scale)
        )
        drawScope.drawRoundRect(
            color = ghostColor,
            topLeft = Offset(centerX - 15f * scale, baseY - 94f * scale),
            size = Size(30f * scale, 42f * scale),
            cornerRadius = CornerRadius(8f * scale, 8f * scale)
        )
    }
}
