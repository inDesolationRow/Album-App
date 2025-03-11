package com.example.photoalbum.model

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.IntSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.photoalbum.enums.ItemType

data class MediaItem(
    val id: Long,

    val displayName: String,

    val data: String? = null,

    val dataBitmap: MutableState<Bitmap?> = mutableStateOf(null),

    //val imageBlockList: SnapshotStateList<Bitmap> = mutableStateListOf(),

    var imageRatio: Float? = null,

    val thumbnailPath: String? = null,

    var thumbnail: Bitmap? = null,

    val thumbnailState: MutableState<Bitmap?> = mutableStateOf(null),

    val type: ItemType,

    var fileSize: Long = 0,

    var size: Int = 0,

    var mimeType: String,

    var orientation: Int = 0,

    var local: Boolean = true,

    var resolution: String = "",

    var duration: Long = 0,

    val videoWhenReady: MutableState<Boolean> = mutableStateOf(false),

    val width: Int = 0,

    val height: Int = 0,
)