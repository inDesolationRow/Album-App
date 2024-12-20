package com.example.photoalbum.data

import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.SystemFolder
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.model.MediaItem

class MediaItemDataSource(private val application: MediaApplication) {

    var allData: MutableList<MediaItem> = mutableListOf()

    val items: DataList = DataList(allData, onGet = {})

    val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
        ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)

    var selectItemIndex: Int = 0

    suspend fun getAllData(param: Long, onlyMediaFile: Boolean, selectItemId: Long): Int {
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
}

class DataList(private val list: MutableList<MediaItem>, private val onGet: (Int) -> Unit) {
    operator fun get(index: Int): MediaItem {
        onGet(index)
        return list[index]
    }
}