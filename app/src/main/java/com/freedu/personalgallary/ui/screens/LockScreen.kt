package com.freedu.personalgallary.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.personalgallary.R
import com.freedu.personalgallary.ui.components.GradientButton
import kotlin.math.roundToInt

private const val MAX_PIN = 8

@Composable
fun LockScreen(
    hasPin: Boolean,
    biometricAvailable: Boolean,
    setupMode: Boolean = false,
    error: String? = null,
    onPin: (String) -> Unit,
    onBiometric: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var phase by remember { mutableIntStateOf(0) } // setup only: 0 = new, 1 = confirm
    val haptics = LocalHapticFeedback.current

    val active = if (setupMode && phase == 1) confirm else pin
    fun press(digit: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val next = (active + digit).take(MAX_PIN)
        if (setupMode && phase == 1) confirm = next else pin = next
    }
    fun erase() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (setupMode && phase == 1) confirm = confirm.dropLast(1) else pin = pin.dropLast(1)
    }
    fun submit() {
        if (setupMode) {
            if (phase == 0) {
                if (pin.length >= 4) phase = 1
            } else {
                onPin("$pin|$confirm")
            }
        } else if (pin.length >= 4) {
            onPin(pin)
        }
    }

    // auto-submit: full-length PIN, or matching confirmation
    LaunchedEffect(pin.length) {
        if (!setupMode && pin.length == MAX_PIN) onPin(pin)
    }
    LaunchedEffect(confirm.length) {
        if (setupMode && phase == 1 && confirm.length == pin.length && pin.length >= 4) {
            onPin("$pin|$confirm")
        }
    }

    // shake on error
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            repeat(3) {
                shakeX.animateTo(16f, tween(55))
                shakeX.animateTo(-16f, tween(55))
            }
            shakeX.animateTo(0f, tween(55))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1233), Color(0xFF3B1F5E), Color(0xFF120B24))
                )
            )
    ) {
        // soft glows
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-90).dp, y = (-70).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF4D6D).copy(alpha = 0.35f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF7B5CFF).copy(alpha = 0.35f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(R.drawable.personal_gallary_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                when {
                    setupMode && phase == 0 -> "Set a private PIN"
                    setupMode -> "Confirm your PIN"
                    hasPin -> "Welcome back"
                    else -> "Private gallery"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    setupMode && phase == 0 -> "4–8 digits · encrypted on this device"
                    setupMode -> "Enter it once more"
                    else -> "Your memories are locked"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            // PIN dots (shake on error)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.offset { IntOffset(shakeX.value.roundToInt(), 0) }
            ) {
                repeat(MAX_PIN) { i ->
                    Box(
                        Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < active.length) Color.White
                                else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(18.dp))
            // keypad
            val rows = listOf("123", "456", "789")
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { ch ->
                        KeyBtn(label = ch.toString(), onPress = { press(ch.toString()) })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!setupMode && biometricAvailable) {
                    IconButton(
                        onClick = onBiometric,
                        modifier = Modifier.size(70.dp)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint, "Unlock with biometrics",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(70.dp))
                }
                KeyBtn(label = "0", onPress = { press("0") })
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { erase() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace, "Delete",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = when {
                    setupMode && phase == 0 -> "Next"
                    setupMode -> "Save PIN"
                    else -> "Unlock"
                },
                onClick = ::submit,
                enabled = active.length >= 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (setupMode && phase == 1) {
                TextButton(onClick = { phase = 0; confirm = "" }) {
                    Text("Back", color = Color.White.copy(alpha = 0.8f))
                }
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KeyBtn(label: String, onPress: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onPress()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
