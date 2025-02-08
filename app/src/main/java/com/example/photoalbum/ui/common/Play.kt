package com.example.photoalbum.ui.common

import android.view.Surface
import androidx.annotation.IntDef
import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun PlayerSurface(player: Player, surfaceType: @SurfaceType Int, modifier: Modifier = Modifier) {
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
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            // 在工厂方法中创建 PlayerView 实例
            PlayerView(context).apply {
                player = exoPlayer

                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                // 如果需要，可以设置是否显示控制器
                useController = true
            }
        },
        modifier = modifier,
        update = { view ->
            // 如果在组合过程中 exoPlayer 发生变化，可以在这里更新 PlayerView
            view.player = exoPlayer
        }
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

/** Surface type equivalent to [SurfaceView] . */
const val SURFACE_TYPE_SURFACE_VIEW = 1
/** Surface type equivalent to [TextureView]. */
const val SURFACE_TYPE_TEXTURE_VIEW = 2