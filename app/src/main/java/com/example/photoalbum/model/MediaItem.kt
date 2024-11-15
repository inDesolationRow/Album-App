package com.example.photoalbum.model

import android.graphics.Bitmap
import com.example.photoalbum.enums.ItemType

data class MediaItem(
    val id: Long,
    val displayName: String,
    //var thumbnail: MutableState<Bitmap?> = mutableStateOf(null),
    val data: String? = null,
    val thumbnailPath: String? = null,
    var thumbnail: Bitmap? = null,
    val type: ItemType,
    var fileSize: Int = 0,
    var size: Int = 0,
    var mimeType: String,
    var orientation: Int = 0
)