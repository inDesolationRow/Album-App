package com.example.photoalbum.ui.screen

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
import com.example.photoalbum.database.model.LocalNetStorageInfo
import com.example.photoalbum.model.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MediaListScreenViewModel(private val application: MediaApplication) :
    BaseViewModel(application) {

    lateinit var localNetStorageInfoListStateFlow: MutableStateFlow<MutableList<LocalNetStorageInfo>>

    lateinit var menu: MutableState<List<Menu>>

    var drawerState by mutableStateOf(DrawerState(DrawerValue.Closed))

    lateinit var selectedItem: MutableState<Menu>

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            getLocalNetStorageInfoList()
            menu = mutableStateOf(getMenuList())
            selectedItem = mutableStateOf(menu.value[0])
            localNetStorageInfoListStateFlow.collect{
                if (it.size != menu.value.size){
                    menu.value = getMenuList()
                }
            }
        }
    }

    private fun getMenuList(): List<Menu>{
        val menuList: MutableList<Menu> = mutableListOf()
        menuList.add(
            Menu(
                application.getString(R.string.local_file),
                true,
                Icons.Filled.Folder
            )
        )
        val list = localNetStorageInfoListStateFlow.value
        for (info in list) {
            menuList.add(Menu(info.displayName, true, Icons.Filled.Cloud))
        }
        menuList.add(
            Menu(
                application.getString(R.string.add_local_cloud),
                true,
                Icons.Filled.CloudSync
            )
        )
        return menuList
    }

    private suspend fun getLocalNetStorageInfoList() {
        val localNetStorageInfoList = application.mediaDatabase.localNetStorageInfoDao.getList().toMutableList()
        localNetStorageInfoListStateFlow = MutableStateFlow(localNetStorageInfoList)
    }
}