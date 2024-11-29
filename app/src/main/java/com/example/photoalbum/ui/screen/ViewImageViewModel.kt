package com.example.photoalbum.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import com.example.photoalbum.data.LocalNetStorageMediaFileService
import com.example.photoalbum.data.LocalStorageMediaFileService
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ViewImageViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    var local: Boolean = true

    var thumbnailFlow: MutableState<Flow<PagingData<MediaItem>>> = mutableStateOf(flowOf())

    var nextDirectory: String? = null

    var previousDirectory: String? = null

    var expandMyBar: Boolean by mutableStateOf(false)

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    private val smbClient by lazy {
        val client = SmbClient()
        application.localNetStorageInfo?.let {
            val result =
                client.connect(ip = it.ip, user = it.user, pwd = it.password, shared = it.shared)
            if (result is ConnectResult.ConnectError) {
                showDialog = MediaListDialogEntity(MediaListDialog.LOCAL_NET_OFFLINE, true)
            }
        }
        return@lazy client
    }

    private val service by lazy {
        if (local) {
            return@lazy LocalStorageMediaFileService(application)
        } else {
            return@lazy LocalNetStorageMediaFileService(application, smbClient)
        }
    }

    fun initData(directory: Any, imageId: Long, local: Boolean) {
        this.local = local
        if (local) {
            viewModelScope.launch(Dispatchers.IO) {
                val service = service as LocalStorageMediaFileService
                service.getAllDataForMediaList(directory as Long)
                thumbnailFlow.value = Pager(
                    PagingConfig(pageSize = 10, initialLoadSize = 20)
                ) {
                    MediaItemPagingSource(service)
                }.flow.cachedIn(viewModelScope)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                if (!smbClient.isConnect()) {
                    showDialog =
                        MediaListDialogEntity(MediaListDialog.LOCAL_NET_OFFLINE, true, onClick = {
                            userAction.value = UserAction.Back
                        })
                    return@launch
                }
                val service = service as LocalNetStorageMediaFileService
                service.getAllDataForMediaList(directory as String)
                thumbnailFlow.value = Pager(
                    PagingConfig(pageSize = 10, initialLoadSize = 20)
                ) {
                    MediaItemPagingSource(service)
                }.flow.cachedIn(viewModelScope)
            }
        }
    }

    fun expandBar(expand: Boolean, recomposeKey: Int = 0) {
        userAction.value = UserAction.ExpandStatusBarAction(expand, recomposeKey)
    }
}