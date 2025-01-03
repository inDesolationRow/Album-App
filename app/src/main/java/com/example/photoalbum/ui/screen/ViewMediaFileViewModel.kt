package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.MediaItemDataSource
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.smb.SmbClient
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

    var itemIndex = mutableIntStateOf(0)

    var nextDirectory: String? = null

    var previousDirectory: String? = null

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    private val smbClient by lazy {
        val client = SmbClient()
        connectSmb(client)
        return@lazy client
    }

    val source: MediaItemDataSource by lazy {
        return@lazy MediaItemDataSource(application, loadSize = 20, maxSize = 80)
    }
    /*private val thumbnailsService by lazy {
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
    }*/

    /*private val imageService by lazy {
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
    }*/

    fun initData(directory: Any, imageId: Long, local: Boolean) {
        this.local = local
        viewModelScope.launch(Dispatchers.IO) {
            /*if (local) {
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

            } else
            {
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
            }*/
            if (local) {
                val select = source.getAllData(
                    param = directory as Long,
                    onlyMediaFile = true,
                    imageId
                )
                itemIndex.intValue = select
                source.items[select]
            }
        }
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
}