package com.example.photoalbum.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.photoalbum.data.LocalStorageThumbnailService
import com.example.photoalbum.data.MediaItemPagingSource
import com.example.photoalbum.data.model.Album
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.utils.getLastPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GroupingScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings,
) : BaseViewModel(application, userAction, settings) {
    /**
     * 顶部bar的状态
     */
    var directoryName = mutableStateOf("")

    var typeName = mutableStateOf("")

    var directoryNum = mutableIntStateOf(0)

    var photosNum = mutableIntStateOf(0)

    var videoNum = mutableIntStateOf(0)

    /**
     * 分组数据源
     */
    val groupingList: SnapshotStateList<Album> = mutableStateListOf()

    val groupingId: MutableState<Long> = mutableLongStateOf(-1L)

    val currentPageInfo: MutableStateFlow<Pair<Long, Int>> = MutableStateFlow(-1L to ItemType.GROUPING.value)

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    /**
     * 分组内容
     */
    val localMediaFileFlow: MutableState<Flow<PagingData<MediaItem>>?> = mutableStateOf(null)

    val localLevelStack: SnapshotStateList<Triple<Long, Int, Int>> = mutableStateListOf()

    var localMediaFileService = LocalStorageThumbnailService(
        application,
        maxSize = settings.maxSizeLarge,
        initialLoadSize = settings.initialLoadSizeLarge
    )

    val back = mutableStateOf(false)

    val firstDisplayCueCard = mutableStateOf(true)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val list = application.mediaDatabase.albumDao.queryByParentId(-1)
            list?.let {
                groupingList.addAll(it)
            }
            updateTopBarInfo(directoryNum = list?.size)
            userAction.collect { action ->
                if (action is UserAction.AddGrouping) {
                    groupingList.add(action.album)
                    if (currentPageInfo.value.first == -1L)
                        updateTopBarInfo(directoryNum = groupingList.size)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            currentPageInfo.collect { pageInfo ->
                if (pageInfo.first != -1L && pageInfo.second == ItemType.GROUPING.value) {
                    groupingId.value = pageInfo.first
                    initializeMediaService(pageInfo.first)
                } else if (pageInfo.second == ItemType.DIRECTORY.value) {
                    initializeMediaServiceForDirectory(pageInfo.first)
                } else if (pageInfo.first == -1L) {
                    groupingId.value = -1L
                    val list = application.mediaDatabase.albumDao.queryByParentId(-1)
                    updateTopBarInfo(directoryNum = list?.size)
                }
                if (localLevelStack.isEmpty() || pageInfo.first != localLevelStack.last().first) {
                    localLevelStack.add(Triple(pageInfo.first, pageInfo.second, 0))
                }
            }
        }

    }

    fun recompose() {
        val clearList = localMediaFileService.allData.toList()
        viewModelScope.launch(Dispatchers.IO) {
            clearCache(clearList)
            if (currentPageInfo.value.first != -1L && currentPageInfo.value.second == ItemType.GROUPING.value) {
                initializeMediaService(currentPageInfo.value.first)
            } else if (currentPageInfo.value.second == ItemType.DIRECTORY.value) {
                initializeMediaServiceForDirectory(currentPageInfo.value.first)
            } else if (currentPageInfo.value.first == -1L) {
                val list = application.mediaDatabase.albumDao.queryByParentId(-1)
                updateTopBarInfo(directoryNum = list?.size)
            }
            if (localLevelStack.isEmpty() || currentPageInfo.value.first != localLevelStack.last().first) {
                localLevelStack.add(Triple(currentPageInfo.value.first, currentPageInfo.value.second, 0))
            }
        }
    }

    private fun initializeMediaService(albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            localMediaFileService = LocalStorageThumbnailService(
                application,
                maxSize = settings.maxSizeLarge,
                initialLoadSize = settings.initialLoadSizeLarge
            )
            val result = localMediaFileService.getAllDataByAlbumId(albumId)
            val groupingName = application.mediaDatabase.albumDao.getNameById(albumId)
            updateTopBarInfo(groupingName, result.first, result.second, result.third)
            localMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = settings.pageSizeLarge,
                    initialLoadSize = settings.initialLoadSizeLarge,
                    prefetchDistance = settings.prefetchDistanceLarge,
                    maxSize = settings.maxSizeLarge
                )
            ) {
                MediaItemPagingSource(localMediaFileService)
            }.flow.cachedIn(viewModelScope)
            System.gc()
        }
    }

    private fun initializeMediaServiceForDirectory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            localMediaFileService = LocalStorageThumbnailService(
                application,
                maxSize = settings.maxSizeLarge,
                initialLoadSize = settings.initialLoadSizeLarge
            )
            val result = localMediaFileService.getAllData(id)
            val directoryName = application.mediaDatabase.directoryDao.getPathById(id)?.let { path ->
                getLastPath(path)
            }
            updateTopBarInfo(directoryName, result.first, result.second, result.third)
            localMediaFileFlow.value = Pager(
                PagingConfig(
                    pageSize = settings.pageSizeLarge,
                    initialLoadSize = settings.initialLoadSizeLarge,
                    prefetchDistance = settings.prefetchDistanceLarge,
                    maxSize = settings.maxSizeLarge
                )
            ) {
                MediaItemPagingSource(localMediaFileService)
            }.flow.cachedIn(viewModelScope)
            System.gc()
        }
    }

    fun localMediaFileStackBack() {
        back.value = true
        localLevelStack.removeLast()
        currentPageInfo.value = localLevelStack.last().first to localLevelStack.last().second
    }

    fun loadGrouping(album: Album) {
        currentPageInfo.value = album.id to ItemType.GROUPING.value
    }

    fun updateTopBarInfo(
        name: String? = null,
        directoryNum: Int? = null,
        imageNum: Int? = null,
        videoNum: Int? = null,
    ) {
        if (currentPageInfo.value.first == -1L) {
            typeName.value = "分组"
        } else {
            typeName.value = "目录"
        }
        this.directoryNum.intValue = directoryNum ?: 0
        this.photosNum.intValue = imageNum ?: 0
        this.videoNum.intValue = videoNum ?: 0
        this.directoryName.value = name ?: "收藏夹"
    }

    fun clearCache(start: Int, end: Int) {
        try {
            val clearList = localMediaFileService.allData.toList()
            clearList.slice(IntRange(start, end)).onEach { item ->
                item.thumbnail?.recycle()
                item.thumbnail = null
                item.thumbnailState.value?.recycle()
                item.thumbnailState.value = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCache(clear: List<MediaItem>? = null) {
        try {
            if (clear == null) {
                val clearList = localMediaFileService.allData.toList()
                clearList.onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            } else {
                clear.onEach { item ->
                    item.thumbnail?.recycle()
                    item.thumbnail = null
                    item.thumbnailState.value?.recycle()
                    item.thumbnailState.value = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}