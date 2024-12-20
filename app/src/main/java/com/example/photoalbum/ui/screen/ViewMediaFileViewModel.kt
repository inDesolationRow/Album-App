package com.example.photoalbum.ui.screen

import androidx.compose.foundation.lazy.LazyListState
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
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.LocalNetStorageMediaFileService
import com.example.photoalbum.data.LocalNetStorageThumbnailService
import com.example.photoalbum.data.LocalStorageMediaFileService
import com.example.photoalbum.data.LocalStorageThumbnailService
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

class ViewMediaFileViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    var local by mutableStateOf(true)

    var isRow by mutableStateOf(true)

    var expandMyBar: Boolean by mutableStateOf(false)

    var thumbnailFlow: MutableState<Flow<PagingData<MediaItem>>> = mutableStateOf(flowOf())

    var imageFlow: MutableState<Flow<PagingData<MediaItem>>> = mutableStateOf(flowOf())

    var itemIndex = mutableIntStateOf(0)

    val thumbnailScrollState = LazyListState()

    var source : MutableState<PagingSource<Int, MediaItem>?> = mutableStateOf(null)

    var nextDirectory: String? = null

    var previousDirectory: String? = null

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    private val smbClient by lazy {
        val client = SmbClient()
        connectSmb(client)
        return@lazy client
    }

    private val thumbnailsService by lazy {
        if (local) {
            return@lazy LocalStorageThumbnailService(
                application,
                maxSize = settings.maxSizeLarge,
                initialLoadSize = settings.initialLoadSizeLarge
            )
        } else {
            return@lazy LocalNetStorageThumbnailService(
                application,
                smbClient,
                maxSize = settings.maxSizeLarge,
                initialLoadSize = settings.initialLoadSizeLarge
            )
        }
    }

    private val imageService by lazy {
        if (local) {
            return@lazy LocalStorageMediaFileService(application, maxSize = 2, initialLoadSize = 4)
        } else {
            return@lazy LocalNetStorageMediaFileService(
                application,
                smbClient,
                maxSize = 2,
                initialLoadSize = 4
            )
        }
    }

    fun initData(directory: Any, imageId: Long, local: Boolean) {
        this.local = local
        viewModelScope.launch(Dispatchers.IO) {
            if (local) {
                val thumbnailsService = thumbnailsService as LocalStorageThumbnailService
                itemIndex.intValue =
                    thumbnailsService.getAllData(
                        param = directory as Long,
                        onlyMediaFile = true,
                        imageId
                    )
                source.value = MediaItemPagingSource(thumbnailsService)
                thumbnailFlow.value = Pager(
                    PagingConfig(
                        pageSize = settings.pageSizeLarge,
                        initialLoadSize = settings.initialLoadSizeLarge,
                        prefetchDistance = settings.prefetchDistanceLarge,
                        maxSize = settings.maxSizeLarge
                    ),
                    initialKey = 10
                ) {
                    source.value!!
                }.flow.cachedIn(viewModelScope)
                val imageService = imageService as LocalStorageMediaFileService
                imageService.sharedAllData(thumbnailsService.sharedAllData())
                imageFlow.value = Pager(
                    PagingConfig(pageSize = 1, initialLoadSize = 2)
                ) {
                    MediaItemPagingSource(imageService)
                }.flow.cachedIn(viewModelScope)

            } else {
                if (!smbClient.isConnect()) {
                    showDialog =
                        MediaListDialogEntity(MediaListDialog.LOCAL_NET_OFFLINE, true, onClick = {
                            userAction.value = UserAction.Back
                        })
                    return@launch
                }
                val thumbnailsService = thumbnailsService as LocalNetStorageThumbnailService
                itemIndex.intValue =
                    thumbnailsService.getAllData(
                        param = directory as String,
                        onlyMediaFile = true,
                        imageId
                    )
                thumbnailFlow.value = Pager(
                    PagingConfig(
                        pageSize = settings.pageSizeLarge,
                        initialLoadSize = settings.initialLoadSizeLarge,
                        prefetchDistance = settings.prefetchDistanceLarge,
                        maxSize = settings.maxSizeLarge
                    )
                ) {
                    MediaItemPagingSource(thumbnailsService)
                }.flow.cachedIn(viewModelScope)

                val imageService = imageService as LocalNetStorageMediaFileService
                imageService.sharedAllData(thumbnailsService.sharedAllData())
                //smbClient.cacheImage(imageService.allData[itemIndex.intValue].id)
                imageFlow.value = Pager(
                    PagingConfig(pageSize = 1, initialLoadSize = 2)
                ) {
                    MediaItemPagingSource(imageService)
                }.flow.cachedIn(viewModelScope)
            }
        }
    }

    suspend fun loadUntilTargetPage(pagingSource: PagingSource<Int, MediaItem>, targetPage: Int, loadSize: Int, end:()->Unit) {
        var currentPage = 1
        while (currentPage < targetPage) {
            // 手动加载分页
            val loadResult = pagingSource.load(PagingSource.LoadParams.Refresh(currentPage, loadSize, false))
            if (loadResult is PagingSource.LoadResult.Page) {
                // 如果加载成功，增加当前页
                currentPage++
            } else {
                // 如果出现错误，跳出循环
                break
            }
        }
        end()
    }

    private fun connectSmb(smbClient: SmbClient, reconnect: Boolean = false) {
        application.localNetStorageInfo?.let {
            val result =
                smbClient.connect(
                    ip = it.ip,
                    user = it.user,
                    pwd = it.password,
                    shared = it.shared,
                    reconnect = reconnect
                )
            if (result is ConnectResult.ConnectError) {
                showDialog = MediaListDialogEntity(MediaListDialog.LOCAL_NET_OFFLINE, true)
            }
        }
    }

    fun expandBar(expand: Boolean, recomposeKey: Int = 0) {
        userAction.value = UserAction.ExpandStatusBarAction(expand, recomposeKey)
    }

    fun getItemCount(): Int {
        return thumbnailsService.allData.size
    }

    fun clearCache(start: Int, end: Int) {
        println("测试: start $start end $end ")
        thumbnailsService.allData.slice(IntRange(start, end)).onEach { item ->
            item.thumbnail?.recycle()
            item.thumbnail = null
            item.thumbnailState.value?.recycle()
            item.thumbnailState.value = null
        }
    }
}