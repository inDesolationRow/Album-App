package com.example.photoalbum.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.SystemFolder
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.utils.decodeSampledBitmapFromStream
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

class MediaItemPagingSource(private val apiService: MediaFileService<*>) :
    PagingSource<Int, MediaItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return try {
            val page = params.key ?: 1
            val response = apiService.getData(page, params.loadSize)
            LoadResult.Page(
                data = response,
                prevKey = if (page == 1) null else page - 1,
                nextKey = apiService.next(page, params.loadSize)
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

interface MediaFileService<T> {

    val allData: MutableList<MediaItem>

    val thumbnailsPath: String

    suspend fun getData(page: Int, loadSize: Int): List<MediaItem>

    fun next(page: Int, loadSize: Int): Int?

    suspend fun getAllDataForMediaList(param: T)

    suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap?

    suspend fun createThumbnail(
        path: String,
        mediaFileId: Long,
        fileName: String,
    ): Bitmap?
}

class LocalStorageMediaFileService(private val application: MediaApplication) :
    MediaFileService<Long> {

    override val allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        val start = (page - 1) * loadSize
        var end = page * loadSize - 1
        if (end > allData.size - 1)
            end = allData.size - 1
        val items = allData.slice(IntRange(start, end))

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                //1k分辨率以下的图像生成缩略图时同步, 1k以上分辨率不再同步,使用state延时重组
                if (item.fileSize < ImageSize.ONE_K.size) {
                    jobs.add(coroutineScope.launch(Dispatchers.IO) {
                        item.thumbnail = loadThumbnail(item)
                    })
                } else {
                    coroutineScope.launch(Dispatchers.IO) {
                        item.thumbnailState.value = loadThumbnail(item)
                    }
                }
            }
        }
        jobs.forEach {
            it.join()
        }
        val endDate = System.currentTimeMillis()
        val re = endDate - startDate
        println("测试:加载bitmap用时$re")
        return items
    }

    override fun next(page: Int, loadSize: Int): Int? {
        val start = page * loadSize
        if (start > allData.size) return null
        val result = page + 1
        return result
    }

    override suspend fun getAllDataForMediaList(param: Long) {
        allData.clear()
        val directories =
            application.mediaDatabase.directoryDao.querySortedByNameForDirectory(param)
        val order1: MutableList<MediaItem> = mutableListOf()
        val order2: MutableList<MediaItem> = mutableListOf()
        val order3: MutableList<MediaItem> = mutableListOf()
        if (!directories.isNullOrEmpty()) {
            for (dir in directories) {
                val item = MediaItem(
                    id = dir.directoryId,
                    type = ItemType.DIRECTORY,
                    displayName = dir.displayName,
                    mimeType = "",
                )
                val name = dir.displayName.lowercase()
                when{
                    name.contains(SystemFolder.DCIM.displayName) -> {order1.add(item)}
                    name.contains(SystemFolder.CAMERA.displayName) -> {order1.add(item)}
                    name.contains(SystemFolder.DOCUMENT.displayName) -> {order2.add(item)}
                    name.contains(SystemFolder.DOWNLOAD.displayName) -> {order2.add(item)}
                    name.contains(SystemFolder.PICTURES.displayName) -> {order2.add(item)}
                    name.contains(SystemFolder.SCREENSHOTS.displayName) -> {order2.add(item)}
                    else -> {order3.add(item)}
                }
            }
            allData.addAll(order1)
            allData.addAll(order2)
            allData.addAll(order3)
        }

        val mediaList =
            application.mediaDatabase.directoryDao.querySortedMediaFilesByDirectoryId(param)
        if (mediaList.isNullOrEmpty()) return
        for (mediaFile in mediaList) {
            val item = MediaItem(
                id = mediaFile.mediaFileId,
                type = mediaFile.mimeType.let {
                    val test = it.lowercase()
                    return@let when {
                        test.contains("image") -> {
                            ItemType.IMAGE
                        }

                        test.contains("video") -> {
                            ItemType.VIDEO
                        }

                        else -> {
                            ItemType.ERROR
                        }
                    }
                },
                data = mediaFile.data,
                thumbnailPath = mediaFile.thumbnail,
                displayName = mediaFile.displayName,
                mimeType = mediaFile.mimeType,
                orientation = mediaFile.orientation,
                fileSize = mediaFile.size
            )
            allData.add(item)
        }
    }

    override suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return if (mediaItem.thumbnailPath.isNullOrEmpty()) {
            createThumbnail(
                mediaItem.data!!,
                mediaItem.id,
                mediaItem.displayName
            ) ?: BitmapFactory.decodeFile(
                File(
                    thumbnailsPath,
                    getThumbnailName(mediaItem.displayName)
                ).absolutePath
            )
        } else BitmapFactory.decodeFile(mediaItem.thumbnailPath)
    }

    override suspend fun createThumbnail(
        path: String,
        mediaFileId: Long,
        fileName: String,
    ): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        return coroutineScope.async(context = Dispatchers.IO) {
            var image: Bitmap? = null
            try {
                val thumbnailName = getThumbnailName(fileName)
                val thumbnailPath =
                    (application.applicationContext.getExternalFilesDir(null)
                        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)
                val testFile = File(thumbnailPath, thumbnailName)

                if (testFile.exists()) {
                    application.mediaDatabase.mediaFileDao.updateThumbnail(
                        mediaFileId,
                        testFile.absolutePath
                    )
                    return@async null
                }

                decodeSampledBitmapFromStream(path)?.let {
                    image = it
                    saveBitmapToPrivateStorage(
                        bitmap = it,
                        fileName = thumbnailName,
                        directory = thumbnailPath
                    )?.let { file ->
                        application.mediaDatabase.mediaFileDao.updateThumbnail(
                            mediaFileId,
                            file.absolutePath
                        )
                    }
                }
                image
            } catch (e: Exception) {
                return@async null
            }
        }.await()
    }
}

class LocalNetStorageMediaFileService(val application: MediaApplication, private val smbClient: SmbClient) :
    MediaFileService<String> {

    override val allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_NET_STORAGE.path)

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        val start = (page - 1) * loadSize
        var end = page * loadSize - 1
        if (end > allData.size - 1)
            end = allData.size - 1
        val items = allData.slice(IntRange(start, end))

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                //1k分辨率以下的图像生成缩略图时同步, 1k以上分辨率不再同步,使用state延时重组
                if (item.fileSize < ImageSize.ONE_K.size) {
                    jobs.add(coroutineScope.launch(Dispatchers.IO) {
                        item.thumbnail = loadThumbnail(item)
                    })
                } else {
                    coroutineScope.launch(Dispatchers.IO) {
                        item.thumbnailState.value = loadThumbnail(item)
                    }
                }
            }
        }
        jobs.forEach {
            it.join()
        }
        val endDate = System.currentTimeMillis()
        val re = endDate - startDate
        println("测试:加载bitmap用时$re")
        return items
    }

    override fun next(page: Int, loadSize: Int): Int? {
        val start = page * loadSize
        if (start > allData.size) return null
        val result = page + 1
        return result
    }

    override suspend fun getAllDataForMediaList(param: String) {
        allData.clear()
        allData.addAll(smbClient.getList(param))
    }

    override suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return try {
            createThumbnail(
                path = mediaItem.data!!,
                mediaFileId = mediaItem.id,
                fileName = mediaItem.displayName
            ) ?: BitmapFactory.decodeFile(
                File(
                    thumbnailsPath,
                    getThumbnailName(mediaItem.displayName, otherStr = mediaItem.id.toString())
                ).absolutePath
            )
        }catch (e: Exception){
            null
        }
    }

    override suspend fun createThumbnail(
        path: String,
        mediaFileId: Long,
        fileName: String
    ): Bitmap? {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        return coroutineScope.async(context = Dispatchers.IO) {
            var image: Bitmap? = null
            try {
                val thumbnailName = getThumbnailName(name = fileName, otherStr = mediaFileId.toString())
                val thumbnailPath =
                    (application.applicationContext.getExternalFilesDir(null)
                        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_NET_STORAGE.path)
                val testFile = File(thumbnailPath, thumbnailName)

                if (testFile.exists()) return@async null
                smbClient.getImageThumbnail(name = fileName)?.let {
                    image = it
                    saveBitmapToPrivateStorage(
                        bitmap = it,
                        fileName = thumbnailName,
                        directory = thumbnailPath
                    )
                }
                image
            } catch (e: Exception) {
                return@async null
            }
        }.await()
    }

}
