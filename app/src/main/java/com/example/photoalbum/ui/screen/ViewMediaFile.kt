package com.example.photoalbum.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.example.photoalbum.R
import com.example.photoalbum.data.DataList
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Composable
fun ViewMediaFile(viewModel: ViewMediaFileViewModel) {
    viewModel.expandBar(false)
    val statusBarHeight = WindowInsets.systemBars.getTop(LocalDensity.current)
    val statusBarHeightDp = with(LocalDensity.current) { statusBarHeight.toDp() }
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    var topPaddingValues = PaddingValues()
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
            if (viewModel.expandMyBar) BottomBar(
                items = viewModel.source.items,
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
    items: DataList,
    notPreview: Bitmap,
    height: Dp,
    screenWidth: Dp,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier
) {
    val state =
        rememberPagerState(initialPage = selectItemIndex.intValue, pageCount = { items.size() })
    val padding = height * 0.2f
    val itemHeight = height - padding * 2
    val itemWidth = (itemHeight * 0.75f)
    val estimateItemNumber: Int = screenWidth.value.toInt() / itemWidth.value.toInt()
    val startPadding = (estimateItemNumber / 2) * itemWidth

    val scope = rememberCoroutineScope()
    val animateFlag = remember { mutableIntStateOf(0) }
    var scrollPage by remember { mutableFloatStateOf(0f) }
    val animation = remember { Animatable(initialValue = selectItemIndex.intValue.toFloat()) }

    LaunchedEffect(animateFlag.intValue) {
        if (!animation.isRunning && animateFlag.intValue > 0) {
            try {
                animation.animateTo(
                    targetValue = selectItemIndex.intValue + scrollPage,
                    animationSpec = tween(durationMillis = abs(scrollPage).toInt() * 75)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                animation.snapTo(animation.value)
            }
        }
    }

    LaunchedEffect(animation.value.toInt(), animation.isRunning) {
        if (animation.isRunning) {
            state.animateScrollToPage(animation.value.toInt())
        }
    }

    LaunchedEffect(state.currentPage) {
        selectItemIndex.intValue = state.currentPage
    }

    Box {
        HorizontalPager(
            userScrollEnabled = false,
            contentPadding = PaddingValues(start = startPadding, end = startPadding),
            state = state,
            pageSpacing = TinyPadding,
            pageSize = PageSize.Fixed(itemWidth),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxHeight()
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker() // 记录滑动速度
                    var isDragging = false // 用于标记是否正在拖动
                    var addup = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // 拖动开始，初始化状态
                            isDragging = true
                            velocityTracker.resetTracking()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // 拖动中，记录增量和位置
                            if (isDragging) {
                                change.consume() // 消费事件，防止干扰
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                ) // 记录位置和时间
                                addup += dragAmount
                                if (abs(addup) >= itemWidth.toPx() / 2 && !animation.isRunning) {
                                    if (addup < 0) {
                                        addup = 0f
                                        scrollPage = min(
                                            1f,
                                            items.size() - selectItemIndex.intValue.toFloat() - 1
                                        )
                                        animateFlag.intValue += 1
                                    } else {
                                        addup = 0f
                                        scrollPage = max(-1f, -selectItemIndex.intValue.toFloat())
                                        animateFlag.intValue += 1
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            // 拖动结束，切换到滑动逻辑
                            isDragging = false
                            addup = 0f
                            val velocity = velocityTracker.calculateVelocity() // 计算滑动速度
                            val speed = velocity.x // 水平方向的速度
                            if (abs(speed) > 1000) { // 自定义滑动速度阈值
                                if (speed < 0) {
                                    scrollPage = min(
                                        20f,
                                        items.size() - selectItemIndex.intValue.toFloat() - 1
                                    )
                                    animateFlag.intValue += 1
                                } else {
                                    scrollPage = max(-20f, -selectItemIndex.intValue.toFloat())
                                    animateFlag.intValue += 1
                                }
                            }
                        },
                        onDragCancel = {
                            // 拖动取消的处理
                            addup = 0f
                            scope.launch {
                                animation.stop()
                            }
                            isDragging = false
                            println("测试:拖动取消")
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {

                    }, onPress = {
                        scope.launch {
                            animation.stop()
                        }
                    })
                }
        ) { page ->
            println("测试 $page")
            items[page].let {
                Box(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .height(itemHeight)
                        .fillMaxWidth()
                ) {
                    val image = it.thumbnailState.value?.let { bitmap ->
                        if (bitmap.isRecycled) it.thumbnail
                        else bitmap
                    } ?: it.thumbnail
                    DisplayImage(
                        bitmap = image ?: notPreview,
                        context = context,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (page != selectItemIndex.intValue) {
                                modifier.drawWithContent {
                                    drawContent()
                                    drawRect(
                                        color = Color.Gray.copy(alpha = 0.8f),
                                        size = size
                                    )
                                }
                            } else modifier)
                    )
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
    val state = rememberLazyListState()
    ZoomViewImage(
        isRow = viewModel.isRow,
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
    //items: LazyPagingItems<MediaItem>,
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
                /*items(count = items.itemCount) {
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
                }*/
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
