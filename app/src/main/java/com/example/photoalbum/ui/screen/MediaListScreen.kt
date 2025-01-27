package com.example.photoalbum.ui.screen

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.R
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.enums.StorageType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.EditLocalNetStorageDialog
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import com.example.photoalbum.ui.theme.LargePadding
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val naviDrawerGesturesEnabled = remember { mutableStateOf(true) }
    val multipleChoiceMode = remember { mutableStateOf(false) }
    val multipleChoiceList: SnapshotStateList<Long> = remember { mutableStateListOf() }
    val selectAll = remember { mutableStateOf(false) }

    viewModel.currentMenuItem.value?.let {
        BackHandler {
            if (viewModel.localLevelStack.size == 1 && it.id == viewModel.menuLocalStorage) {
                if (multipleChoiceMode.value) {
                    multipleChoiceMode.value = false
                    multipleChoiceList.clear()
                    naviDrawerGesturesEnabled.value = true
                    selectAll.value = false
                } else
                    activity?.moveTaskToBack(true)
            } else if (it.id == viewModel.menuLocalStorage && viewModel.localLevelStack.size >= 2) {
                if (multipleChoiceMode.value) {
                    multipleChoiceMode.value = false
                    multipleChoiceList.clear()
                    naviDrawerGesturesEnabled.value = true
                    selectAll.value = false
                } else {
                    viewModel.clearCache(StorageType.LOCAL)
                    viewModel.localMediaFileStackBack()
                }

            } else if (it.id >= viewModel.menuLocalNetMinimumId && viewModel.localNetLevelStack.size >= 2) {
                val path = viewModel.localNetStackBack()
                //每次操作时判断连接是否有效
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val result = if (viewModel.isConnect())
                        ConnectResult.Success
                    else
                        viewModel.connectSmb(id = it.id, reconnection = true)
                    if (result is ConnectResult.Success) {
                        viewModel.initLocalNetMediaFilePaging(path)
                    } else {
                        viewModel.smbClient.rollback()
                        viewModel.showDialog = MediaListDialogEntity(
                            mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                            isShow = true
                        )
                    }
                }
            }
        }
    }

    MediaListMainScreen(
        viewModel = viewModel,
        gesturesEnabled = naviDrawerGesturesEnabled,
        multipleChoiceMode = multipleChoiceMode,
        multipleChoiceList = multipleChoiceList,
        selectAll = selectAll,
        modifier = modifier
    )
}

@Composable
fun MediaListMainScreen(
    viewModel: MediaListScreenViewModel,
    gesturesEnabled: MutableState<Boolean>,
    multipleChoiceMode: MutableState<Boolean>,
    multipleChoiceList: SnapshotStateList<Long>,
    selectAll: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectItem = viewModel.currentMenuItem.value ?: return

    var getNavHostHeight by rememberSaveable { mutableStateOf(false) }
    var hostHeight by rememberSaveable(saver = dpSaver) { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val actionState = viewModel.userAction.collectAsState()
    if (actionState.value is UserAction.ExpandStatusBarAction) {
        viewModel.expand = (actionState.value as UserAction.ExpandStatusBarAction).expand
    }
    val topBarAnimateDp: State<Dp>? = if (getNavHostHeight) {
        animateDpAsState(
            targetValue = if (multipleChoiceMode.value || viewModel.expand) hostHeight else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "隐藏或显示bottomBar"
        )
    } else {
        null
    }

    val showPopup = remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = viewModel.drawerState,
        gesturesEnabled = gesturesEnabled.value,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    viewModel.menu.value.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.displayName) },
                            selected = item == viewModel.currentMenuItem.value,
                            onClick = {
                                scope.launch { viewModel.drawerState.close() }
                                viewModel.currentMenuItem.value = item
                                if (item.id == viewModel.menuLocalStorage) {
                                    viewModel.updateDirectoryName(true)
                                    viewModel.updateDirectoryInfo(true)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        },
        content = {
            Scaffold(topBar = {
                TopBar(
                    viewModel = viewModel,
                    multipleChoiceMode = multipleChoiceMode.value,
                    multipleChoiceList = multipleChoiceList,
                    selectAll = selectAll,
                    showPopup = showPopup,
                    modifier = Modifier
                        .then(
                            if (topBarAnimateDp != null) {
                                Modifier.height(topBarAnimateDp.value)
                            } else {
                                Modifier
                            }
                        )
                        .padding(start = MediumPadding)
                        .onGloballyPositioned { layout ->
                            with(density) {
                                if (!getNavHostHeight) {
                                    hostHeight = layout.size.height.toDp()
                                    getNavHostHeight = true
                                }
                            }
                        }
                )
            }) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    //整个composable的弹出dialog逻辑
                    //RoundedPopup(showPopup = showPopup)
                    AnimatedPopup(
                        showPopup = showPopup,
                        with(density) {
                            DpSize(
                                viewModel.settings.phoneSize?.width?.toDp() ?: LocalConfiguration.current.screenWidthDp.toDp(),
                                viewModel.settings.phoneSize?.height?.toDp() ?: LocalConfiguration.current.screenHeightDp.toDp()
                            )
                        },
                        scope
                    )
                    if (viewModel.showDialog.isShow) {
                        when (viewModel.showDialog.mediaListDialog) {
                            MediaListDialog.LOCAL_NET_IP_ERROR -> MessageDialog(
                                messageRes = R.string.connect_failed_ip_error,
                                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

                            MediaListDialog.LOCAL_NET_USER_ERROR -> MessageDialog(
                                messageRes = R.string.connect_failed_user_error,
                                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

                            MediaListDialog.LOCAL_NET_SHARED_ERROR -> MessageDialog(
                                messageRes = R.string.connect_failed_shared_error,
                                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

                            MediaListDialog.LOCAL_NET_ADD_SUCCESS -> MessageDialog(
                                messageRes = R.string.connect_success,
                                clickConfirm = {
                                    viewModel.showDialog.onClick()
                                    viewModel.showDialog = MediaListDialogEntity()
                                },
                                onDismiss = {
                                    viewModel.showDialog.onClick()
                                    viewModel.showDialog = MediaListDialogEntity()
                                })

                            MediaListDialog.LOCAL_NET_CONNECTING ->
                                ProgressDialog(R.string.connecting)

                            MediaListDialog.LOCAL_NET_OFFLINE -> MessageDialog(
                                messageRes = R.string.local_net_offline,
                                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

                            MediaListDialog.NONE -> {}
                        }
                    }
                    if (selectItem.id == viewModel.menuLocalStorage) {
                        val items = viewModel.localMediaFileFlow.value.collectAsLazyPagingItems()
                        MediaList(
                            itemList = items,
                            itemCount = viewModel.getItemCount(),
                            itemIndex = viewModel.localLevelStack.last().second,
                            maxSize = viewModel.settings.maxSizeLarge,
                            back = viewModel.back,
                            state = viewModel.localState.value,
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
                            multipleChoiceMode = multipleChoiceMode,
                            multipleChoiceList = multipleChoiceList,
                            selectAll = selectAll,
                            context = viewModel.application.applicationContext,
                            onMultipleChoice = {
                                viewModel.userAction.value = UserAction.ExpandStatusBarAction(false)
                                gesturesEnabled.value = false
                            },
                            onClear = { start, end ->
                                viewModel.clearCache(start, end, StorageType.LOCAL)
                            },
                            onLocalFileOpen = { id, type, image ->
                                if (type == ItemType.DIRECTORY) viewModel.currentDirectoryId.value =
                                    id
                                else if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                                    viewModel.application.loadThumbnailBitmap = image
                                    viewModel.userAction.value = UserAction.ExpandStatusBarAction(false)
                                    viewModel.userAction.value =
                                        UserAction.OpenImage(viewModel.currentDirectoryId.value, id)
                                }
                            },
                            expand = {
                                viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
                            },
                            onScroll = { index ->
                                val last = viewModel.localLevelStack.removeLast()
                                viewModel.localLevelStack.add(last.first to index)
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                    } else if (selectItem.id == viewModel.menuAddLocalNetStorage) {
                        AddLocalNetStorage(onClick = { ip, user, pwd, shared ->
                            viewModel.showDialog =
                                MediaListDialogEntity(MediaListDialog.LOCAL_NET_CONNECTING, true)
                            viewModel.viewModelScope.launch {
                                val result = viewModel.connectSmb(ip, user, pwd, shared)
                                result(result = result, viewModel = viewModel) {
                                    viewModel.addLocalNetStorageInfo(
                                        LocalNetStorageInfo(
                                            displayName = "$user://$shared",
                                            ip = ip,
                                            user = user,
                                            password = pwd ?: "",
                                            shared = shared
                                        )
                                    )
                                    viewModel.showDialog =
                                        MediaListDialogEntity(
                                            mediaListDialog = MediaListDialog.LOCAL_NET_ADD_SUCCESS,
                                            isShow = true,
                                        )
                                }
                            }
                        })
                    } else if (selectItem.id >= viewModel.menuLocalNetMinimumId) {
                        if (!viewModel.jumpToView) {
                            viewModel.jumpToView = false
                            LaunchedEffect(
                                selectItem.id,
                                viewModel.recomposeLocalNetStorageListKey
                            ) {
                                val result = viewModel.connectSmb(selectItem.id)
                                result(result = result, viewModel = viewModel) {
                                    viewModel.initLocalNetMediaFilePaging()
                                }
                            }
                        }
                        if (viewModel.editLocalNetStorageInfo) {
                            var info: LocalNetStorageInfo? by remember { mutableStateOf(null) }
                            LaunchedEffect(selectItem.id) {
                                info = viewModel.getLocalNetStorageInfo(id = selectItem.id)
                            }
                            info?.let {
                                EditLocalNetStorageDialog(it) {
                                    viewModel.viewModelScope.launch {
                                        viewModel.updateLocalNetStorage(it)
                                        viewModel.recomposeLocalNetStorageListKey += 1
                                        viewModel.editLocalNetStorageInfo = false
                                    }
                                }
                            }
                        }
                        val items = viewModel.localNetMediaFileFlow.value.collectAsLazyPagingItems()
                        MediaList(
                            itemList = items,
                            itemCount = viewModel.getItemCount(),
                            itemIndex = viewModel.localNetLevelStack.size.let {
                                if (it == 0)
                                    return@let 0
                                else
                                    return@let viewModel.localNetLevelStack.last().second
                            },
                            state = viewModel.localNetState.value,
                            maxSize = viewModel.settings.maxSizeLarge,
                            back = viewModel.back,
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
                            context = viewModel.application.applicationContext,
                            expand = {
                                viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
                            },
                            onClear = { start, end ->
                                viewModel.clearCache(start, end, StorageType.CLOUD)
                            },
                            onLocalNetOpen = { id, name, type, image ->
                                //每次操作时判断连接是否有效
                                //如果连接失效弹窗提示,并尝试重连
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    val result = if (viewModel.isConnect())
                                        ConnectResult.Success
                                    else
                                        viewModel.connectSmb(
                                            id = selectItem.id,
                                            reconnection = true
                                        )
                                    if (result is ConnectResult.Success) {
                                        if (type == ItemType.DIRECTORY) viewModel.initLocalNetMediaFilePaging(
                                            name
                                        ) else if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                                            viewModel.application.loadThumbnailBitmap = image
                                            viewModel.userAction.value = UserAction.ExpandStatusBarAction(false)
                                            viewModel.userAction.value =
                                                UserAction.OpenImage(
                                                    viewModel.smbClient.getPath().dropLast(1),
                                                    id,
                                                    selectItem.id
                                                )
                                            viewModel.jumpToView = true
                                        }
                                    } else {
                                        viewModel.showDialog = MediaListDialogEntity(
                                            mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                                            isShow = true
                                        )
                                    }
                                }
                            },
                            onScroll = { index ->
                                if (viewModel.localNetLevelStack.isNotEmpty()) {
                                    val last = viewModel.localNetLevelStack.removeLast()
                                    viewModel.localNetLevelStack.add(last.first to index)
                                }
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun TopBar(
    viewModel: MediaListScreenViewModel,
    multipleChoiceMode: Boolean,
    multipleChoiceList: SnapshotStateList<Long>,
    selectAll: MutableState<Boolean>,
    showPopup: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 5.dp, bottom = 5.dp)
            .fillMaxWidth()
    ) {
        if (!multipleChoiceMode) {
            Box {
                IconButton(onClick = {
                    coroutineScope.launch(Dispatchers.Main) {
                        viewModel.drawerState.open()
                    }
                }) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Dehaze),
                        contentDescription = null,
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = viewModel.directoryName.value,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.top_bar_directory_info,
                        viewModel.directoryNum.intValue,
                        viewModel.photosNum.intValue
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            /*                if (selectItem.id == viewModel.menuLocalStorage) {
                                IconButton(onClick = {}) {
                                    Icon(
                                        painter = rememberVectorPainter(Icons.Filled.Search),
                                        contentDescription = null
                                    )
                                }

                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.padding(end = SmallPadding)
                                ) {
                                    Icon(
                                        painter = rememberVectorPainter(Icons.Filled.AdsClick),
                                        contentDescription = null
                                    )
                                }
                            }
                            if (selectItem.id >= viewModel.menuLocalNetMinimumId) {
                                IconButton(onClick = {
                                    viewModel.editLocalNetStorageInfo = true
                                }) {
                                    Icon(
                                        painter = rememberVectorPainter(Icons.Filled.Edit),
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.delLocalNetStorageInfo(selectItem.id)
                                    viewModel.delLocalNetStorageInfoInMenu(selectItem.id)
                                }) {
                                    Icon(
                                        painter = rememberVectorPainter(Icons.Filled.Delete),
                                        contentDescription = null
                                    )
                                }
                            }*/
        } else {
            Box(contentAlignment = Alignment.BottomCenter) {
                IconToggleButton(
                    checked = selectAll.value,
                    onCheckedChange = {
                        selectAll.value = !selectAll.value
                        if (selectAll.value) {
                            val all = viewModel.localMediaFileService.allData
                            val filter = multipleChoiceList.toSet()
                            all.map { item ->
                                if (!filter.contains(item.id)) {
                                    multipleChoiceList.add(item.id)
                                }
                            }
                        } else {
                            multipleChoiceList.clear()
                        }
                    },
                    modifier = Modifier.zIndex(2f)
                ) {
                    println("改变图片 ${selectAll.value}")
                    if (selectAll.value) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "选中", tint = Color.DarkGray)
                    } else {
                        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = "未选中", tint = Color.LightGray)
                    }
                }
                Text(
                    stringResource(R.string.check_all),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                stringResource(R.string.check_num, multipleChoiceList.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Box(contentAlignment = Alignment.BottomCenter) {
                IconButton(onClick = {
                    showPopup.value = true
                }) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Check),
                        contentDescription = null
                    )
                }
                Text(
                    stringResource(R.string.top_bar_add_favorite),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun MediaList(
    modifier: Modifier = Modifier,
    itemList: LazyPagingItems<MediaItem>,
    itemCount: Int,
    itemIndex: Int = 0,
    state: LazyGridState,
    maxSize: Int,
    nullPreviewIcon: Bitmap,
    directoryIcon: Bitmap,
    gridColumn: Int,
    expand: (Boolean) -> Unit,
    context: Context,
    back: MutableState<Boolean>,
    multipleChoiceMode: MutableState<Boolean>? = null,
    multipleChoiceList: SnapshotStateList<Long>? = null,
    selectAll: MutableState<Boolean>? = null,
    onMultipleChoice: (() -> Unit)? = null,
    onClear: (Int, Int) -> Unit,
    onScroll: (Int) -> Unit,
    onLocalFileOpen: ((Long, ItemType, Bitmap?) -> Unit)? = null,
    onLocalNetOpen: ((Long, String, ItemType, Bitmap?) -> Unit)? = null,
) {
    //实现清除缓存
    val topClearIndex = remember { mutableIntStateOf(0) }
    val bottomClearIndex = remember { mutableIntStateOf(0) }
    val farIndex = remember { mutableIntStateOf(0) }
    val preIndex = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    //实现多选
    var tapNum by remember { mutableIntStateOf(0) }
    var checkJob: Job? = null
    val onSelect: (Long) -> Unit = { id: Long ->
        if (multipleChoiceList?.contains(id) == false) {
            multipleChoiceList.add(id)
            if (itemCount == multipleChoiceList.size)
                selectAll?.value = true
        } else {
            if (itemCount == (multipleChoiceList?.size ?: 0))
                selectAll?.value = false
            multipleChoiceList?.remove(id)
        }
    }

    val delay = remember { mutableLongStateOf(0L) }
    LaunchedEffect(state, itemCount, back) {
        snapshotFlow { state.firstVisibleItemIndex }.collect {
            if (!back.value) {
                onScroll(state.firstVisibleItemIndex)
            }

            //条件1：向下滚动 条件2：第一个显示的Item index距离上次清理的最后一个item有maxSize个item
            if (it > preIndex.intValue && it >= if (topClearIndex.intValue == 0) maxSize * 2 else maxSize * 2 + topClearIndex.intValue) {
                val end = topClearIndex.intValue + maxSize
                onClear(topClearIndex.intValue, end)
                if (topClearIndex.intValue + maxSize <= itemCount - maxSize)
                    topClearIndex.intValue += maxSize
            }
            //条件1：向上滚动 条件2：第一个显示的Item index距离上次清理的最后一个item有maxSize个item
            if (it < preIndex.intValue && it <= bottomClearIndex.intValue - maxSize * 2) {
                onClear(it + maxSize, bottomClearIndex.intValue)
                bottomClearIndex.intValue -= maxSize
            }
            if (it < preIndex.intValue && farIndex.intValue < preIndex.intValue) {
                farIndex.intValue = preIndex.intValue
                bottomClearIndex.intValue = farIndex.intValue
            }
            preIndex.intValue = min(itemCount - 1, it)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumn),
        state = state,
        modifier = modifier
            .padding(start = TinyPadding)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            if (event.changes.size == 1) {
                                tapNum = 0
                            }
                        }
                    }
                }
            }
    ) {
        items(itemList.itemCount) { index ->
            itemList[index]?.let { item ->
                if (index % 10 == 0)
                    LaunchedEffect(state) {
                        snapshotFlow { state.firstVisibleItemIndex }
                            .collect {
                                if (itemIndex != 0 && back.value) {
                                    if (state.firstVisibleItemIndex == 0) {
                                        scope.launch {
                                            while (delay.longValue < 100) {
                                                state.scrollToItem(itemIndex)
                                                delay(10)
                                                delay.longValue += 10
                                            }
                                            delay.longValue = 0
                                        }
                                    } else {
                                        back.value = false
                                    }
                                } else {
                                    back.value = false
                                }
                            }
                    }
                val image = item.thumbnailState.value?.let { bitmap ->
                    if (bitmap.isRecycled) item.thumbnail
                    else bitmap
                } ?: item.thumbnail
                val checked = multipleChoiceList?.contains(item.id) == true

                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            var pressTimer: Long = 0
                            var canOpen = true
                            var canCheck = true
                            val canMultipleChoice = multipleChoiceMode != null
                            var check = false
                            val openImage = {
                                if (onLocalFileOpen != null) onLocalFileOpen(
                                    item.id,
                                    item.type,
                                    image
                                ) else if (onLocalNetOpen != null) onLocalNetOpen(
                                    item.id,
                                    item.displayName,
                                    item.type,
                                    image
                                )
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        tapNum++
                                        pressTimer = System.currentTimeMillis()
                                        if (tapNum == 1) {
                                            if (canMultipleChoice) {
                                                checkJob = scope.launch {
                                                    delay(1000)
                                                    multipleChoiceMode?.value = true
                                                    onMultipleChoice?.invoke()
                                                }
                                            }
                                        } else {
                                            canOpen = false
                                            checkJob?.cancel()
                                        }
                                    }

                                    PointerEventType.Move -> {
                                        if (event.changes.size > 1) {
                                            canOpen = false
                                            checkJob?.cancel()
                                        } else {
                                            val pointerEvent = event.changes.last()
                                            val move = (pointerEvent.position - pointerEvent.previousPosition).getDistance()
                                            if (move > 10f) {
                                                canOpen = false
                                                canCheck = false
                                            }
                                            if (move > 20f)
                                                checkJob?.cancel()
                                        }
                                    }

                                    PointerEventType.Release -> {
                                        multipleChoiceMode?.value?.let { multiChoice ->
                                            if (multiChoice) {
                                                check = true
                                            }
                                        }
                                        if (System.currentTimeMillis() - pressTimer < 1000) {
                                            checkJob?.cancel()
                                        }

                                        if (canOpen && !check && tapNum == 1) {
                                            openImage()
                                        }
                                        if (check && canCheck) {
                                            onSelect(item.id)
                                        }
                                        if (event.changes.size == 1) {
                                            canOpen = true
                                            canCheck = true
                                            pressTimer = 0
                                            check = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    MediaFilePreview(
                        image = image,
                        nullPreviewIcon = nullPreviewIcon,
                        directoryIcon = directoryIcon,
                        directoryName = item.displayName,
                        fileType = item.type,
                        context = context,
                        modifier = Modifier
                            .padding(end = TinyPadding, top = TinyPadding)
                    )
                    if (multipleChoiceMode != null && multipleChoiceMode.value && (item.type == ItemType.DIRECTORY || image != null)) {
                        IconToggleButton(
                            checked = checked,
                            colors = IconButtonDefaults.iconToggleButtonColors(
                                // 选中时图标的颜色
                                containerColor = Color(0x80808080),
                                // 未选中时图标的颜色
                                contentColor = Color.White,
                                // 选中时背景颜色
                                checkedContainerColor = Color.White,
                                // 未选中时背景颜色
                                checkedContentColor = Color.DarkGray
                            ),
                            onCheckedChange = {},
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp)
                                .offset(x = (-10).dp, y = 10.dp)
                        ) {
                            if (checked) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "选中",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "未选中",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val invisibleStatusBar by remember {
        derivedStateOf {
            state.firstVisibleItemScrollOffset > 0
        }
    }
    var lifecycle by remember {
        mutableStateOf(true)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    lifecycle = true
                }

                Lifecycle.Event.ON_PAUSE -> {
                    lifecycle = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (lifecycle && multipleChoiceMode?.value == false)
        expand(!invisibleStatusBar)
}

@Composable
fun MediaFilePreview(
    image: Bitmap?,
    nullPreviewIcon: Bitmap,
    directoryIcon: Bitmap,
    fileType: ItemType,
    directoryName: String,
    context: Context,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (image == null || image.width == 0 && image.height == 0) {
            when (fileType) {
                ItemType.IMAGE -> {
                    DisplayImage(
                        bitmap = nullPreviewIcon,
                        context = context,
                        modifier = Modifier.aspectRatio(1f)
                    )
                }

                ItemType.DIRECTORY -> {
                    DisplayImage(
                        bitmap = directoryIcon,
                        context = context,
                        modifier = Modifier.aspectRatio(1f)
                    )
                }

                ItemType.VIDEO -> {}
                ItemType.ERROR -> {}
            }
        } else {
            DisplayImage(
                bitmap = image,
                context = context,
                modifier = Modifier.aspectRatio(1f)
            )
        }
        if (fileType == ItemType.DIRECTORY) {
            Text(
                text = directoryName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = SmallPadding)
            )
        }
    }
}

@Composable
fun AddLocalNetStorage(
    onClick: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        var ip by remember { mutableStateOf("") }
        val ipSource = remember { MutableInteractionSource() }
        val ipFocused = ipSource.collectIsFocusedAsState()

        var user by remember { mutableStateOf("") }
        val userSource = remember { MutableInteractionSource() }
        val userFocused = userSource.collectIsFocusedAsState()

        var pwd by remember { mutableStateOf("") }
        val pwdSource = remember { MutableInteractionSource() }
        val pwdFocused = pwdSource.collectIsFocusedAsState()

        var shared by remember { mutableStateOf("") }
        val sharedSource = remember { MutableInteractionSource() }
        val sharedFocused = sharedSource.collectIsFocusedAsState()

        OutlinedTextField(
            value = ip,
            onValueChange = { newValue -> ip = newValue },
            label = {
                if (ipFocused.value) Text(text = stringResource(R.string.ip)) else Text(
                    text = stringResource(
                        R.string.ip_input
                    )
                )
            },
            interactionSource = ipSource,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = user,
            onValueChange = { newValue -> user = newValue },
            label = {
                if (userFocused.value) Text(text = stringResource(R.string.user)) else Text(
                    text = stringResource(R.string.user_input)
                )
            },
            interactionSource = userSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = pwd,
            onValueChange = { newValue -> pwd = newValue },
            label = {
                if (pwdFocused.value) Text(text = stringResource(R.string.pwd)) else Text(
                    text = stringResource(
                        R.string.pwd_input
                    )
                )
            },
            interactionSource = pwdSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = shared,
            onValueChange = { newValue -> shared = newValue },
            label = {
                if (sharedFocused.value) Text(text = stringResource(R.string.shared)) else Text(
                    text = stringResource(R.string.shared_input)
                )
            },
            interactionSource = sharedSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LargePadding)
        ) {
            TextButton(onClick = { onClick(ip.trim(), user.trim(), pwd.trim(), shared.trim()) }) {
                Text(
                    text = stringResource(R.string.submit),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

fun result(
    result: ConnectResult,
    viewModel: MediaListScreenViewModel,
    onSuccess: () -> Unit,
) {
    when (result) {
        is ConnectResult.IPError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_IP_ERROR,
                isShow = true
            )

        is ConnectResult.AuthenticateError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_USER_ERROR,
                isShow = true
            )


        is ConnectResult.SharedError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_SHARED_ERROR,
                isShow = true
            )


        is ConnectResult.ConnectError -> {}
        is ConnectResult.Success -> onSuccess()
    }
}

@Composable
fun RoundedPopup(showPopup: MutableState<Boolean>) {
    if (showPopup.value) {
        Popup(
            alignment = Alignment.Center, // 控制弹出位置
            onDismissRequest = { showPopup.value = false } // 点击外部关闭
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp, 200.dp) // 设置宽高
                    .background(Color.White, shape = RoundedCornerShape(16.dp)) // 圆角背景
                    .padding(16.dp)
            ) {
                Text("这是一个圆角弹出窗口", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun AnimatedPopup(showPopup: MutableState<Boolean>, size: DpSize, scope: CoroutineScope) {
    val width = remember { (size.width.value * 0.9f).dp }
    val height = remember { (size.height.value * 0.95f).dp }

    val transition = updateTransition(targetState = showPopup.value, label = "popup")
    val offset by transition.animateDp(
        label = "popupOffset",
        transitionSpec = { tween(durationMillis = 300) }
    ) { state -> if (state) 0.dp else height }

    val isPopupVisible = remember { mutableStateOf(false) } // 控制是否真正显示 Popup

    // 监听 showPopup 的变化，管理 Popup 显示状态
    LaunchedEffect(showPopup.value) {
        if (showPopup.value) {
            isPopupVisible.value = true // 打开 Popup
        } else {
            // 延迟到动画完成后再隐藏 Popup
            delay(300) // 动画时长和 transitionSpec 一致
            isPopupVisible.value = false
        }
    }

    if (isPopupVisible.value) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = {
                showPopup.value = false
            }
        ) {
            Box(
                modifier = Modifier
                    .size(width, height)
                    .offset(y = offset)
                    .background(Color(0xE6FFFFFF), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("这是一个带动画的圆角弹出窗口", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}