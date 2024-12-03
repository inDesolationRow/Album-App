@file:Suppress("DEPRECATION")

package com.example.photoalbum.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

fun decodeBitmap(filePath: String, orientation: Float): Bitmap? {
    try {
        /*        val result : Bitmap
                val before = System.currentTimeMillis()
                result = BitmapFactory.decodeFile(filePath)
                val after = System.currentTimeMillis()
                println("测试:原图加载时间 ${after - before}")*/
        return rotateBitmap(BitmapFactory.decodeFile(filePath), orientation = orientation)
    } catch (e: Exception) {
        println("测试:解析失败 ${e.printStackTrace()}")
        return null
    }
}

fun decodeBitmap(byteArray: ByteArray): Bitmap? {
    try {
        /*        val result : Bitmap
                val before = System.currentTimeMillis()
                result = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                val after = System.currentTimeMillis()
                println("测试:原图加载时间 ${after - before}")*/
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    } catch (e: Exception) {
        println("测试:解析失败 ${e.printStackTrace()}")
        return null
    }
}

fun decodeBitmap(inputStream: InputStream): Bitmap? {
    try {
        /*        val result : Bitmap
                val before = System.currentTimeMillis()
                result = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                val after = System.currentTimeMillis()
                println("测试:原图加载时间 ${after - before}")*/
        return BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        println("测试:解析失败 ${e.printStackTrace()}")
        return null
    }
}

fun decodeSampledBitmap(
    filePath: String,
    orientation: Float,
    reqWidth: Int = 300,
    reqHeight: Int = 300
): Bitmap? {
    try {
        // 第一次加载仅获取图片的尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        // 计算 inSampleSize 值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // 关闭 inJustDecodeBounds 并加载图像
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return rotateBitmap(BitmapFactory.decodeFile(filePath, options), orientation)
    } catch (e: Exception) {
        println("测试:解析失败 ${e.printStackTrace()}")
        return null
    }
}

fun decodeSampledBitmap(
    byteArray: ByteArray,
    reqWidth: Int = 300,
    reqHeight: Int = 300
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
        println("测试:解析失败 ${e.printStackTrace()}")
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

fun saveBitmapToPrivateStorage(
    bitmap: Bitmap,
    fileName: String,
    directory: String
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

fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {

    val renderScript = RenderScript.create(context)
    val input = Allocation.createFromBitmap(renderScript, bitmap)
    val output = Allocation.createTyped(renderScript, input.type)
    val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

    script.setRadius(radius) // 设置模糊半径
    script.setInput(input)
    script.forEach(output)

    output.copyTo(bitmap) // 将处理后的图像输出回Bitmap
    renderScript.destroy()
    return bitmap
}

