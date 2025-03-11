package com.example.photoalbum.ui.common

import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.sqrt


@Composable
fun DisplayImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    context: Context,
) {
    val image = ImageRequest.Builder(context)
        .data(bitmap)  // 设置图片的 URI 或者数据
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    AsyncImage(
        model = image,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
fun DisplayBlockImage(
    imageBlockList: SnapshotStateList<Bitmap>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    context: Context,
    block: Int = 16,
) {
    /*    for(){}
        Column {

        }*/
    LazyVerticalGrid(
        columns = GridCells.Fixed(sqrt(block.toDouble()).toInt()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    ) {
        items(count = imageBlockList.size) { index ->
            /* println("${imageBlockList[index].width}* ${imageBlockList[index].height}")
             val image = ImageRequest.Builder(context)
                 .data(imageBlockList[index])  // 设置图片的 URI 或者数据
                 //.memoryCachePolicy(CachePolicy.DISABLED)
                 //.diskCachePolicy(CachePolicy.ENABLED)
                 .build()
             AsyncImage(
                 model = image,
                 contentDescription = null,
                 contentScale = ContentScale.Fit,
                 modifier = modifier,
             )*/
            /*Image(
                painter = BitmapPainter(image = imageBlockList[index].asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .padding(0.dp)

            )*/
            val blockImage = imageBlockList[index].asImageBitmap()
            androidx.compose.foundation.Canvas(
                modifier =
                Modifier.size(100.dp)
            ) {
                drawImage(
                    blockImage,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(blockImage.width, blockImage.height),
                    dstOffset = IntOffset.Zero
                )
            }
        }
    }

}