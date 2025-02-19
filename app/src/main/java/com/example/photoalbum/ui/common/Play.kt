package com.example.photoalbum.ui.common

import android.view.Surface
import android.view.TextureView
import androidx.annotation.IntDef
import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun PlayerSurface(player: Player, modifier: Modifier = Modifier, surfaceType: @SurfaceType Int) {
    val onSurfaceCreated: (Surface) -> Unit = { surface -> player.setVideoSurface(surface) }
    val onSurfaceDestroyed: () -> Unit = { player.setVideoSurface(null) }
    val onSurfaceInitialized: AndroidExternalSurfaceScope.() -> Unit = {
        onSurface { surface, _, _ ->
            onSurfaceCreated(surface)
            surface.onDestroyed { onSurfaceDestroyed() }
        }
    }

    when (surfaceType) {
        SURFACE_TYPE_SURFACE_VIEW ->
            AndroidExternalSurface(modifier = modifier, onInit = onSurfaceInitialized)

        SURFACE_TYPE_TEXTURE_VIEW ->
            AndroidEmbeddedExternalSurface(modifier = modifier, onInit = onSurfaceInitialized)

        else -> throw IllegalArgumentException("Unrecognized surface type: $surfaceType")
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
    modifier = modifier.fillMaxSize())
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

/** Surface type equivalent to [SurfaceView] . */
const val SURFACE_TYPE_SURFACE_VIEW = 1

/** Surface type equivalent to [TextureView]. */
const val SURFACE_TYPE_TEXTURE_VIEW = 2