package com.example.photoalbum.repository

import android.content.Context
import android.provider.MediaStore
import com.example.photoalbum.model.MediaFile

interface MediaStoreRepository {

    suspend fun getMediaList() : List<MediaFile>

}

class ImageStoreImageRepositoryImpl(private val context: Context): MediaStoreRepository {

    override suspend fun getMediaList() : List<MediaFile>{
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
            MediaStore.Images.Media.SIZE
        )

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
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
                if(name.isNullOrEmpty()||data.isNullOrEmpty()||size == 0L){
                    continue
                }
                mediaItems.add(MediaFile(mediaFileId = id,
                    dateTaken = dateTaken,
                    bucketId = bucket,
                    generationAdded = add,
                    bucketDisplayName = bucketName,
                    displayName = name,
                    data = data,
                    relativePath = path,
                    ownerPackageName = packName?:"",
                    volumeName = volName,
                    isDownload = isDownload,
                    mimeType = mimeType))
            }
        }
        return mediaItems
    }

}