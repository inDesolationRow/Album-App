package com.example.photoalbum.ui.common

import android.view.Surface
import android.view.TextureView
import androidx.annotation.IntDef
import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.photoalbum.R
import com.example.photoalbum.ui.theme.TinyPadding
import com.example.photoalbum.utils.millisToTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerSurface(player: Player, modifier: Modifier = Modifier, surfaceType: @SurfaceType Int) {
    //初始化ui参数
    val coroutineScope = rememberCoroutineScope()
    val play = remember { mutableStateOf(true) }
    val duration = remember { mutableLongStateOf(0) }
    val position = remember { mutableLongStateOf(0) }
    var job: Job? = null
    LaunchedEffect(Unit) {
        job = coroutineScope.launch {
            try {
                while (true) {
                    delay(500)
                    play.value = player.isPlaying
                    duration.longValue = player.duration
                    position.longValue = player.currentPosition
                }
            } catch (cancel: CancellationException) {
                throw cancel
            }
        }
    }

    //初始化surface
    val onSurfaceCreated: (Surface) -> Unit = { surface -> player.setVideoSurface(surface) }
    val onSurfaceDestroyed: () -> Unit = { player.setVideoSurface(null) }
    val onSurfaceInitialized: AndroidExternalSurfaceScope.() -> Unit = {
        onSurface { surface, _, _ ->
            onSurfaceCreated(surface)
            surface.onDestroyed {
                onSurfaceDestroyed()
                job?.cancel()
            }
        }
    }

    when (surfaceType) {
        SURFACE_TYPE_SURFACE_VIEW ->
            AndroidExternalSurface(modifier = modifier, onInit = onSurfaceInitialized)

        SURFACE_TYPE_TEXTURE_VIEW -> {
            AndroidEmbeddedExternalSurface(modifier = modifier, onInit = onSurfaceInitialized)
            PlayerUi(
                check = play,
                duration = duration,
                position = position,
                modifier = modifier
            ) { isPlay ->
                if (isPlay) {
                    if (!player.isPlaying)
                        player.play()
                } else
                    if (player.isPlaying)
                        player.pause()
            }
        }

        else -> throw IllegalArgumentException("Unrecognized surface type: $surfaceType")
    }
}

@Composable
fun PlayerUi(
    check: MutableState<Boolean>,
    duration: MutableLongState,
    position: MutableLongState,
    modifier: Modifier, onCheck: (Boolean) -> Unit,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.padding(bottom = TinyPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color(0x80000000))
                .padding(start = TinyPadding, end = TinyPadding * 2)
                .clickable {
                    check.value = !check.value
                    onCheck(check.value)
                }
        ) {
            IconToggleButton(
                checked = check.value,
                onCheckedChange = {
                    check.value = it
                    onCheck(it)
                },
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
            ) {
                if (check.value) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = Color.White,
                        modifier = Modifier.padding(0.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.Stop,
                        contentDescription = "Localized description",
                        tint = Color.White,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
            val durationTriple = millisToTime(duration.longValue)
            val positionTriple = millisToTime(position.longValue)
            val hour = durationTriple.first != "0"
            Text(
                if (hour)
                    stringResource(R.string.duration2, positionTriple.first, positionTriple.second, positionTriple.third)
                else
                    stringResource(R.string.duration1, positionTriple.second, positionTriple.third),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                "/ " + if (hour)
                    stringResource(R.string.duration2, durationTriple.first, durationTriple.second, durationTriple.third)
                else
                    stringResource(R.string.duration1, durationTriple.second, durationTriple.third),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComposable(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                player.setVideoTextureView(this)
                this.clipToOutline = true
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * The type of surface view used for media playbacks. One of [SURFACE_TYPE_SURFACE_VIEW] or
 * [SURFACE_TYPE_TEXTURE_VIEW].
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@IntDef(SURFACE_TYPE_SURFACE_VIEW, SURFACE_TYPE_TEXTURE_VIEW)
annotation class SurfaceType

/** Surface type equivalent to [android.view.SurfaceView] . */
const val SURFACE_TYPE_SURFACE_VIEW = 1

/** Surface type equivalent to [TextureView]. */
const val SURFACE_TYPE_TEXTURE_VIEW = 2