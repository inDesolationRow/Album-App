package com.example.photoalbum.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AniCueCard(
    modifier: Modifier = Modifier,
    cueText: String,
) {
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val shakeValues = listOf( // 抖动偏移序列
                18f, -18f, 14f, -14f, 10f, -10f, 6f, -6f, 3f, -3f, 0f
            )
            val rotateValues = listOf(2f, -2f, 1f, -1f, 1f, -1f, 1f, -1f, 1f, -1f, 0f)

            for (i in shakeValues.indices) {
                offsetX.animateTo(
                    targetValue = shakeValues[i],
                    animationSpec = tween(durationMillis = 100) // 每次偏移 100ms
                )
                rotation.animateTo(
                    targetValue = rotateValues[i],
                    animationSpec = tween(durationMillis = 30)
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                translationX = offsetX.value,
                rotationZ = rotation.value,
            )
    ) {
        ElevatedCard(
            modifier = Modifier
                .width(300.dp)
                .height(100.dp)
        ) {
            Text(
                cueText,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}