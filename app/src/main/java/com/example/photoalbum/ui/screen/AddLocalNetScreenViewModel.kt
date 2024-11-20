package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddLocalNetScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    fun addLocalNetStorageInfo(localNetStorageInfo: LocalNetStorageInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            application.mediaDatabase.localNetStorageInfoDao.insert(localNetStorageInfo)
            userAction.value = UserAction.AddLocalNet(localNetStorageInfo)
        }
    }

}

