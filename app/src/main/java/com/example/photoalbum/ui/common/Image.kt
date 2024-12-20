package com.example.photoalbum.ui.common

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun DisplayImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    context: Context
) {
    /*val image = ImageRequest.Builder(context)
        .data(bitmap)  // 设置图片的 URI 或者数据
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()*/
    AsyncImage(
        model = bitmap,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}
