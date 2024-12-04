package com.example.photoalbum.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.R
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.model.Menu
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    viewModel.selectedItem.value?.let {
        //本地文件列表的后退逻辑
        BackHandler(it.id == viewModel.menuLocalStorage && viewModel.localLevelStack.size >= 2) {
            viewModel.localMediaFileStackBack()
        }
        //本地网络文件列表的后退逻辑
        BackHandler(it.id >= viewModel.menuLocalNetMinimumId && viewModel.localNetLevelStack.size >= 2) {
            val path = viewModel.localNetStackBack()
            //每次操作时判断连接是否有效
            if (viewModel.smbClient.isConnect()) {
                viewModel.initLocalNetMediaFilePaging(path)
            } else {
                //如果连接失效弹窗提示,并尝试重连
                viewModel.smbClient.rollback()
                viewModel.showDialog = MediaListDialogEntity(
                    mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                    isShow = true
                )
                viewModel.selectedItem.value?.let {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        viewModel.connectSmb(
                            id = it.id,
                            reconnection = true
                        )
                    }
                }
            }
        }
    }

    MediaListMainScreen(viewModel, modifier = modifier)
}

@Composable
fun MediaListMainScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val selectItem = viewModel.selectedItem.value ?: return

    var getNavHostHeight by rememberSaveable { mutableStateOf(false) }
    var hostHeight by rememberSaveable(saver = dpSaver) { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val actionState = viewModel.userAction.collectAsState()
    if (actionState.value is UserAction.ExpandStatusBarAction) {
        viewModel.expand = (actionState.value as UserAction.ExpandStatusBarAction).expand
    }
    val topBarAnimateDp: State<Dp>? = if (getNavHostHeight) {
        animateDpAsState(
            targetValue = if (viewModel.expand) hostHeight else 0.dp,
            animationSpec = tween(durationMillis = 600),
            label = "隐藏或显示bottomBar"
        )
    } else {
        null
    }

    ModalNavigationDrawer(
        drawerState = viewModel.drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    viewModel.menu.value.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.displayName) },
                            selected = item == viewModel.selectedItem.value,
                            onClick = {
                                scope.launch { viewModel.drawerState.close() }
                                viewModel.selectedItem.value = item
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
                    selectItem = selectItem,
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
                            itemIndex = viewModel.localLevelStack.last().second,
                            back = viewModel.back,
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
                            context = viewModel.application.applicationContext,
                            clickId = { id, type ->
                                if (type == ItemType.DIRECTORY) viewModel.currentDirectoryId.value =
                                    id
                                else if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
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
                            itemIndex = viewModel.localNetLevelStack.size.let {
                                if (it == 0)
                                    return@let 0
                                else
                                    return@let viewModel.localNetLevelStack.last().second
                            },
                            back = viewModel.back,
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
                            context = viewModel.application.applicationContext,
                            expand = {
                                viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
                            },
                            clickString = { id, name, type ->
                                //每次操作时判断连接是否有效
                                if (viewModel.isConnect()) {
                                    if (type == ItemType.DIRECTORY) viewModel.initLocalNetMediaFilePaging(
                                        name
                                    ) else if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                                        viewModel.userAction.value =
                                            UserAction.OpenImage(
                                                viewModel.smbClient.getPath().dropLast(1),
                                                id,
                                                selectItem.id
                                            )
                                        viewModel.jumpToView = true
                                    }
                                } else {
                                    //如果连接失效弹窗提示,并尝试重连
                                    viewModel.showDialog = MediaListDialogEntity(
                                        mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                                        isShow = true
                                    )
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        viewModel.connectSmb(
                                            id = selectItem.id,
                                            reconnection = true
                                        )
                                    }
                                }
                            },
                            onScroll = { index ->
                                val last = viewModel.localNetLevelStack.removeLast()
                                viewModel.localNetLevelStack.add(last.first to index)
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
    selectItem: Menu,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
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
            if (selectItem.id == viewModel.menuLocalStorage) {
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
            }
        }
    }
}

@Composable
fun MediaList(
    itemList: LazyPagingItems<MediaItem>,
    nullPreviewIcon: Bitmap,
    directoryIcon: Bitmap,
    gridColumn: Int,
    expand: (Boolean) -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    itemIndex: Int = 0,
    back: MutableState<Boolean>,
    onScroll: (Int) -> Unit,
    clickId: ((Long, ItemType) -> Unit)? = null,
    clickString: ((Long, String, ItemType) -> Unit)? = null,
) {
    val state = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumn),
        state = state,
        modifier = modifier
            .padding(start = TinyPadding)
    ) {
        items(itemList.itemCount) { index ->
            LaunchedEffect(state) {
                snapshotFlow { state.firstVisibleItemIndex }
                    .collect {
                        if (!back.value)
                            onScroll(state.firstVisibleItemIndex)
                        else {
                            if (itemIndex != 0) {
                                if (state.firstVisibleItemIndex != itemIndex) {
                                    state.scrollToItem(itemIndex)
                                } else {
                                    back.value = false
                                }
                            }
                        }
                    }
            }
            itemList[index]?.let {
                MediaFilePreview(
                    image = if (it.fileSize < ImageSize.M_1.size) it.thumbnail else it.thumbnailState.value,
                    nullPreviewIcon = nullPreviewIcon,
                    directoryIcon = directoryIcon,
                    directoryName = it.displayName,
                    fileType = it.type,
                    context = context,
                    modifier = Modifier
                        .padding(end = TinyPadding, top = TinyPadding)
                        .clickable {
                            if (clickId != null) clickId(
                                it.id,
                                it.type
                            ) else if (clickString != null) clickString(
                                it.id,
                                it.displayName,
                                it.type
                            )
                        }
                )
            }
        }
    }

    val invisibleStatusBar by remember {
        derivedStateOf {
            state.firstVisibleItemScrollOffset > 0
        }
    }
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
    modifier: Modifier = Modifier
) {
    println("测试:item重组")
    Column(modifier = modifier) {
        if (image == null) {
            when (fileType) {
                ItemType.DIRECTORY -> {
                    DisplayImage(
                        bitmap = directoryIcon,
                        context = context,
                        modifier = Modifier.aspectRatio(1f)
                    )
                }

                ItemType.IMAGE -> {
                    DisplayImage(
                        bitmap = nullPreviewIcon,
                        context = context,
                        modifier = Modifier.aspectRatio(1f)
                    )
                }

                ItemType.VIDEO -> {}
                ItemType.ERROR -> {}
            }
        } else {
            /*            Card(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.padding(bottom = TinyPadding)
                        ) {
                            DisplayImage(bitmap = image, orientation = orientation, modifier = Modifier.padding(bottom = TinyPadding))
                        }*/
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
    modifier: Modifier = Modifier
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
    onSuccess: () -> Unit
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