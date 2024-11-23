package com.example.photoalbum.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
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
import com.example.photoalbum.ui.common.EditLocalNetStorageDialog
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import com.example.photoalbum.ui.theme.LargePadding
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    viewModel.selectedItem.value?.let {
        //本地文件列表的后退逻辑
        BackHandler(it.id == viewModel.menuLocalStorage && viewModel.levelStack.size >= 2) {
            viewModel.localMediaFileStackBack()
        }
        //本地网络文件列表的后退逻辑
        BackHandler(it.id >= viewModel.menuLocalNetMinimumId && viewModel.smbClient.pathStackSize() >= 2) {
            val path = viewModel.smbClient.back()
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
                    modifier = Modifier.padding(start = MediumPadding)
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
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
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
                        val recomposeLocalNetStorageListKey =
                            viewModel.recomposeLocalNetStorageListKey.collectAsState()
                        LaunchedEffect(selectItem.id, recomposeLocalNetStorageListKey.value) {
                            val result = viewModel.connectSmb(selectItem.id)
                            result(result = result, viewModel = viewModel) {
                                viewModel.initLocalNetMediaFilePaging()
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
                                        viewModel.recomposeLocalNetStorageListKey.value += 1
                                        viewModel.editLocalNetStorageInfo = false
                                    }
                                }
                            }
                        }
                        val items = viewModel.localNetMediaFileFlow.value.collectAsLazyPagingItems()
                        MediaList(
                            itemList = items,
                            nullPreviewIcon = viewModel.notPreviewIcon,
                            directoryIcon = viewModel.directoryIcon,
                            gridColumn = viewModel.settings.gridColumnNumState.intValue,
                            expand = {
                                viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
                            },
                            clickString = { id, name, type, data ->
                                //每次操作时判断连接是否有效
                                if (viewModel.isConnect()) {
                                    if (type == ItemType.DIRECTORY) viewModel.initLocalNetMediaFilePaging(
                                        name
                                    ) else if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                                        viewModel.userAction.value =
                                            UserAction.OpenImage(data.dropLast(name.length + 1), id)
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
fun TopBar(viewModel: MediaListScreenViewModel, selectItem: Menu, modifier: Modifier = Modifier) {
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
    modifier: Modifier = Modifier,
    clickId: ((Long, ItemType) -> Unit)? = null,
    clickString: ((Long, String, ItemType, String) -> Unit)? = null,
) {
    val lazyState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumn),
        state = lazyState,
        modifier = modifier
            .padding(start = TinyPadding)
    ) {
        items(itemList.itemCount) { index ->
            itemList[index]?.let {
                MediaFilePreview(
                    image = if (it.fileSize < ImageSize.ONE_K.size) it.thumbnail else it.thumbnailState.value,
                    nullPreviewIcon = nullPreviewIcon,
                    directoryIcon = directoryIcon,
                    directoryName = it.displayName,
                    fileType = it.type,
                    orientation = it.orientation,
                    modifier = Modifier
                        .padding(end = TinyPadding, top = TinyPadding)
                        .clickable {
                            if (clickId != null) clickId(
                                it.id,
                                it.type
                            ) else if (clickString != null) clickString(
                                it.id,
                                it.displayName,
                                it.type,
                                it.data!!
                            )
                        }
                )
            }
        }
    }
    val invisibleStatusBar by remember {
        derivedStateOf {
            lazyState.firstVisibleItemScrollOffset > 0
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
    orientation: Int,
    modifier: Modifier = Modifier
) {
    println("测试:item重组")
    Column(modifier = modifier) {
        if (image == null) {
            when (fileType) {
                ItemType.DIRECTORY -> {
                    DisplayImage(bitmap = directoryIcon, scale = true)
                }

                ItemType.IMAGE -> {
                    DisplayImage(bitmap = nullPreviewIcon, scale = true)
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
            DisplayImage(bitmap = image, orientation = orientation)
        }
        if (fileType == ItemType.DIRECTORY) {
            Text(
                text = directoryName,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun DisplayImage(
    modifier: Modifier = Modifier,
    bitmap: Bitmap,
    scale: Boolean = false,
    orientation: Int = 0
) {
    AsyncImage(
        model = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .graphicsLayer {
                rotationZ = orientation.toFloat()
                if (scale) {
                    scaleX = 1.0f
                    scaleY = 1.0f
                    translationX = -20f
                    translationY = -20f
                }
            }
    )
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

@Preview
@Composable
fun ScreenPreview() {
    PhotoAlbumTheme {
        //AddLocalNetStorage()
    }
}
