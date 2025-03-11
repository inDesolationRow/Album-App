package com.example.photoalbum.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.graphics.Rect
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt


fun getMiddleFrame(
    context: Context,
    videoUri: Uri,
    duration: Long,
    reqWidth: Int = 300,
    reqHeight: Int = 300,
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)

        //val timeUs = (duration / 2) * 1000L
        val timeUs = if (duration >= 1000) 1000L * 1000L else 1000L
        // 获取中间帧。OPTION_CLOSEST_SYNC 可保证获取距离指定时间最近的关键帧
        retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC, reqWidth, reqHeight)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}

/*
fun getImageRatio(
    filePath: String,
    orientation: Int = 0,
): Float {
    var ratio = 0f
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true // 只加载图片的元信息，不加载实际内容
    }
    BitmapFactory.decodeFile(filePath, options)
    ratio =
        if (orientation == 90 || orientation == 270)
            options.outHeight.toFloat() / options.outWidth.toFloat()
        else
            options.outWidth.toFloat() / options.outHeight.toFloat()
    return ratio
}
*/

fun decodeBitmap(
    filePath: String,
    orientation: Float,
    targetWidth: Int = 4320,
    maxMemorySize: Int = 50 * 1024 * 1024,
): Bitmap? {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只加载图片的元信息，不加载实际内容
        }
        BitmapFactory.decodeFile(filePath, options)
        // 计算合适的 inSampleSize
        options.inSampleSize = calculateOptimalSampleSize(
            options = options,
            targetWidth = targetWidth,
            maxMemorySize = maxMemorySize
        )
        options.inJustDecodeBounds = false
        val bitmap = rotateBitmap(BitmapFactory.decodeFile(filePath, options), orientation = orientation)
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        println("错误:解析失败文件地址 $filePath")
        return null
    }
}

fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val temp = this[index1]
    this[index1] = this[index2]
    this[index2] = temp
}

fun decodeBitmap(
    filePath: String,
    orientation: Float,
    block: Int,
): MutableList<Bitmap>? {
    val decoder: BitmapRegionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(filePath)
    } else {
        BitmapRegionDecoder.newInstance(filePath, true)
    }
    val width = decoder.width
    val height = decoder.height
    if (width == 0 || height == 0) return null

    try {
        val line = sqrt(block.toDouble()).toInt()
        val blockWidth = width / line
        val blockHeight = height / line
        val blockList: MutableList<Bitmap> = mutableListOf()
        val options = BitmapFactory.Options().apply {
            inMutable = true
        }

        for (column in 0..<line) {
            for (row in 0..<line) {
                val left = row * blockWidth
                val top = column * blockHeight
                val right = /*if (row == block - 1) width else */(row + 1) * blockWidth
                val bottom = /*if (column == block - 1) height else*/ (column + 1) * blockHeight
                val rect = Rect(left, top, right, bottom)
                println("左上: (${left}, ${top}) 右下: (${right}, ${bottom}) 宽高：($width*$height)")
                //blockList.add(rotateBitmap(decoder.decodeRegion(rect, options), orientation))
                blockList.add(decoder.decodeRegion(rect, options))
            }
        }
        /*        when (orientation) {
                    90f -> {
                        TODO()
                    }
                    180f -> {
                        TODO()
                    }
                    270f -> {
                        TODO()
                    }
                }*/
        return blockList
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun decodeBitmap(
    filePath: String,
    width: Int,
    height: Int,
    orientation: Float,
    targetWidth: Int = 4320,
    maxMemorySize: Int = 50 * 1024 * 1024,
): Bitmap? {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只加载图片的元信息，不加载实际内容
        }
        val imageWidth: Int
        val imageHeight: Int
        if (width > 0 && height > 0) {
            imageWidth = width
            imageHeight = height
        } else {
            BitmapFactory.decodeFile(filePath, options)
            imageWidth = options.outWidth
            imageHeight = options.outHeight
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateOptimalSampleSize(
            width = imageWidth,
            height = imageHeight,
            targetWidth = targetWidth,
            maxMemorySize = maxMemorySize
        )
        val bitmap = rotateBitmap(BitmapFactory.decodeFile(filePath, options), orientation = orientation)
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        println("错误:解析失败文件地址 $filePath")
        return null
    }
}

fun decodeBitmap(
    byteArray: ByteArray,
    targetWidth: Int = 1080,
    maxMemorySize: Int = 50 * 1024 * 1024,
): Bitmap? {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只加载图片的元信息，不加载实际内容
        }
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        options.inSampleSize = calculateOptimalSampleSize(
            options = options,
            targetWidth = targetWidth,
            maxMemorySize = maxMemorySize
        )
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    } catch (e: Exception) {
        println("错误:解析失败 ${e.printStackTrace()}")
        return null
    }
}

fun decodeSampledBitmap(
    filePath: String,
    orientation: Float,
    width: Int,
    height: Int,
    reqWidth: Int = 300,
    reqHeight: Int = 300,
): Bitmap? {
    try {
        // 第一次加载仅获取图片的尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val imageWidth: Int
        val imageHeight: Int
        if (width > 0 && height > 0) {
            imageWidth = width
            imageHeight = height
        } else {
            BitmapFactory.decodeFile(filePath, options)
            imageWidth = options.outWidth
            imageHeight = options.outHeight
        }
        options.inJustDecodeBounds = false
        // 计算 inSampleSize 值
        options.inSampleSize = calculateInSampleSize(width = imageWidth, height = imageHeight, reqWidth, reqHeight)

        // 关闭 inJustDecodeBounds 并加载图像
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return rotateBitmap(BitmapFactory.decodeFile(filePath, options), orientation)
    } catch (e: Exception) {
        e.printStackTrace()
        println("错误:失败图片地址 $filePath")
        return null
    }
}

fun decodeSampledBitmap(
    byteArray: ByteArray,
    reqWidth: Int = 300,
    reqHeight: Int = 300,
): Bitmap? {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

        // 计算 inSampleSize 值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // 关闭 inJustDecodeBounds 并加载图像
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        return bitmap
    } catch (e: Exception) {
        println("错误:解析失败")
        e.printStackTrace()
        return null
    }
}


fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / reqWidth
        val halfWidth: Int = width / reqHeight

        inSampleSize = if (halfWidth >= halfHeight) halfHeight else halfWidth
    }
    return inSampleSize
}

fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val (imageHeight: Int, imageWidth: Int) = height to width
    var inSampleSize = 1

    if (imageHeight > reqHeight || imageWidth > reqWidth) {
        val halfHeight: Int = imageHeight / reqWidth
        val halfWidth: Int = imageWidth / reqHeight

        inSampleSize = if (halfWidth >= halfHeight) halfHeight else halfWidth
    }
    return inSampleSize
}

fun calculateOptimalSampleSize(
    options: BitmapFactory.Options,
    targetWidth: Int,
    maxMemorySize: Int,
): Int {
    val originalWidth: Int = options.outWidth
    val originalHeight: Int = options.outHeight
    val bytesPerPixel = 4 // ARGB_8888 格式，每像素 4 字节
    var inSampleSize = 1

    // 宽度适配
    if (originalWidth > targetWidth) {
        inSampleSize = originalWidth / targetWidth
    }

    // 内存限制调整
    while (true) {
        val scaledWidth = originalWidth / inSampleSize
        val scaledHeight = originalHeight / inSampleSize
        val estimatedMemory = scaledWidth * scaledHeight * bytesPerPixel

        if (estimatedMemory <= maxMemorySize) {
            break
        }

        // 优化缩放策略，逐步增加缩放比
        inSampleSize++
    }

    return inSampleSize
}

fun calculateOptimalSampleSize(
    width: Int,
    height: Int,
    targetWidth: Int,
    maxMemorySize: Int,
): Int {
    val originalWidth: Int = width
    val originalHeight: Int = height
    val bytesPerPixel = 4 // ARGB_8888 格式，每像素 4 字节
    var inSampleSize = 1

    // 宽度适配
    if (originalWidth > targetWidth) {
        inSampleSize = originalWidth / targetWidth
    }

    // 内存限制调整
    while (true) {
        val scaledWidth = originalWidth / inSampleSize
        val scaledHeight = originalHeight / inSampleSize
        val estimatedMemory = scaledWidth * scaledHeight * bytesPerPixel

        if (estimatedMemory <= maxMemorySize) {
            break
        }

        // 优化缩放策略，逐步增加缩放比
        inSampleSize++
    }

    return inSampleSize
}

fun saveBitmapToPrivateStorage(
    bitmap: Bitmap,
    fileName: String,
    directory: String,
): File? {
    val saveDirectory = File(directory)
    if (!saveDirectory.exists()) {
        saveDirectory.mkdirs()
    }
    val file = File(directory, fileName)

    // 使用 FileOutputStream 写入文件
    try {
        FileOutputStream(file).use { fos ->
            // 将 Bitmap 压缩为 JPEG 或 PNG 格式
            // 这里假设使用 PNG 格式（无损压缩）
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file // 返回保存的文件
    } catch (e: IOException) {
        println("保存失败 :${e.message}")
        e.printStackTrace()
        return null // 如果发生错误，返回 null
    }
}

fun rotateBitmap(bitmap: Bitmap, orientation: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(orientation)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/*
fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
    val start = System.currentTimeMillis()
    val renderScript = RenderScript.create(context)
    val input = Allocation.createFromBitmap(renderScript, bitmap)
    val output = Allocation.createTyped(renderScript, input.type)
    val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

    script.setRadius(radius) // 设置模糊半径
    script.setInput(input)
    script.forEach(output)

    output.copyTo(bitmap) // 将处理后的图像输出回Bitmap
    renderScript.destroy()
    val end = System.currentTimeMillis()
    return bitmap
}
*/

