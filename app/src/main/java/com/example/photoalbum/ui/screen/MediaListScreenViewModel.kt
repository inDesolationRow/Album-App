package com.example.photoalbum.ui.screen

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.LocalNetStorageThumbnailService
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.LocalStorageThumbnailService
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.enums.StorageType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.model.Menu
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MediaListScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings,
) : BaseViewModel(application, userAction, settings) {

    /**
     * 顶部bar的状态
     */
    var directoryName = mutableStateOf("")

    var directoryNum = mutableIntStateOf(0)

    var photosNum = mutableIntStateOf(0)

    private var localDirectoryInfo = 0 to 0

    /**
     * 弹出窗的状态
     */
    var showDialog by mutableStateOf(MediaListDialogEntity())

    /**
     * 拖拽抽屉相关的状态
     */
    val menuLocalStorage = -1

    val menuAddLocalNetStorage = -2

    val menuLocalNetMinimumId = 1

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    var menu: MutableState<List<Menu>> = mutableStateOf(listOf())

    var currentMenuItem: MutableState<Menu?> = mutableStateOf(null)

    private lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    /**
     * 本地媒体列表相关的状态
     * -1代表所有根目录
     */
    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    val localLevelStack: SnapshotStateList<Pair<Long, Int>> = mutableStateListOf()

    val localState = mutableStateOf(LazyGridState())

    private var recomposeLocalStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    lateinit var localMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>>

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    var localMediaFileService =
        LocalStorageThumbnailService(
            application,
            maxSize = settings.maxSizeLarge,
            initialLoadSize = settings.initialLoadSizeLarge
        )

    val back = mutableStateOf(false)

    /**
     * 本地网络相关的状态
     */
    private var newNetStorageInfoId: Int? = null

    val smbClient = SmbClient()

    var localNetMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>> = mutableStateOf(flowOf())

    val localNetLevelStack: SnapshotStateList<Pair<String, Int>> = smbClient.pathStack

    var editLocalNetStorageInfo by mutableStateOf(false)

    var recomposeLocalNetStorageListKey by mutableIntStateOf(0)

    val localNetState = mutableStateOf(LazyGridState())

    private lateinit var localNetMediaFileService: LocalNetStorageThumbnailService

    var jumpToView: Boolean = false

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
                updateDirectoryName(
                    true, if (it >= 0) {
                        application.mediaDatabase.directoryDao.getDirectoryNameById(it) ?: ""
                    } else {
                        "根路径"
                    }
                )
                if (!::localMediaFileFlow.isInitialized) {
                    localMediaFileFlow = mutableStateOf(initLocalMediaFilePaging())
                    localLevelStack.add(-1L to 0)
                } else {
                    initLocalMediaFilePaging(it)
                    if (localLevelStack.last().first != it) {
                        localLevelStack.add(it to 0)
                    }
                }
            }
        }

        viewModelScope.launch {
            recomposeLocalStorageListKey.collect {
                updateForUpdateKey(currentDirectoryId.value, it)
            }
        }

        viewModelScope.launch {
            super.userAction.collect {
                if (it is UserAction.ScanAction) {
                    if (it.scanState == ScanResult.SUCCESS) {
                        recomposeLocalStorageListKey.value += 1
                    }
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
            currentMenuItem.value = menu.value[0]

            //观察本地网络列表，由更改本地网络列表触发拖拽抽屉更新
            localNetStorageInfoListStateFlow.collect {
                menu.value = getMenuList(localNetStorageInfoListStateFlow.value)
                newNetStorageInfoId?.let {
                    updateSelectItem(it)
                    newNetStorageInfoId = null
                }
            }
        }
    }

    private fun getMenuList(list: MutableList<LocalNetStorageInfo>?): List<Menu> {
        val menuList: MutableList<Menu> = mutableListOf()
        menuList.add(
            Menu(
                menuLocalStorage,
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
                menuAddLocalNetStorage,
                application.getString(R.string.add_local_cloud),
                true,
                Icons.Filled.CloudSync
            )
        )
        return menuList
    }

    private fun updateSelectItem(id: Int) {
        val selectItem = menu.value.filter { it.id == id }
        currentMenuItem.value = selectItem.first()
    }

    fun updateDirectoryName(local: Boolean, name: String? = null) {
        if (local) {
            if (name == null) {
                viewModelScope.launch(Dispatchers.IO) {
                    directoryName.value = application.mediaDatabase.directoryDao.getDirectoryNameById(currentDirectoryId.value) ?: "根路径"
                }
            } else {
                directoryName.value = name
            }
        } else {
            directoryName.value = name.takeIf { !it.isNullOrEmpty() } ?: "根路径"
        }
    }

    fun updateDirectoryInfo(local: Boolean, directoryNum: Int? = null, imageNum: Int? = null) {
        if (local) {
            if (directoryNum != null && imageNum != null) {
                this.directoryNum.intValue = directoryNum
                photosNum.intValue = imageNum
                localDirectoryInfo = directoryNum to imageNum
            } else {
                this.directoryNum.intValue = localDirectoryInfo.first
                photosNum.intValue = localDirectoryInfo.second
            }
        } else {
            this.directoryNum.intValue = directoryNum ?: 0
            photosNum.intValue = imageNum ?: 0
        }
    }

    private fun initLocalMediaFilePaging(directoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            localMediaFileService =
                LocalStorageThumbnailService(
                    application,
                    maxSize = settings.maxSizeLarge,
                    initialLoadSize = settings.initialLoadSizeLarge
                )
            val result = localMediaFileService.getAllData(directoryId)
            updateDirectoryInfo(true, result.second, result.third)
            localMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = settings.pageSizeLarge,
                    initialLoadSize = settings.initialLoadSizeLarge,
                    prefetchDistance = settings.prefetchDistanceLarge,
                    maxSize = settings.maxSizeLarge
                )
            ) {
                MediaItemPagingSource(
                    localMediaFileService
                )
            }.flow.cachedIn(viewModelScope)
            System.gc()
        }
    }

    private suspend fun initLocalMediaFilePaging(): Flow<PagingData<MediaItem>> {
        val result = viewModelScope.async(Dispatchers.IO) {
            val result = localMediaFileService.getAllData(-1)
            updateDirectoryInfo(true, result.second, result.third)
            MediaItemPagingSource(
                localMediaFileService
            )
        }.await()
        return Pager(
            PagingConfig(
                pageSize = settings.pageSizeLarge,
                initialLoadSize = settings.initialLoadSizeLarge,
                prefetchDistance = settings.prefetchDistanceLarge,
                maxSize = settings.maxSizeLarge
            )
        ) {
            result
        }.flow.cachedIn(viewModelScope)
    }

    fun getItemCount(): Int {
        return localMediaFileService.allData.size
    }

    fun clearCache(start: Int, end: Int, type: StorageType) {
        try {
            val clearList = localMediaFileService.allData.toList()
            if (type == StorageType.LOCAL) {
                clearList.slice(IntRange(start, end)).onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            } else {
                clearList.slice(IntRange(start, end)).onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCache(type: StorageType) {
        try {
            if (type == StorageType.LOCAL) {
                val clearList = localMediaFileService.allData.toList()
                clearList.onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            } else{
                val clearList = localNetMediaFileService.allData.toList()
                clearList.onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun localMediaFileStackBack() {
        back.value = true
        localLevelStack.removeLast()
        currentDirectoryId.value = localLevelStack.last().first
    }

    /**
     * 强制触发更新
     *
     * @param directoryId 当前本地目录的id
     * @param updateKey 新的key，不得重复
     */
    private fun updateForUpdateKey(directoryId: Long, updateKey: Int) {
        updateKey.let {
            if (updateKey != 0) initLocalMediaFilePaging(directoryId)
        }
    }

    fun delLocalNetStorageInfoInMenu(id: Int) {
        val list = localNetStorageInfoListStateFlow.value.toMutableList()
        val del = list.filter { it.id == id }
        list.remove(del.first())
        localNetStorageInfoListStateFlow.value = list
        updateSelectItem(menuAddLocalNetStorage)
    }

    fun addLocalNetStorageInfo(localNetStorageInfo: LocalNetStorageInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            newNetStorageInfoId =
                application.mediaDatabase.localNetStorageInfoDao.insert(localNetStorageInfo).toInt()
            val update = localNetStorageInfoListStateFlow.value.toMutableList()
            localNetStorageInfo.id = newNetStorageInfoId!!
            update.add(localNetStorageInfo)
            localNetStorageInfoListStateFlow.value = update
        }
    }

    fun isConnect(): Boolean {
        return smbClient.isConnect()
    }

    suspend fun connectSmb(
        ip: String,
        user: String,
        pwd: String?,
        shared: String,
        reconnection: Boolean = false,
    ): ConnectResult {
        return viewModelScope.async(Dispatchers.IO) {
            val result = smbClient.connect(ip, user, pwd, shared, reconnection)
            application.localNetStorageInfo = LocalNetStorageInfo(
                ip = ip,
                user = user,
                password = pwd ?: "",
                shared = shared
            )
            result
        }.await()
    }

    suspend fun connectSmb(id: Int, reconnection: Boolean = false): ConnectResult {
        val localNetStorageInfo = application.mediaDatabase.localNetStorageInfoDao.getById(id)
        return localNetStorageInfo?.let {
            val result = connectSmb(
                localNetStorageInfo.ip,
                localNetStorageInfo.user,
                localNetStorageInfo.password,
                localNetStorageInfo.shared,
                reconnection = reconnection
            )
            application.localNetStorageInfo = localNetStorageInfo
            result
        } ?: ConnectResult.ConnectError("database_error")
    }

    fun delLocalNetStorageInfo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            application.mediaDatabase.localNetStorageInfoDao.deleteById(id)
        }
    }

    suspend fun getLocalNetStorageInfo(id: Int): LocalNetStorageInfo? {
        return application.mediaDatabase.localNetStorageInfoDao.getById(id)
    }

    fun updateLocalNetStorage(localNetStorageInfo: LocalNetStorageInfo) {
        viewModelScope.launch {
            application.mediaDatabase.localNetStorageInfoDao.update(localNetStorageInfo)
        }
    }

    fun initLocalNetMediaFilePaging(path: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val test = path ?: ""
            updateDirectoryName(false, path ?: "根路径")
            localNetMediaFileService = LocalNetStorageThumbnailService(
                application,
                smbClient,
                maxSize = settings.initialLoadSizeLarge,
                initialLoadSize = settings.maxSizeLarge
            )
            val result = localNetMediaFileService.getAllData(test)
            updateDirectoryInfo(false, result.second, result.third)
            localNetMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = settings.pageSizeLarge,
                    initialLoadSize = settings.initialLoadSizeLarge,
                    prefetchDistance = settings.prefetchDistanceLarge,
                    maxSize = settings.maxSizeLarge
                )
            ) {
                MediaItemPagingSource(
                    localNetMediaFileService
                )
            }.flow.cachedIn(viewModelScope)
            System.gc()
        }
    }

    fun localNetStackBack(): String {
        back.value = true
        return smbClient.back()
    }
}