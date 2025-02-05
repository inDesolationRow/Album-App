package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.DataService
import com.example.photoalbum.data.LocalDataSource
import com.example.photoalbum.data.LocalNetDataSource
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
    settings: Settings,
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(MediaListDialogEntity())

    var isRow by mutableStateOf(true)

    var expandMyBar: Boolean by mutableStateOf(false)

    var itemIndex = mutableIntStateOf(-1)

    var loadPageParams = mutableStateOf(-1 to -1)

    var initializer by mutableStateOf(false)

/*    var nextDirectory: String? = null

    var previousDirectory: String? = null*/

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    private val smbClient by lazy {
        val client = SmbClient()
        connectSmb(client)
        return@lazy client
    }
    private var _source: DataService<*>? = null

    val source: DataService<*>
        get() {
            return _source!!
        }

    fun initData(directory: Any, imageId: Long, local: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _source = if (local)
                LocalDataSource(application, loadSize = 20, maxSize = 80)
            else {
                if (!smbClient.isConnect()) {
                    showDialog =
                        MediaListDialogEntity(MediaListDialog.LOCAL_NET_OFFLINE, true, onClick = {
                            userAction.value = UserAction.Back
                        })
                    return@launch
                }
                LocalNetDataSource(
                    application,
                    loadSize = 20,
                    maxSize = 80,
                    smbClient = smbClient
                )
            }
            initializer = true
            val select =
                source.let {
                    if (local) return@let (it as LocalDataSource).getAllData(
                        param = directory as Long,
                        onlyMediaFile = true,
                        imageId
                    ) else return@let (it as LocalNetDataSource).getAllData(
                        param = directory as String,
                        onlyMediaFile = true,
                        imageId
                    )
                }
            if (select != -1) {
                loadPageParams.value = select to source.items.size()
                itemIndex.intValue = select
                source.items[select]
            }
        }

    }

    fun initDataByAlbumId(albumId: Long, imageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _source = LocalDataSource(application, loadSize = 20, maxSize = 80)
            initializer = true
            val select =
                source.let {
                    return@let (it as LocalDataSource).getAllDataByAlbumId(albumId, imageId)
                }
            if (select != -1) {
                loadPageParams.value = select to source.items.size()
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
        userAction.value = UserAction.ExpandStatusBarAction(expand, recomposeKey, 0)
    }

    override fun onCleared() {
        super.onCleared()
        source.clearCache()
        if (source is LocalNetDataSource) {
            smbClient.close()
        }
    }
}