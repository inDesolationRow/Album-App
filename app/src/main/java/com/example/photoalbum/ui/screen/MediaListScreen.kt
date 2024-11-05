package com.example.photoalbum.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photoalbum.R
import com.example.photoalbum.database.model.LocalNetStorageInfo
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random


@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
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
                Box(modifier = Modifier.padding(padding)) {
                    MediaList(viewModel = viewModel)
                    Row {
                        Button(onClick = {
                            viewModel.delLocalNetStorageInfo()
                        }) {
                            Text(text = "测试删除一个网络存储")
                        }

                        Button(onClick = {
                            viewModel.addLocalNetStorageInfo(LocalNetStorageInfo(displayName = Random.nextFloat().toString()))
                        }) {
                            Text(text = "测试添加一个网络存储")
                        }
                    }
                }
            }
        },
        modifier = modifier
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
fun MediaList(viewModel: MediaListScreenViewModel) {
    val itemsList = (0..5).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .aspectRatio(1f)
            .padding(start = MediumPadding)
    ) {
        items(itemsList) {
            MediaFilePreview(
                viewModel = viewModel,
                modifier = Modifier.padding(end = MediumPadding, top = MediumPadding)
            )
        }
    }
}

@Composable
fun MediaFilePreview(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(bottom = TinyPadding)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "目录",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview
@Composable
fun ScreenPreview() {
    PhotoAlbumTheme {
        //MediaList()
    }
}
