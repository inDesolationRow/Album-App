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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.LocalStorageMediaFileService
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.data.model.Settings
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
import kotlinx.coroutines.launch

class MediaListScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    /**
     * 拖拽抽屉相关的状态
     */
    val menuLocalStorage = -1

    val menuAddLocalNetStorage = -2

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    var menu: MutableState<List<Menu>> = mutableStateOf(listOf())

    var selectedItem: MutableState<Menu?> = mutableStateOf(null)

    /**
     * 本地媒体列表相关的状态
     * -1代表所有根目录
     */
    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    val levelStack: MutableList<Long> = mutableListOf()

    var level by mutableIntStateOf(1)

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    private var recomposeLocalStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    lateinit var mediaFileFlow: MutableState<Flow<PagingData<MediaItem>>>

    private val mediaService = LocalStorageMediaFileService(application)

    /**
     * 本地网络相关的状态
     */
    private lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    private var newNetStorageInfoId: Int? = null

    private val smbClient = SmbClient()

    lateinit var localNetStorageFlow: MutableState<Flow<PagingData<MediaItem>>>

    var editLocalNetStorageInfo by mutableStateOf(false)

    var recomposeLocalNetStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    var currentDirectoryName: MutableStateFlow<String> = MutableStateFlow("")

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
                if (!::mediaFileFlow.isInitialized) {
                    mediaFileFlow = mutableStateOf(initLocalMediaFilePaging())
                    levelStack.add(-1L)
                } else {
                    initLocalMediaFilePaging(it)
                    if (levelStack.last() != it) {
                        level += 1
                        levelStack.add(it)
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
                when (val action = it) {
                    is UserAction.ExpandStatusBarAction -> {
                    }

                    is UserAction.ScanAction -> {
                        if (action.end) {
                            recomposeLocalStorageListKey.value += 1
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

    private fun updateSelectItem(id: Int) {
        val selectItem = menu.value.filter { it.id == id }
        selectedItem.value = selectItem.first()
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

    private fun initLocalMediaFilePaging(directoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaService.getAllDataForMediaList(directoryId)
            mediaFileFlow.value = Pager(
                PagingConfig(pageSize = 20, initialLoadSize = 30)
            ) {
                MediaItemPagingSource(mediaService)
            }.flow.cachedIn(viewModelScope)
        }

    }

    private suspend fun initLocalMediaFilePaging(): Flow<PagingData<MediaItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            mediaService.getAllDataForMediaList(-1)
            val flow = Pager(
                PagingConfig(pageSize = 20, initialLoadSize = 30)
            ) {
                MediaItemPagingSource(mediaService)
            }.flow.cachedIn(viewModelScope)
            flow
        }.await()
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

    suspend fun connectSmb(ip: String, user: String, pwd: String?, shared: String): ConnectResult {
        return viewModelScope.async(Dispatchers.IO) {
            smbClient.connect(ip, user, pwd, shared)
        }.await()
    }

    suspend fun connectSmb(id: Int): ConnectResult {
        val localNetStorageInfo = application.mediaDatabase.localNetStorageInfoDao.getById(id)
        return localNetStorageInfo?.let {
            return@let connectSmb(
                localNetStorageInfo.ip,
                localNetStorageInfo.user,
                localNetStorageInfo.password,
                localNetStorageInfo.shared
            )
        } ?: ConnectResult.ConnectError("database_error")
    }

    fun loadLocalNetStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            smbClient.getList("新建文件夹/lg tv/图包/Seven Graphics 合集/2022")
        }
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
}