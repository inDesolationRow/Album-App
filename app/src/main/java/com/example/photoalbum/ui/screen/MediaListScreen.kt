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
import androidx.compose.material3.Card
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.LazyPagingItems
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
import com.example.photoalbum.ui.common.EditLocalNetStorageDialog
import com.example.photoalbum.ui.theme.LargePadding
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    if (viewModel.editLocalNetStorageInfo) {
        var info: LocalNetStorageInfo? by remember { mutableStateOf(null) }
        viewModel.selectedItem.value?.let {
            LaunchedEffect(it.id) {
                info = viewModel.getLocalNetStorageInfo(id = it.id)
            }
            info?.let { info ->
                EditLocalNetStorageDialog(info) {
                    viewModel.viewModelScope.launch {
                        viewModel.updateLocalNetStorage(it)
                        viewModel.recomposeLocalNetStorageListKey.value += 1
                        viewModel.editLocalNetStorageInfo = false
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
    val mediaListNavHost = rememberNavController()
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
                NavHost(
                    navController = mediaListNavHost,
                    startDestination = "item/${viewModel.menuLocalStorage}"
                ) {
                    composable(route = "item/{id}") {
                        val id = it.arguments?.getString("id")?.toIntOrNull()
                        id?.let { id ->
                            if (id == viewModel.menuLocalStorage) {
                                val localViewModel = ViewModelProvider.create(
                                    it, BaseViewModel.Companion.Factory(
                                        application = viewModel.application,
                                        userAction = viewModel.userAction,
                                        settings = viewModel.settings
                                    )
                                )[LocalMediaFileScreenViewModel::class.java]
                                LocalMediaFileScreen(
                                    viewModel = localViewModel,
                                    recomposeKey = viewModel.recomposeLocalStorageListKey,
                                    modifier = Modifier.padding(padding)
                                )
                            } else if (id == viewModel.menuAddLocalNetStorage) {
                                val localViewModel = ViewModelProvider.create(
                                    it, BaseViewModel.Companion.Factory(
                                        application = viewModel.application,
                                        userAction = viewModel.userAction,
                                        settings = viewModel.settings
                                    )
                                )[AddLocalNetScreenViewModel::class.java]
                                AddLocalNetScreen(
                                    viewModel = localViewModel,
                                    modifier = Modifier.padding(padding)
                                )
                            } else if (id >= viewModel.menuLocalNetMinimumId) {
                                val localNetViewModel = ViewModelProvider.create(
                                    it, BaseViewModel.Companion.Factory(
                                        application = viewModel.application,
                                        userAction = viewModel.userAction,
                                        settings = viewModel.settings
                                    )
                                )[LocalNetMediaFileScreenViewModel::class.java]
                                LocalNetMediaFileScreen(
                                    viewModel = localNetViewModel,
                                    localNetId = id,
                                    recomposeKey = viewModel.recomposeLocalNetStorageListKey,
                                    modifier = Modifier.padding(padding),
                                )
                            }
                        }
                    }
                }
                viewModel.selectedItem.value?.let {
                    mediaListNavHost.navigate("item/${it.id}")
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
    clickString: ((String, ItemType) -> Unit)? = null,
) {
    val lazyState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumn),
        state = lazyState,
        modifier = modifier
            .padding(start = MediumPadding)
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
                        .padding(end = MediumPadding, top = MediumPadding)
                        .clickable {
                            if (clickId != null) clickId(
                                it.id,
                                it.type
                            ) else if (clickString != null) clickString(
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
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(bottom = TinyPadding)
            ) {
                DisplayImage(bitmap = image, orientation = orientation)
            }
        }
        Text(
            text = directoryName,
            style = MaterialTheme.typography.labelMedium
        )
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
            .graphicsLayer() {
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
