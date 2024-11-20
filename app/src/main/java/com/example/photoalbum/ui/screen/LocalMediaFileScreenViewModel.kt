package com.example.photoalbum.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.LocalStorageMediaFileService
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LocalMediaFileScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var currentDirectoryId: MutableStateFlow<Long> = MutableStateFlow(-1)

    val levelStack: SnapshotStateList<Long> = mutableStateListOf()

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    var localMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>>? = null

    private val localMediaFileService = LocalStorageMediaFileService(application)

    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            currentDirectoryId.collect {
                if (localMediaFileFlow == null) {
                    localMediaFileFlow = mutableStateOf(initLocalMediaFilePaging())
                    levelStack.add(-1L)
                } else {
                    initLocalMediaFilePaging(it)
                    if (levelStack.last() != it) {
                        levelStack.add(it)
                    }
                }
            }
        }

        /*        viewModelScope.launch {
                    recomposeLocalStorageListKey.collect {

                    }
                }*/

        /*viewModelScope.launch {
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
        }*/
    }

    private fun initLocalMediaFilePaging(directoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            localMediaFileService.getAllDataForMediaList(directoryId)
            localMediaFileFlow?.value = Pager(
                PagingConfig(pageSize = 20, initialLoadSize = 30)
            ) {
                MediaItemPagingSource(localMediaFileService)
            }.flow.cachedIn(viewModelScope)
        }
    }

    private suspend fun initLocalMediaFilePaging(): Flow<PagingData<MediaItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            localMediaFileService.getAllDataForMediaList(-1)
            val flow = Pager(
                PagingConfig(pageSize = 20, initialLoadSize = 30)
            ) {
                MediaItemPagingSource(localMediaFileService)
            }.flow.cachedIn(viewModelScope)
            flow
        }.await()
    }

    fun localMediaFileStackBack() {
        val levelStack = levelStack
        levelStack.removeLast()
        currentDirectoryId.value = levelStack.last()
    }

    /**
     * 强制触发更新
     *
     * @param directoryId 当前本地目录的id
     * @param updateKey 新的key，不得重复
     */
    fun updateForUpdateKey(directoryId: Long, updateKey: Int) {
        updateKey.let {
            if (updateKey != 0) initLocalMediaFilePaging(directoryId)
        }
    }

}