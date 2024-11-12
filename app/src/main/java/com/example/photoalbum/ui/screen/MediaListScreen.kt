package com.example.photoalbum.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    BackHandler(viewModel.level >= 2) {
        viewModel.level -= 1
        val levelStack = viewModel.levelStack
        levelStack.removeLast()
        viewModel.currentDirectoryId.value = levelStack.last()
    }
    MediaListMainScreen(viewModel, modifier = modifier)
}

@Composable
fun MediaListMainScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
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
                TopBar(viewModel = viewModel, modifier = Modifier.padding(start = MediumPadding))
            }) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    val items = viewModel.flow.value.collectAsLazyPagingItems()

                    MediaList(
                        itemList = items,
                        nullPreviewIcon = viewModel.notPreviewIcon,
                        directoryIcon = viewModel.directoryIcon,
                        click = { id, type ->
                            if (type == ItemType.DIRECTORY) viewModel.currentDirectoryId.value = id
                        },
                        expand = {
                            viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun TopBar(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
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
    }
}

@Composable
fun MediaList(
    itemList: LazyPagingItems<MediaItem>,
    nullPreviewIcon: Bitmap,
    directoryIcon: Bitmap,
    expand: (Boolean) -> Unit,
    click: (Long, ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = lazyState,
        modifier = modifier
            .padding(start = MediumPadding)
    ) {
        items(itemList.itemCount) { index ->
            itemList[index]?.let {
                MediaFilePreview(
                    image = it.thumbnail,
                    nullPreviewIcon = nullPreviewIcon,
                    directoryIcon = directoryIcon,
                    directoryName = it.displayName,
                    fileType = it.type,
                    modifier = Modifier
                        .padding(end = MediumPadding, top = MediumPadding)
                        .clickable { click(it.id, it.type) }
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
    modifier: Modifier = Modifier
) {
    println("测试:item重组")
    Column(modifier = modifier) {
        if (image == null) {
            when (fileType) {
                ItemType.DIRECTORY -> { DisplayImage(bitmap = directoryIcon, scale = true) }
                ItemType.IMAGE -> { DisplayImage(bitmap = nullPreviewIcon, scale = true) }
                ItemType.VIDEO -> {}
                ItemType.ERROR -> {}
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(bottom = TinyPadding)
            ) {
                DisplayImage(bitmap = image)
            }
        }
        Text(
            text = directoryName,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun DisplayImage(modifier: Modifier = Modifier, bitmap: Bitmap, scale: Boolean = false) {
    AsyncImage(
        model = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .graphicsLayer {
                if (scale) {
                    scaleX = 1.0f
                    scaleY = 1.0f
                    translationX = -20f
                    translationY = -20f
                }
            }
    )
}

@Preview
@Composable
fun ScreenPreview() {
    PhotoAlbumTheme {
    }
}
