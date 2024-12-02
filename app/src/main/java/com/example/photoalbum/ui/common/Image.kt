package com.example.photoalbum.ui.common

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun DisplayImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = bitmap,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}
