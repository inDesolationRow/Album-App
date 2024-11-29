package com.example.photoalbum.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.R
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ViewMediaFile(viewModel: ViewImageViewModel) {
    viewModel.expandBar(false)
    val statusBarHeight = WindowInsets.systemBars.getTop(LocalDensity.current)
    val statusBarHeightDp = with(LocalDensity.current) { statusBarHeight.toDp() }
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    var topPaddingValues = PaddingValues()
    val flow = viewModel.thumbnailFlow.value.collectAsLazyPagingItems()
    if (viewModel.showDialog.isShow) {
        if (viewModel.showDialog.mediaListDialog == MediaListDialog.LOCAL_NET_OFFLINE) {
            MessageDialog(
                messageRes = R.string.local_net_offline,
                clickConfirm = viewModel.showDialog.onClick
            ) { viewModel.showDialog.onClick }
        }
    }
    Scaffold(
        topBar = {
            if (viewModel.expandMyBar) TopBar(
                viewModel = viewModel,
                modifier = Modifier
                    .zIndex(1f)
                    .background(Color.White.copy(alpha = 0.8f))
                    .height(screenHeightDp * 0.12f)
                    .padding(topPaddingValues)
            )
        },
        bottomBar = {
            val listState = rememberLazyListState()
            if (viewModel.expandMyBar) BottomBar(
                state = listState,
                height = screenHeightDp * 0.12f,
                notPreview = viewModel.notPreviewIcon,
                items = flow,
                modifier = Modifier
                    .zIndex(1f)
                    .background(Color.White.copy(alpha = 0.8f))
                    .height(screenHeightDp * 0.12f)
            )
        }
    ) {
        it.let {
            topPaddingValues = PaddingValues(top = statusBarHeightDp)
        }
        View(modifier = Modifier
            .zIndex(0f)
            .fillMaxSize()
            .padding(topPaddingValues)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    viewModel.expandMyBar = !viewModel.expandMyBar
                    viewModel.expandBar(false, recomposeKey = Random.nextInt())
                })
            })
    }
}

@Composable
private fun TopBar(viewModel: ViewImageViewModel, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            IconButton(onClick = {
                viewModel.userAction.value = UserAction.Back
            }) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.ArrowBackIosNew),
                    contentDescription = null,
                )
            }
        }
        IconButton(
            onClick = {},
            modifier = Modifier.padding(end = SmallPadding)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.ScreenRotation),
                contentDescription = null
            )
        }

    }
}

@Composable
private fun BottomBar(
    items: LazyPagingItems<MediaItem>,
    notPreview: Bitmap,
    height: Dp,
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val padding = height * 0.15f
    Box(
        modifier = modifier
            .padding(top = padding, bottom = padding)
            .fillMaxWidth()
    ) {
        LazyRow(state = state) {
            items(count = items.itemCount) {
                items[it]?.let { item ->
                    if (item.type == ItemType.IMAGE) {
                        DisplayImage(
                            bitmap = if (item.fileSize < ImageSize.ONE_K.size) item.thumbnail
                                ?: notPreview else item.thumbnailState.value
                                ?: notPreview,
                            orientation = item.orientation,
                            aspectRatio = 0.75f,
                            modifier = Modifier.padding(end = TinyPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun View(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black))
}

@Preview
@Composable
fun Preview() {
    PhotoAlbumTheme {

    }
}
