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
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class LocalNetMediaFileScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    val smbClient = SmbClient()

    var localNetMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>> = mutableStateOf(flowOf())

    var currentDirectoryName: MutableStateFlow<String> = MutableStateFlow("")

    private val localNetMediaFileService = LocalNetStorageMediaFileService(application, smbClient)

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    fun isConnect(): Boolean {
        return smbClient.isConnect()
    }

    fun initLocalNetMediaFilePaging(path: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val test = path ?: ""
            localNetMediaFileService.getAllDataForMediaList(test)
            localNetMediaFileFlow.value = Pager(
                PagingConfig(pageSize = 20, initialLoadSize = 30)
            ) {
                MediaItemPagingSource(
                    localNetMediaFileService
                )
            }.flow.cachedIn(viewModelScope)
        }
    }

}