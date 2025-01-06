package com.example.photoalbum.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.MutableState
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.SystemFolder
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.utils.blurBitmap
import com.example.photoalbum.utils.decodeBitmap
import com.example.photoalbum.utils.decodeSampledBitmap
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

class MediaItemPagingSource(
    private val apiService: MediaFileService<*>,
) :
    PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return try {
            val page = params.key ?: 1
            val response = apiService.getData(page, params.loadSize)/*if (page == 1) {
                if (directoryCount > params.loadSize)
                    apiService.getData(page, directoryCount)
                else
                    apiService.getData(page, params.loadSize)
            } else {
                apiService.getData(page, params.loadSize)
            }*/
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

    fun close() {
        invalidate()
    }
}

interface MediaFileService<T> {

    var allData: MutableList<MediaItem>

    val thumbnailsPath: String

    var initialLoadSize: Int

    var maxSize: Int

    var selectItemIndex: Int

    suspend fun getAllData(param: T, onlyMediaFile: Boolean = false, selectItemId: Long = -1): Int

    fun sharedAllData(allData: MutableList<MediaItem>? = null): MutableList<MediaItem>

    suspend fun getData(page: Int, loadSize: Int): List<MediaItem>

    fun next(page: Int, loadSize: Int): Int?

    fun getItemIndex(id: Long): Int
}

class LocalStorageThumbnailService(
    private val application: MediaApplication,
    override var initialLoadSize: Int,
    override var maxSize: Int,
) :
    MediaFileService<Long> {

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    override var selectItemIndex: Int = 0

    override fun sharedAllData(allData: MutableList<MediaItem>?): MutableList<MediaItem> {
        allData?.let {
            this.allData = it
        }
        return this.allData
    }

    override suspend fun getAllData(param: Long, onlyMediaFile: Boolean, selectItemId: Long): Int {
        allData.clear()
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
                selectItemIndex = index
            }
            allData.add(item)
        }
        return index
    }

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        val start = (page - 1) * loadSize
        val end = min(page * loadSize - 1, allData.size - 1)
        val items = allData.slice(IntRange(start, end))
        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                if (item.thumbnail == null && item.thumbnailState.let {
                        if (it.value == null) return@let true
                        else return@let it.value!!.isRecycled
                    }) {
                    //加载第一页制作缩略图时全部使用state以节省时间 后续只有大图生成缩略图才使用state
                    if ((item.fileSize > ImageSize.M_1.size && item.thumbnailPath.isNullOrEmpty()) ||
                        (loadSize == initialLoadSize && item.thumbnailPath.isNullOrEmpty())
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
        return items
    }

    override fun next(page: Int, loadSize: Int): Int? {
        val start = page * loadSize
        if (start > allData.size - 1) return null
        return page + 1
    }

    override fun getItemIndex(id: Long): Int {
        return allData.indexOfFirst { id == it.id }
    }

    private suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
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

    private suspend fun createThumbnail(
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

class LocalStorageMediaFileService(
    private val application: MediaApplication,
    override var initialLoadSize: Int,
    override var maxSize: Int,
) :
    MediaFileService<Long> {

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath: String = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)
    override var selectItemIndex: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun sharedAllData(allData: MutableList<MediaItem>?): MutableList<MediaItem> {
        allData?.let {
            this.allData = it
        }
        return this.allData
    }

    override suspend fun getAllData(param: Long, onlyMediaFile: Boolean, selectItemId: Long): Int {
        allData.clear()
        var index = -1
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
            if (mediaFile.mediaFileId == selectItemId) index = allData.size
            allData.add(item)
        }
        return index
    }

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        if (page == 1) initialLoadSize = loadSize
        val start = (page - 1) * page.let {
            return@let if (it == 2) initialLoadSize else loadSize
        }
        var end = page.let {
            return@let if (page == 2) start + loadSize else page * loadSize
        } - 1

        if (end > allData.size - 1)
            end = allData.size - 1
        val items = allData.slice(IntRange(start, end))

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                item.thumbnail = loadThumbnail(mediaItem = item)
                coroutineScope.launch(Dispatchers.IO) {
                    loadData(item.data!!, item.dataBitmap, item.orientation.toFloat())
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
        if (start > allData.size - 1) return null
        return page + 1
    }

    override fun getItemIndex(id: Long): Int {
        return allData.indexOfFirst { id == it.id }
    }

    private fun loadData(path: String, state: MutableState<Bitmap?>, orientation: Float) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            state.value = decodeBitmap(path, orientation)
        }
    }

    private fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return BitmapFactory.decodeFile(
            mediaItem.thumbnailPath ?: File(
                thumbnailsPath,
                getThumbnailName(mediaItem.displayName)
            ).absolutePath
        )?.let {
            return@let blurBitmap(
                bitmap = it,
                context = application.applicationContext,
                radius = 5f
            )
        }
    }

}

class LocalNetStorageMediaFileService(
    val application: MediaApplication,
    private val smbClient: SmbClient, override var initialLoadSize: Int, override var maxSize: Int,
) :
    MediaFileService<String> {

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath: String = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    override var selectItemIndex: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun sharedAllData(allData: MutableList<MediaItem>?): MutableList<MediaItem> {
        allData?.let {
            this.allData = it
        }
        return this.allData
    }

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        if (page == 1) initialLoadSize = loadSize
        val start = (page - 1) * page.let {
            return@let if (it == 2) initialLoadSize else loadSize
        }
        var end = page.let {
            return@let if (page == 2) start + loadSize else page * loadSize
        } - 1
        if (end > allData.size - 1)
            end = allData.size - 1
        val items = allData.slice(IntRange(start, end))

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                loadThumbnail(item)?.let {
                    item.thumbnail = blurBitmap(
                        context = application.applicationContext,
                        bitmap = it,
                        radius = 5f
                    )
                }
                coroutineScope.launch {
                    loadData(
                        mediaItem = item,
                        smbClient = smbClient,
                        mutableState = item.dataBitmap
                    )
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

    private fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return try {
            BitmapFactory.decodeFile(
                File(
                    thumbnailsPath,
                    getThumbnailName(mediaItem.displayName, otherStr = mediaItem.id.toString())
                ).absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun loadData(
        mediaItem: MediaItem,
        smbClient: SmbClient,
        mutableState: MutableState<Bitmap?>,
    ) {
        try {
            mutableState.value = smbClient.getImage(
                getThumbnailName(
                    name = mediaItem.displayName,
                    otherStr = mediaItem.id.toString()
                ), mediaItem.id
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun next(page: Int, loadSize: Int): Int? {
        val start = page * loadSize
        if (start > allData.size) return null
        val result = page + 1
        return result
    }

    override fun getItemIndex(id: Long): Int {
        return allData.indexOfFirst { id == it.id }
    }

    override suspend fun getAllData(
        param: String,
        onlyMediaFile: Boolean,
        selectItemId: Long,
    ): Int {
        TODO("Not yet implemented")
    }

}

class LocalNetStorageThumbnailService(
    val application: MediaApplication,
    private val smbClient: SmbClient, override var maxSize: Int, override var initialLoadSize: Int,
) :
    MediaFileService<String> {

    override var allData: MutableList<MediaItem> = mutableListOf()

    override val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_NET_STORAGE.path)

    override var selectItemIndex: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun sharedAllData(allData: MutableList<MediaItem>?): MutableList<MediaItem> {
        allData?.let {
            this.allData = it
        }
        return this.allData
    }

    override suspend fun getData(page: Int, loadSize: Int): List<MediaItem> {
        val start = (page - 1) * loadSize
        val end = min(page * loadSize - 1, allData.size - 1)
        val items = allData.slice(IntRange(start, end))

        val startDate = System.currentTimeMillis()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val jobs: MutableList<Job> = mutableListOf()
        for (item in items) {
            if (item.type == ItemType.IMAGE) {
                if (item.thumbnail == null && item.thumbnailState.let {
                        if (it.value == null) return@let true
                        else return@let it.value!!.isRecycled
                    }) {
                    val thumbnailName =
                        getThumbnailName(name = item.displayName, otherStr = item.id.toString())
                    val testFile = File(thumbnailsPath, thumbnailName)
                    if (item.fileSize > ImageSize.M_1.size && !testFile.exists()) {
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
        return items
    }

    override fun next(page: Int, loadSize: Int): Int? {
        val start = page * loadSize
        if (start > allData.size) return null
        val result = page + 1
        return result
    }

    override fun getItemIndex(id: Long): Int {
        return allData.indexOfFirst { id == it.id }
    }

    override suspend fun getAllData(
        param: String,
        onlyMediaFile: Boolean,
        selectItemId: Long,
    ): Int {
        allData.clear()
        allData.addAll(smbClient.getList(param, onlyMediaFile))
        return getItemIndex(selectItemId)
    }

    private suspend fun loadThumbnail(mediaItem: MediaItem): Bitmap? {
        return try {
            createThumbnail(
                mediaFileId = mediaItem.id,
                fileName = mediaItem.displayName
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

    private suspend fun createThumbnail(
        mediaFileId: Long,
        fileName: String,
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
