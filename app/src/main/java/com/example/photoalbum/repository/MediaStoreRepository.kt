package com.example.photoalbum.repository

import android.content.Context
import android.provider.MediaStore
import com.example.photoalbum.data.model.MediaFile

interface MediaStoreRepository {

    suspend fun getMediaList(): List<MediaFile>

    suspend fun updateMediaList(generationAdded: Int): List<MediaFile>
}

class ImageStoreImageRepositoryImpl(private val context: Context) : MediaStoreRepository {

    override suspend fun getMediaList(): List<MediaFile> {
        val mediaItems = mutableListOf<MediaFile>()
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.OWNER_PACKAGE_NAME,
            MediaStore.Images.Media.VOLUME_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.GENERATION_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.IS_DOWNLOAD,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val imageCursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )

        imageCursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val dateTaken = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                val data = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val packName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.OWNER_PACKAGE_NAME))
                val volName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.VOLUME_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                val bucket = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                val bucketName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val add = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.GENERATION_ADDED))
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val isDownload = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.IS_DOWNLOAD))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val orientation = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION))
                val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                if (name.isNullOrEmpty() || data.isNullOrEmpty() || size == 0L) {
                    continue
                }
                mediaItems.add(
                    MediaFile(
                        mediaFileId = id,
                        dateTaken = dateTaken,
                        bucketId = bucket,
                        generationAdded = add,
                        bucketDisplayName = bucketName,
                        displayName = name,
                        data = data,
                        relativePath = path,
                        ownerPackageName = packName ?: "",
                        volumeName = volName,
                        isDownload = isDownload,
                        mimeType = mimeType,
                        size = size,
                        orientation = orientation,
                        width = width,
                        height = height
                    )
                )
            }
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.OWNER_PACKAGE_NAME,
            MediaStore.Video.Media.VOLUME_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.GENERATION_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.IS_DOWNLOAD,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.ORIENTATION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.DURATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val videoCursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )

        videoCursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                val dateTaken = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                val data = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val packName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.OWNER_PACKAGE_NAME))
                val volName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.VOLUME_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH))
                val bucket = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val bucketName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val add = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.GENERATION_ADDED))
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                val isDownload = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.IS_DOWNLOAD))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val orientation = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION))
                val resolution = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION))
                val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                if (name.isNullOrEmpty() || data.isNullOrEmpty() || size == 0L) {
                    continue
                }
                if (size > 0 && duration > 0 && resolution != null) {
                    mediaItems.add(
                        MediaFile(
                            mediaFileId = id,
                            dateTaken = dateTaken,
                            bucketId = bucket,
                            generationAdded = add,
                            bucketDisplayName = bucketName,
                            displayName = name,
                            data = data,
                            relativePath = path,
                            ownerPackageName = packName ?: "",
                            volumeName = volName,
                            isDownload = isDownload,
                            mimeType = mimeType,
                            size = size,
                            orientation = orientation,
                            resolution = resolution,
                            duration = duration,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        }
        return mediaItems
    }

    override suspend fun updateMediaList(generationAdded: Int): List<MediaFile> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.OWNER_PACKAGE_NAME,
            MediaStore.Images.Media.VOLUME_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.GENERATION_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.IS_DOWNLOAD,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.GENERATION_ADDED} > ?",
            arrayOf("$generationAdded"),
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )
        val mediaItems = mutableListOf<MediaFile>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val dateTaken = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                val data = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val packName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.OWNER_PACKAGE_NAME))
                val volName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.VOLUME_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                val bucket = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                val bucketName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val add = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.GENERATION_ADDED))
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val isDownload = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.IS_DOWNLOAD))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val orientation = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION))
                val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                if (name.isNullOrEmpty() || data.isNullOrEmpty() || size == 0L) {
                    continue
                }
                mediaItems.add(
                    MediaFile(
                        mediaFileId = id,
                        dateTaken = dateTaken,
                        bucketId = bucket,
                        generationAdded = add,
                        bucketDisplayName = bucketName,
                        displayName = name,
                        data = data,
                        relativePath = path,
                        ownerPackageName = packName ?: "",
                        volumeName = volName,
                        isDownload = isDownload,
                        mimeType = mimeType,
                        size = size,
                        orientation = orientation,
                        width = width,
                        height = height
                    )
                )
            }
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.OWNER_PACKAGE_NAME,
            MediaStore.Video.Media.VOLUME_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.GENERATION_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.IS_DOWNLOAD,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.ORIENTATION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.DURATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val videoCursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            "${MediaStore.Video.Media.GENERATION_ADDED} > ?",
            arrayOf("$generationAdded"),
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )

        videoCursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                val dateTaken = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                val data = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val packName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.OWNER_PACKAGE_NAME))
                val volName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.VOLUME_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH))
                val bucket = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val bucketName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val add = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.GENERATION_ADDED))
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                val isDownload = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.IS_DOWNLOAD))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val orientation = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION))
                val resolution = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION))
                val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                //println("增长id$generationAdded 文件的增长id$add")

                if (name.isNullOrEmpty() || data.isNullOrEmpty() || size == 0L) {
                    continue
                }
                if (size > 0 && duration > 0 && resolution != null) {
                    mediaItems.add(
                        MediaFile(
                            mediaFileId = id,
                            dateTaken = dateTaken,
                            bucketId = bucket,
                            generationAdded = add,
                            bucketDisplayName = bucketName,
                            displayName = name,
                            data = data,
                            relativePath = path,
                            ownerPackageName = packName ?: "",
                            volumeName = volName,
                            isDownload = isDownload,
                            mimeType = mimeType,
                            size = size,
                            orientation = orientation,
                            resolution = resolution,
                            duration = duration,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        }
        return mediaItems
    }

}