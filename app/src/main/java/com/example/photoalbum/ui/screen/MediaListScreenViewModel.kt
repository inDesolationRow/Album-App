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
import com.example.photoalbum.data.MediaItemPagingSourceService
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.Menu
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MediaListScreenViewModel(
    private val application: MediaApplication,
    userAction: MutableStateFlow<UserAction>
) :
    BaseViewModel(application, userAction) {
    //-1代表所有根目录
    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    var recomposeKey: MutableStateFlow<Int> = MutableStateFlow(0)

    lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    lateinit var menu: MutableState<List<Menu>>

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    lateinit var selectedItem: MutableState<Menu>

    private var newLocalNetStorageInfoId: Int? = null

    private val menuMediaListId = -1

    private val menuAddLocalNetStorageId = -2

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    lateinit var flow: MutableState<Flow<PagingData<MediaItem>>>

    private val mediaService = MediaItemPagingSourceService(application)

    val levelStack: MutableList<Long> = mutableListOf()

    var level by mutableIntStateOf(1)

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
                if (!::flow.isInitialized) {
                    flow = mutableStateOf(initPaging())
                    levelStack.add(-1L)
                } else {
                    initPaging(it)
                    if (levelStack.last() != it){
                        level += 1
                        levelStack.add(it)
                    }
                }
            }
        }

        viewModelScope.launch {
            recomposeKey.collect {
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

    private fun initPaging(directoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaService.getAllDataForMediaList(directoryId)
            flow.value = Pager(
                PagingConfig(pageSize = 30, initialLoadSize = 60)
            ) {
                MediaItemPagingSource(mediaService)
            }.flow.cachedIn(viewModelScope)
        }

    }

    private suspend fun initPaging(): Flow<PagingData<MediaItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            mediaService.getAllDataForMediaList(-1)
            val flow = Pager(
                PagingConfig(pageSize = 10, initialLoadSize = 20)
            ) {
                MediaItemPagingSource(mediaService)
            }.flow.cachedIn(viewModelScope)
            flow
        }.await()
    }

    /**
     * 强制触发更新
     *
     * @param directoryId 当前目录的id
     * @param updateKey 新的key，不得重复
     */
    private fun updateForUpdateKey(directoryId: Long, updateKey: Int) {
        updateKey.let {
            if (updateKey != 0) initPaging(directoryId)
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