package com.example.photoalbum.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.LocalNetStorageMediaFileService
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MediaListScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {
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

    private var newNetStorageInfoId: Int? = null

    var editLocalNetStorageInfo by mutableStateOf(false)

    //让本地网络列表强制刷新
    var recomposeLocalNetStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    //让本地文件列表强制刷新
    var recomposeLocalStorageListKey: MutableStateFlow<Int> = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            userAction.collect{
                when(val action = userAction.value){
                    is UserAction.AddLocalNet -> {
                        updateMenuList(action.localNetStorageInfo)
                    }
                    is UserAction.ExpandStatusBarAction -> {}
                    is UserAction.ScanAction -> {}
                    is UserAction.NoneAction -> {}
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

    private fun updateMenuList(localNetStorageInfo: LocalNetStorageInfo){
       val update = localNetStorageInfoListStateFlow.value.toMutableList()
        update.add(localNetStorageInfo)
        localNetStorageInfoListStateFlow.value = update
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

    fun delLocalNetStorageInfoInMenu(id: Int) {
        val list = localNetStorageInfoListStateFlow.value.toMutableList()
        val del = list.filter { it.id == id }
        list.remove(del.first())
        localNetStorageInfoListStateFlow.value = list
        updateSelectItem(menuAddLocalNetStorage)
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