package com.example.photoalbum.ui.screen

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
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.LocalNetStorageThumbnailService
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.LocalStorageThumbnailService
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.data.model.Settings
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
import kotlin.math.max

class MediaListScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

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

    var selectedItem: MutableState<Menu?> = mutableStateOf(null)

    private lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    /**
     * 本地媒体列表相关的状态
     * -1代表所有根目录
     */
    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    val localLevelStack: SnapshotStateList<Pair<Long, Int>> = mutableStateListOf()

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    private var recomposeLocalStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    lateinit var localMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>>

    private var localMediaFileService =
        LocalStorageThumbnailService(application, maxSize = 90, initialLoadSize = 60)

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

    private lateinit var localNetMediaFileService: LocalNetStorageThumbnailService

    var jumpToView: Boolean = false

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
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
                    if (it.end) {
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
            selectedItem.value = menu.value[0]

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
        selectedItem.value = selectItem.first()
    }

    private fun initLocalMediaFilePaging(directoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            localMediaFileService =
                LocalStorageThumbnailService(application, maxSize = 90, initialLoadSize = 60)
            localMediaFileService.getAllData(directoryId)
            localMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = 30,
                    initialLoadSize = 60,
                    prefetchDistance = 20,
                    maxSize = 90
                )
            ) {
                MediaItemPagingSource(localMediaFileService)
            }.flow
            System.gc()
        }
    }

    private suspend fun initLocalMediaFilePaging(): Flow<PagingData<MediaItem>> {
        val result = viewModelScope.async(Dispatchers.IO) {
            localMediaFileService.getAllData(-1)
            MediaItemPagingSource(localMediaFileService)
        }.await()
        return Pager(
            PagingConfig(pageSize = 30, initialLoadSize = 60, prefetchDistance = 20, maxSize = 90)
        ) {
            result
        }.flow
    }

    fun getItemCount(): Int {
        return localMediaFileService.allData.size
    }

    fun clearCache(start: Int, end: Int, type: StorageType) {
        if (type == StorageType.LOCAL)
            localMediaFileService.allData.slice(IntRange(start, end)).onEach { item ->
                item.thumbnail?.recycle()
                item.thumbnail = null
                //it.thumbnailState.value?.recycle()
                //it.thumbnailState.value = null
            } else
            localNetMediaFileService.allData.slice(IntRange(start, end)).onEach { item ->
                item.thumbnail?.recycle()
                item.thumbnail = null
                //it.thumbnailState.value?.recycle()
                //it.thumbnailState.value = null
            }
    }

    fun localMediaFileStackBack() {
        back.value = true
        val levelStack = localLevelStack
        levelStack.removeLast()
        currentDirectoryId.value = levelStack.last().first
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
        reconnection: Boolean = false
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
            localNetMediaFileService = LocalNetStorageThumbnailService(
                application,
                smbClient,
                maxSize = 60,
                initialLoadSize = 90
            )
            localNetMediaFileService.getAllData(test)
            localNetMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = 30,
                    initialLoadSize = 60,
                    prefetchDistance = 10,
                    maxSize = 90
                )
            ) {
                MediaItemPagingSource(
                    localNetMediaFileService
                )
            }.flow
            System.gc()
        }
    }

    fun localNetStackBack(): String {
        back.value = true
        return smbClient.back()
    }
}