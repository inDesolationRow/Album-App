package com.example.photoalbum.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun decodeSampledBitmapFromStream(filePath: String, reqWidth: Int = 256, reqHeight: Int = 256): Bitmap? {
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
        return BitmapFactory.decodeFile(filePath, options)
    }catch (e: Exception){
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

fun saveBitmapToPrivateStorage(context: Context, bitmap: Bitmap, fileName: String, directory: String = "Thumbnail"): File? {
    // 获取应用的私有存储路径（内部存储）
    val savePath = (context.getExternalFilesDir(null) ?: context.filesDir).absoluteFile.path.plus("/$directory")
    val saveDirectory = File(savePath)
    if (!saveDirectory.exists()){
        saveDirectory.mkdirs()
    }
    val file = File(savePath, fileName)

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