package com.example.photoalbum.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.Menu
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.utils.decodeSampledBitmapFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Semaphore

class MediaListScreenViewModel(
    private val application: MediaApplication,
    userAction: MutableStateFlow<UserAction>
) :
    BaseViewModel(application, userAction) {

    //-1代表所有根目录
    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    var recomposeKey: MutableStateFlow<Int> = MutableStateFlow(0)

    var items: SnapshotStateList<MediaItem> = mutableStateListOf()

    lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    lateinit var menu: MutableState<List<Menu>>

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    lateinit var selectedItem: MutableState<Menu>

    private var newLocalNetStorageInfoId: Int? = null

    private val menuMediaListId = -1

    private val menuAddLocalNetStorageId = -2

    val notPreview = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
                if (it >= -1) {
                    println("测试:通过currentDirectoryId更新")
                    try {
                        loadDirectoryAndMediaFile(it)
                    }catch (e: Exception){
                        println("错误原因${e.message}")
                    }
                }
            }
        }

        viewModelScope.launch {
            recomposeKey.collect {
                println("测试:通过recomposeKey更新")
                updateForUpdateKey(currentDirectoryId.value, it)
            }
        }

        viewModelScope.launch {
            super.userAction.collect {
                when (val action = it) {
                    is UserAction.ExpandStatusBarAction -> {
                    }

                    is UserAction.ScanAction -> {
                        if (action.end) {
                            recomposeKey.value += 1
                        }
                    }

                    UserAction.NoneAction -> {}
                }
            }
        }

        //初始化拖拽抽屉所需的数据
        viewModelScope.launch(context = Dispatchers.IO) {
            val localNetStorageInfoList =
                application.mediaDatabase.localNetStorageInfoDao.getList()?.toMutableList()
            localNetStorageInfoListStateFlow =
                MutableStateFlow(localNetStorageInfoList ?: mutableListOf())
            //生成Menu列表
            menu = mutableStateOf(getMenuList(localNetStorageInfoList))
            selectedItem = mutableStateOf(menu.value[0])

            //观察本地网络列表，由更改本地网络列表触发拖拽抽屉更新
            localNetStorageInfoListStateFlow.collect {
                menu.value = getMenuList(localNetStorageInfoListStateFlow.value)
                newLocalNetStorageInfoId?.let {
                    updateSelectItem(it)
                    newLocalNetStorageInfoId = null
                }
            }
        }
    }

    private suspend fun loadDirectoryAndMediaFile(directoryId: Long) {
        val start = System.currentTimeMillis()
        val originalDirectoryList =
            application.mediaDatabase.directoryDao.queryDirectoryWithMediaFileByParentId(parentId = directoryId)
                ?: mutableStateListOf()
        val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
            ?: application.applicationContext.filesDir).absolutePath.plus("/Thumbnail")
        val semaphore = Semaphore(32)
        items.clear()
        val jobs: MutableList<Job> = mutableListOf()

        //遍历需要显示的子目录
        for (dir in originalDirectoryList) {
            val item = MediaItem(
                id = dir.directory.directoryId,
                type = ItemType.DIRECTORY,
                displayName = dir.directory.displayName,
                mimeType = ""
            )
            items.add(item)
            //获取缩略图
            val job = viewModelScope.launch(context = Dispatchers.IO) {
                semaphore.acquire()
                if (dir.mediaFileList.isNotEmpty() && dir.mediaFileList[0].data.isNotBlank()) {
                    dir.mediaFileList[0].thumbnail.isBlank().let {
                        val thumbnail = if (it) {
                            createThumbnail(
                                dir.mediaFileList[0].data,
                                dir.mediaFileList[0].mediaFileId,
                                dir.mediaFileList[0].displayName
                            ) ?: decodeSampledBitmapFromStream(
                                File(
                                    thumbnailsPath,
                                    dir.mediaFileList[0].displayName.split(".").first()
                                        .plus("_thumbnail.png")
                                ).absolutePath
                            )
                        } else decodeSampledBitmapFromStream(dir.mediaFileList[0].data)
                        viewModelScope.launch(context = Dispatchers.Main) {
                            item.thumbnail.value = thumbnail
                        }
                    }
                }
                semaphore.release()
            }
            jobs.add(job)
        }

        //遍历需要显示的子文件，如果directoryId = -1 则显示的是根目录无子文件不需要遍历
        if (directoryId == -1L) return
        val directory =
            application.mediaDatabase.directoryDao.queryDirectoryWithMediaFileById(directoryId = directoryId)
        if (directory == null || directory.mediaFileList.isEmpty()) return
        val tempList : MutableList<MediaItem> = mutableListOf()
        println("测试: 列表大小${directory.mediaFileList.size}")
        for (mediaFile in directory.mediaFileList) {
            val item = MediaItem(
                id = mediaFile.mediaFileId,
                type = mediaFile.mimeType.let {
                    val test = it.lowercase()
                    return@let when {
                        test.contains("image") -> { ItemType.IMAGE }
                        test.contains("video") -> { ItemType.VIDEO }
                        else -> { ItemType.ERROR }
                    }
                },
                displayName = mediaFile.displayName,
                mimeType = mediaFile.mimeType
            )

            tempList.add(item)
            if (tempList.size >= 200 || tempList.size == directory.mediaFileList.size){
                items.addAll(tempList)
            }

            /*val job = viewModelScope.launch(context = Dispatchers.IO) {
                semaphore.acquire()
                val thumbnail = if (mediaFile.thumbnail.isNotBlank()) {
                    createThumbnail(
                        mediaFile.data,
                        mediaFile.mediaFileId,
                        mediaFile.displayName
                    ) ?: decodeSampledBitmapFromStream(
                        File(
                            thumbnailsPath,
                            mediaFile.displayName.split(".").first()
                                .plus("_thumbnail.png")
                        ).absolutePath
                    )
                } else decodeSampledBitmapFromStream(mediaFile.data)
                item.thumbnail.value = thumbnail
                semaphore.release()
            }*/
            //jobs.add(job)
        }
        jobs.forEach{
            it.join()
        }
        val end = System.currentTimeMillis()
        val d = end - start
        println("测试:渲染用时$d")
    }
/*    private suspend fun loadDirectory(directoryId: Long) {
        val start = System.currentTimeMillis()
        val originalDirectoryList =
            application.mediaDatabase.directoryDao.queryDirectoryWithMediaFileByParentId(parentId = directoryId)
                ?: mutableStateListOf()
        items.clear()
        if (originalDirectoryList.isEmpty()) return
        val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
            ?: application.applicationContext.filesDir).absolutePath.plus("/Thumbnail")
        val semaphore = Semaphore(10)
        val jobs: MutableList<Job> = mutableListOf()

        //遍历需要显示的子目录
        for (dir in originalDirectoryList) {
            val item = MediaItem(
                id = dir.directory.directoryId,
                type = ItemType.DIRECTORY,
                displayName = dir.directory.displayName,
                mimeType = ""
            )
            items.add(item)
            //获取缩略图
            val job = viewModelScope.launch(context = Dispatchers.IO) {
                semaphore.acquire()
                if (dir.mediaFileList.isNotEmpty() && dir.mediaFileList[0].data.isNotBlank()) {
                    dir.mediaFileList[0].thumbnail.isBlank().let {
                        val thumbnail = if (it) {
                            createThumbnail(
                                dir.mediaFileList[0].data,
                                dir.mediaFileList[0].mediaFileId,
                                dir.mediaFileList[0].displayName
                            ) ?: decodeSampledBitmapFromStream(
                                File(
                                    thumbnailsPath,
                                    dir.mediaFileList[0].displayName.split(".").first()
                                        .plus("_thumbnail.png")
                                ).absolutePath
                            )
                        } else decodeSampledBitmapFromStream(dir.mediaFileList[0].data)
                        viewModelScope.launch(context = Dispatchers.Main) {
                            item.thumbnail.value = thumbnail
                        }
                    }
                }
                semaphore.release()
            }
            jobs.add(job)
        }

        jobs.forEach{
            it.join()
        }
        val end = System.currentTimeMillis()
        val d = end - start
        println("测试:渲染目录用时 $d ms")
    }

    private suspend fun loadMediaFile(directoryId: Long){
        if (directoryId == -1L) return
        val thumbnailsPath = (application.applicationContext.getExternalFilesDir(null)
            ?: application.applicationContext.filesDir).absolutePath.plus("/Thumbnail")
        val semaphore = Semaphore(22)
        val jobs: MutableList<Job> = mutableListOf()
        val start = System.currentTimeMillis()
        val directory =
            application.mediaDatabase.directoryDao.queryDirectoryWithMediaFileById(directoryId = directoryId)
        if (directory == null || directory.mediaFileList.isEmpty()) return
        for (mediaFile in directory.mediaFileList) {
            val item = MediaItem(
                id = mediaFile.mediaFileId,
                type = mediaFile.mimeType.let {
                    val test = it.lowercase()
                    return@let when {
                        test.contains("image") -> { ItemType.IMAGE }
                        test.contains("video") -> { ItemType.VIDEO }
                        else -> { ItemType.ERROR }
                    }
                },
                displayName = mediaFile.displayName,
                mimeType = mediaFile.mimeType
            )
            items.add(item)
            val job = viewModelScope.launch(context = Dispatchers.IO) {
                semaphore.acquire()
                val thumbnail = if (mediaFile.thumbnail.isNotBlank()) {
                    createThumbnail(
                        mediaFile.data,
                        mediaFile.mediaFileId,
                        mediaFile.displayName
                    ) ?: decodeSampledBitmapFromStream(
                        File(
                            thumbnailsPath,
                            mediaFile.displayName.split(".").first()
                                .plus("_thumbnail.png")
                        ).absolutePath
                    )
                } else decodeSampledBitmapFromStream(mediaFile.data)
                item.thumbnail.value = thumbnail
                semaphore.release()
            }
            jobs.add(job)
        }
        jobs.forEach{
            it.join()
        }
        val end = System.currentTimeMillis()
        val d = end - start
        println("测试:渲染文件用时 $d ms")
    }*/
    /**
     * 强制触发更新
     *
     * @param directoryId 当前目录的id
     * @param updateKey 新的key，不得重复
     */
    private suspend fun updateForUpdateKey(directoryId: Long, updateKey: Int) {
        updateKey.let {
            if (updateKey != 0)
                loadDirectoryAndMediaFile(directoryId)
        }
    }

    fun delLocalNetStorageInfo() {
        val list = localNetStorageInfoListStateFlow.value.toMutableList()
        val del = list.filter { it.id == selectedItem.value.id }
        list.remove(del.first())
        localNetStorageInfoListStateFlow.value = list
        viewModelScope.launch(Dispatchers.IO) {
            application.mediaDatabase.localNetStorageInfoDao.deleteById(del.first().id)
        }
        updateSelectItem(menuAddLocalNetStorageId)
    }

    fun addLocalNetStorageInfo(info: LocalNetStorageInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            newLocalNetStorageInfoId =
                application.mediaDatabase.localNetStorageInfoDao.insert(info).toInt()
            val update = localNetStorageInfoListStateFlow.value.toMutableList()
            info.id = newLocalNetStorageInfoId!!
            update.add(info)
            localNetStorageInfoListStateFlow.value = update

        }
    }

    private fun updateSelectItem(id: Int) {
        val selectItem = menu.value.filter { it.id == id }
        selectedItem.value = selectItem.first()
    }

    private fun getMenuList(list: MutableList<LocalNetStorageInfo>?): List<Menu> {
        val menuList: MutableList<Menu> = mutableListOf()
        menuList.add(
            Menu(
                menuMediaListId,
                application.getString(R.string.local_file),
                true,
                Icons.Filled.Folder
            )
        )
        if (!list.isNullOrEmpty()) {
            for (info in list) {
                menuList.add(Menu(info.id, info.displayName, true, Icons.Filled.Cloud))
            }
        }
        menuList.add(
            Menu(
                menuAddLocalNetStorageId,
                application.getString(R.string.add_local_cloud),
                true,
                Icons.Filled.CloudSync
            )
        )
        return menuList
    }

}