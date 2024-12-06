package com.example.photoalbum.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.R
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ViewMediaFile(viewModel: ViewMediaFileViewModel) {
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
            val listState = viewModel.thumbnailScrollState
            if (viewModel.expandMyBar) BottomBar(
                state = listState,
                items = flow,
                height = screenHeightDp * 0.12f,
                screenWidth = configuration.screenWidthDp.dp,
                notPreview = viewModel.notPreviewIcon,
                selectItemIndex = viewModel.itemIndex,
                context = viewModel.application.applicationContext,
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
        View(
            viewModel = viewModel,
            screenHeight = screenHeightDp,
            screenWidth = configuration.screenWidthDp.dp,
            context = viewModel.application.applicationContext,
            modifier = Modifier
                .zIndex(0f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        viewModel.expandMyBar = !viewModel.expandMyBar
                        viewModel.expandBar(false, recomposeKey = Random.nextInt())
                    })
                }
        )
    }
}

@Composable
private fun TopBar(viewModel: ViewMediaFileViewModel, modifier: Modifier = Modifier) {
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
    screenWidth: Dp,
    state: LazyListState,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val padding = height * 0.2f
    val itemHeight = height - padding * 2
    val itemWidth = (itemHeight * 0.75f)
    val estimateItemNumber: Int = screenWidth.value.toInt() / itemWidth.value.toInt()
    val startPadding = (estimateItemNumber / 2) * itemWidth
    var previousScrollOffset = remember { 0.dp }
    var offset = remember { 0.dp }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        state.scrollToItem(selectItemIndex.intValue)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, scroll) ->
                val scrollDp = with(density) { scroll.toDp() }
                val scrollDifference = scrollDp - previousScrollOffset
                offset += scrollDifference
                //println("before index ${selectItemIndex.intValue} ,scrollDiff $scrollDifference , scrollDp $scroll, 累计$offset, $itemWidth")
                if (scrollDifference > 0.dp && offset > itemWidth / 3) {
                    if (index + 1 <= items.itemCount - 1) selectItemIndex.intValue = index + 1
                    //println("加 ${selectItemIndex.intValue}")
                } else if (scrollDifference < 0.dp && offset > itemWidth / 3) {
                    selectItemIndex.intValue = index
                    //println("减 ${selectItemIndex.intValue}")
                }
                previousScrollOffset = scrollDp
                //println("after index ${selectItemIndex.intValue} ,scrollDiff $scrollDifference , scrollDp $scroll, 累计$offset")
            }
    }

    Box(
        modifier = modifier
            .padding(top = padding, bottom = padding)
            .fillMaxWidth()
    ) {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(start = startPadding, end = startPadding)
        ) {
            items(count = items.itemCount) {
                items[it]?.let { item ->
                    val select = it == selectItemIndex.intValue
                    if (item.type == ItemType.IMAGE) {
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = if (select) TinyPadding else 0.dp,
                                    end = if (select) TinyPadding * 2 else TinyPadding
                                )
                                .height(itemHeight)
                                .width(itemWidth)
                                .graphicsLayer(
                                    scaleX = if (select) 1.2f else 1f,
                                    scaleY = if (select) 1.2f else 1f,
                                )
                                .clickable {
                                    scope.launch {
                                        state.animateScrollToItem(it)
                                    }
                                    selectItemIndex.intValue = it
                                }
                        ) {
                            DisplayImage(
                                bitmap = item.thumbnail
                                    ?: item.thumbnailState.value?.let { bitmap ->
                                        if (bitmap.isRecycled) item.thumbnail
                                        else bitmap
                                    } ?: notPreview,
                                context = context,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun View(
    viewModel: ViewMediaFileViewModel,
    modifier: Modifier = Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    context: Context
) {
    val items = viewModel.imageFlow.value.collectAsLazyPagingItems()
    val state = rememberLazyListState()
    ZoomViewImage(
        isRow = viewModel.isRow,
        items = items,
        notPreview = viewModel.notPreviewIcon,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        state = state,
        selectItemIndex = viewModel.itemIndex,
        context = context,
        modifier = modifier.background(if (viewModel.expandMyBar) Color.White else Color.Black)
    )
}

@Composable
fun ZoomViewImage(
    isRow: Boolean,
    items: LazyPagingItems<MediaItem>,
    notPreview: Bitmap,
    screenWidth: Dp,
    screenHeight: Dp,
    state: LazyListState,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(onDrag = { change, dragAmount -> true })
                detectTapGestures(onTap = { true })
                detectTransformGestures(onGesture = { centroid, pan, zoom, rotation -> true })
            }) {
        if (isRow) {
            LazyRow(
                state = state,
                userScrollEnabled = false
            ) {
                items(count = items.itemCount) {
                    items[it]?.let { item ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.width(screenWidth)
                        ) {
                            DisplayImage(
                                bitmap = item.dataBitmap.value ?: item.thumbnail ?: notPreview,
                                contentScale = ContentScale.FillWidth,
                                context = context,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(state = state) { }
        }
    }
}

@Preview
@Composable
fun Preview() {
    PhotoAlbumTheme {

    }
}
