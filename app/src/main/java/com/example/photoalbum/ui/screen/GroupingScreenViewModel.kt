package com.example.photoalbum.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.model.Album
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.Dispatchers
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

    /**
     * 数据源
     */
    val groupingList: SnapshotStateList<Album> = mutableStateListOf()

    val currentDirectoryInfo: MutableState<Pair<Int, Int>?> = mutableStateOf(-1 to ItemType.GROUPING.value)

    val notPreviewIcon = application.getDrawable(R.drawable.hide)!!.toBitmap()

    val directoryIcon = application.getDrawable(R.drawable.baseline_folder)!!.toBitmap()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val list = application.mediaDatabase.albumDao.queryByParentId(-1)
            list?.let {
                groupingList.addAll(it)
            }
            updateTopBarInfo(directoryNum = list?.size)
        }
    }

    private fun updateTopBarInfo(name: String? = null, directoryNum: Int? = null, imageNum: Int? = null) {
        if (currentDirectoryInfo.value?.second == ItemType.GROUPING.value) {
            typeName.value = "分组"
        } else {
            typeName.value = "目录"
        }
        this.directoryNum.intValue = directoryNum ?: 0
        this.photosNum.intValue = imageNum ?: 0
        this.directoryName.value = name ?: "收藏夹"

    }
}