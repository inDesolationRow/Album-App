package com.example.photoalbum.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enums.Direction
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.SystemFolder
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.utils.decodeSampledBitmap
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

interface DataService<T> {

    val thumbnailsPath: String

    var allData: MutableList<MediaItem>

    val items: DataList

    suspend fun getAllData(param: T, onlyMediaFile: Boolean, selectItemId: Long): Int

    suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap?

    suspend fun createThumbnail(
        path: String,
        mediaFileId: Long,
        fileName: String,
        orientation: Float,
    ): Bitmap?

}

class LocalDataSource(
    private val application: MediaApplication,
    private val loadSize: Int,
    maxSize: Int,
) : DataService<Long> {

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val items =
        DataList(
            allData,
            loadSize = loadSize,
            maxSize = maxSize,
            onClear = { top, bottom ->
                val items = allData.subList(top, bottom + 1)
                items.onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            }) { top, bottom ->
            val items = allData.subList(top, bottom + 1)
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            val jobs: MutableList<Job> = mutableListOf()
            val startDate = System.currentTimeMillis()
            coroutineScope.launch {
                for (item in items) {
                    if (item.type == ItemType.IMAGE) {
                        if (item.thumbnail == null && item.thumbnailState.let {
                                if (it.value == null) return@let true
                                else return@let it.value!!.isRecycled
                            }) {
                            //加载第一页制作缩略图时全部使用state以节省时间 后续只有大图生成缩略图才使用state
                            if ((item.fileSize > ImageSize.M_1.size && item.thumbnailPath.isNullOrEmpty()) ||
                                (items.size == 2 * loadSize && item.thumbnailPath.isNullOrEmpty())
                            ) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    item.thumbnailState.value = loadThumbnail(item)
                                }
                            } else {
                                jobs.add(coroutineScope.launch(Dispatchers.IO) {
                                    item.thumbnail = loadThumbnail(item)
                                })
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
            }
        }

    override suspend fun getAllData(param: Long, onlyMediaFile: Boolean, selectItemId: Long): Int {
        var index = -1
        if (!onlyMediaFile) {
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
                    when {
                        name.contains(SystemFolder.DCIM.displayName) -> {
                            order1.add(item)
                        }

                        name.contains(SystemFolder.CAMERA.displayName) -> {
                            order1.add(item)
                        }

                        name.contains(SystemFolder.DOCUMENT.displayName) -> {
                            order2.add(item)
                        }

                        name.contains(SystemFolder.DOWNLOAD.displayName) -> {
                            order2.add(item)
                        }

                        name.contains(SystemFolder.PICTURES.displayName) -> {
                            order2.add(item)
                        }

                        name.contains(SystemFolder.SCREENSHOTS.displayName) -> {
                            order2.add(item)
                        }

                        else -> {
                            order3.add(item)
                        }
                    }
                }
                allData.addAll(order1)
                allData.addAll(order2)
                allData.addAll(order3)
            }

        }

        val mediaList =
            application.mediaDatabase.directoryDao.querySortedMediaFilesByDirectoryId(param)
        if (mediaList.isNullOrEmpty()) return index
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
            if (mediaFile.mediaFileId == selectItemId) {
                index = allData.size
            }
            allData.add(item)
        }
        println("测试:边界 ${allData.size}")
        return index
    }

    override suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return if (mediaItem.thumbnailPath.isNullOrEmpty()) {
            createThumbnail(
                mediaItem.data!!,
                mediaItem.id,
                mediaItem.displayName,
                mediaItem.orientation.toFloat()
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
        orientation: Float,
    ): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        return coroutineScope.async(context = Dispatchers.IO) {
            var image: Bitmap? = null
            try {
                val thumbnailName = getThumbnailName(fileName)
                val testFile = File(thumbnailsPath, thumbnailName)

                if (testFile.exists()) {
                    application.mediaDatabase.mediaFileDao.updateThumbnail(
                        mediaFileId,
                        testFile.absolutePath
                    )
                    return@async null
                }

                decodeSampledBitmap(filePath = path, orientation = orientation)?.let {
                    image = it
                    saveBitmapToPrivateStorage(
                        bitmap = it,
                        fileName = thumbnailName,
                        directory = thumbnailsPath
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

class LocalNetDataSource(
    val application: MediaApplication,
    private val loadSize: Int,
    private val smbClient: SmbClient,
    maxSize: Int,
) : DataService<String> {

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val items: DataList = DataList(allData,
        loadSize = loadSize,
        maxSize = maxSize,
        onClear = { top, bottom ->
            val items = allData.subList(top, bottom + 1)
            items.onEach { item ->
                item.thumbnail?.recycle()
                item.thumbnail = null
                item.thumbnailState.value?.recycle()
                item.thumbnailState.value = null
            }
        }) { top, bottom ->
        val items = allData.subList(top, bottom + 1)

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        coroutineScope.launch(Dispatchers.IO) {
            for (item in items) {
                if (item.type == ItemType.IMAGE) {
                    if (item.thumbnail == null && item.thumbnailState.let {
                            if (it.value == null) return@let true
                            else return@let it.value!!.isRecycled
                        }) {
                        val thumbnailName =
                            getThumbnailName(name = item.displayName, otherStr = item.id.toString())
                        val testFile = File(thumbnailsPath, thumbnailName)
                        if (item.fileSize > ImageSize.M_1.size && !testFile.exists() ||
                            (items.size == 2 * loadSize && item.thumbnailPath.isNullOrEmpty())
                        ) {
                            coroutineScope.launch(Dispatchers.IO) {
                                item.thumbnailState.value = loadThumbnail(item)
                            }
                        } else {
                            jobs.add(coroutineScope.launch(Dispatchers.IO) {
                                item.thumbnail = loadThumbnail(item)
                            })
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
        }
    }

    override suspend fun getAllData(
        param: String,
        onlyMediaFile: Boolean,
        selectItemId: Long,
    ): Int {
        allData.clear()
        allData.addAll(smbClient.getList(param, onlyMediaFile))
        return allData.indexOfFirst { selectItemId == it.id }
    }

    override suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return try {
            createThumbnail(
                mediaFileId = mediaItem.id,
                fileName = mediaItem.displayName,
                path = "",
                orientation = 0f
            ) ?: BitmapFactory.decodeFile(
                File(
                    thumbnailsPath,
                    getThumbnailName(mediaItem.displayName, otherStr = mediaItem.id.toString())
                ).absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createThumbnail(
        path: String,
        mediaFileId: Long,
        fileName: String,
        orientation: Float,
    ): Bitmap? {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        return coroutineScope.async(context = Dispatchers.IO) {
            var image: Bitmap? = null
            try {
                val thumbnailName =
                    getThumbnailName(name = fileName, otherStr = mediaFileId.toString())
                val testFile = File(thumbnailsPath, thumbnailName)

                if (testFile.exists()) return@async null
                smbClient.getImageThumbnail(name = fileName, mediaFileId)?.let {
                    image = it
                    saveBitmapToPrivateStorage(
                        bitmap = it,
                        fileName = thumbnailName,
                        directory = thumbnailsPath
                    )
                }
                image
            } catch (e: Exception) {
                return@async null
            }
        }.await()
    }


}

class DataList(
    private val list: MutableList<MediaItem>,
    private val loadSize: Int,
    private val maxSize: Int,
    private val onClear: (Int, Int) -> Unit,
    private val onGet: (Int, Int) -> Unit,
) {
    private var topEdge = -1

    private var bottomEdge = -1

    private var previousIndex = -1

    operator fun get(index: Int): MediaItem {
        val op: Direction
        if (previousIndex == -1) {
            previousIndex = index
            op = Direction.NULL
        } else {
            op = (index - previousIndex).let {
                if (it > 0) return@let Direction.RIGHT
                else return@let Direction.LEFT
            }
        }

        when (op) {
            Direction.LEFT -> {
                if (topEdge + 10 == index) {
                    topEdge = max(topEdge - loadSize, 0)
                    onGet(topEdge, index - 10 - 1)
                    //println("测试:触发加载 上边界$topEdge 下边界$bottomEdge index$index op$op")
                }
                if (bottomEdge - topEdge >= maxSize) {
                    //println("测试:清理 上边界$topEdge 下边界$bottomEdge index$index op$op")
                    //println("测试:onClear 上边界${bottomEdge - (maxSize - loadSize * 2) + 1} 下边界${bottomEdge} index$index op$op")
                    onClear(bottomEdge - (maxSize - loadSize * 2) + 1, bottomEdge)
                    bottomEdge -= (maxSize - loadSize * 2)
                }
            }

            Direction.NULL -> {
                if (index <= loadSize - 1) {
                    topEdge = 0
                    bottomEdge = min(loadSize * 2 - 1, size() - 1)
                    onGet(topEdge, bottomEdge)
                } else {
                    topEdge = index - (loadSize - 1)
                    bottomEdge = min(index + (loadSize - 1), size() - 1)
                    onGet(topEdge, bottomEdge)
                }
                //println("测试:初始加载量 上边界$topEdge 下边界$bottomEdge index$index op$op")
            }

            Direction.RIGHT -> {
                if (bottomEdge - 10 == index) {
                    bottomEdge = min(bottomEdge + loadSize, size() - 1)
                    onGet(index + 10 + 1, bottomEdge)
                    //println("测试:op>0 上边界$topEdge 下边界$bottomEdge index$index op$op")
                }
                if (bottomEdge - topEdge >= maxSize) {
                    //println("测试:清理 上边界$topEdge 下边界$bottomEdge index$index op$op")
                    //println("测试:onClear 上边界$topEdge 下边界${topEdge + maxSize - loadSize * 2 - 1} index$index op$op")
                    onClear(topEdge, topEdge + maxSize - loadSize * 2 - 1)
                    topEdge = topEdge + maxSize - loadSize * 2
                }
            }
        }
        previousIndex = index
        return list[index]
    }

    fun size(): Int {
        return list.size
    }
}