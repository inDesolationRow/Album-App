package com.example.photoalbum.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.database.model.DirectoryWithMediaFile
import com.example.photoalbum.database.model.LocalNetStorageInfo
import com.example.photoalbum.model.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MediaListScreenViewModel(private val application: MediaApplication) :
    BaseViewModel(application) {

    lateinit var originalDirectoryList: List<DirectoryWithMediaFile>

    lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    lateinit var menu: MutableState<List<Menu>>

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    lateinit var selectedItem: MutableState<Menu>

    private var newLocalNetStorageInfoId: Int? = null

    private final val MENU_MEDIA_LIST_ID = -1

    private final val MENU_ADD_LOCAL_NET_STORAGE_ID = -2

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            originalDirectoryList =
                application.mediaDatabase.directoryDao.queryDirectoryWithMediaFileByParentId(-1)
                    ?: listOf()
//            val directoryList = application.mediaDatabase.directoryDao.queryDirectoryByParentId(-1) ?: listOf()
//            var bitmap = null
//            for (dir in directoryList){
//                val previewSrc = application.mediaDatabase.directoryDao.queryPreviewImageByParentId(dir.directoryId)
//            }

            for (dir in originalDirectoryList) {
                if (dir.mediaFileList.isNotEmpty() && dir.mediaFileList[0].data.isNotBlank() && dir.mediaFileList[0].data.isNotEmpty()) {
                    val file = File(dir.mediaFileList[0].data)
                    viewModelScope.launch(context = Dispatchers.IO) {
                        if (file.exists()) {
                            try {
                                val loadImageStartTime = System.currentTimeMillis()
                                //val image = Picasso.get().load(file.absolutePath).get()
                                val image = BitmapFactory.decodeFile(file.absolutePath)
                                val loadImageEndTime = System.currentTimeMillis()
                                val duration = loadImageStartTime - loadImageEndTime
                                println("加载耗时 $duration ms 路径:${file.absolutePath}")
                            } catch (e: Exception) {
                                println("为什么不崩溃${e.message}")
                            }
                        }
                    }
                } else {
                    println(dir)
                }
            }
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("查询耗时 $duration ms")
        }

        //初始化拖拽抽屉所需的数据
        viewModelScope.launch(context = Dispatchers.IO) {
            val localNetStorageInfoList =
                application.mediaDatabase.localNetStorageInfoDao.getList()?.toMutableList()
            localNetStorageInfoListStateFlow =
                MutableStateFlow(localNetStorageInfoList ?: mutableListOf<LocalNetStorageInfo>())
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

    fun delLocalNetStorageInfo() {
        val list = localNetStorageInfoListStateFlow.value.toMutableList()
        val del = list.filter { it.id == selectedItem.value.id }
        list.remove(del.first())
        localNetStorageInfoListStateFlow.value = list
        viewModelScope.launch(Dispatchers.IO) {
            application.mediaDatabase.localNetStorageInfoDao.deleteById(del.first().id)
        }
        updateSelectItem(MENU_ADD_LOCAL_NET_STORAGE_ID)
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
                MENU_MEDIA_LIST_ID,
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
                MENU_ADD_LOCAL_NET_STORAGE_ID,
                application.getString(R.string.add_local_cloud),
                true,
                Icons.Filled.CloudSync
            )
        )
        return menuList
    }

}