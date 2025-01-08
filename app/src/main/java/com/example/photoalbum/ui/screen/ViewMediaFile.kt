package com.example.photoalbum.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.example.photoalbum.R
import com.example.photoalbum.data.DataList
import com.example.photoalbum.data.DataService
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Composable
fun ViewMediaFile(viewModel: ViewMediaFileViewModel) {
    LaunchedEffect(Unit) {
        viewModel.expandBar(false)
    }
    val statusBarHeight = WindowInsets.systemBars.getTop(LocalDensity.current)
    val statusBarHeightDp = with(LocalDensity.current) { statusBarHeight.toDp() }
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    var topPaddingValues = PaddingValues()
    val scope = rememberCoroutineScope()
    var job: Job? = null
    if (viewModel.showDialog.isShow) {
        if (viewModel.showDialog.mediaListDialog == MediaListDialog.LOCAL_NET_OFFLINE) {
            MessageDialog(
                messageRes = R.string.local_net_offline,
                clickConfirm = viewModel.showDialog.onClick
            ) { viewModel.showDialog.onClick }
        }
    }
    if (viewModel.initializer) {
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
                    height = screenHeightDp * 0.10f,
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
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                //如果子composable在400ms内再次点击，事件被消费则取消切换ui
                                if (event.changes.all { change -> change.isConsumed }) {
                                    job?.cancel()
                                    continue
                                }
                                when (event.type) {
                                    PointerEventType.Release -> {
                                        //等待400ms再切换ui，如果这期间子composable再次被点击则取消
                                        job = scope.launch {
                                            delay(400)
                                            viewModel.expandMyBar = !viewModel.expandMyBar
                                            viewModel.expandBar(
                                                false,
                                                recomposeKey = Random.nextInt()
                                            )
                                            event.changes.forEach { change -> change.consume() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .background(
                        if (viewModel.expandMyBar) Color.White else Color.Black
                    )
            )
        }
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
    modifier: Modifier = Modifier,
) {
    val state = rememberPagerState(initialPage = selectItemIndex.intValue, pageCount = { items.size() })
    val padding = height * 0.2f
    val itemHeight = height - padding * 2
    val itemWidth = (itemHeight * 0.7f)
    val startPadding = (screenWidth - itemWidth) / 2

    val scope = rememberCoroutineScope()
    val animateFlag = remember { mutableIntStateOf(0) }
    var scrollPage by remember { mutableFloatStateOf(0f) }
    val animation = remember { Animatable(initialValue = selectItemIndex.intValue.toFloat()) }

    LaunchedEffect(animateFlag.intValue) {
        if (!animation.isRunning && animateFlag.intValue > 0) {
            try {
                animation.animateTo(
                    targetValue = selectItemIndex.intValue + scrollPage,
                    animationSpec = tween(
                        durationMillis = abs(scrollPage).toInt() * 50,
                        easing = LinearEasing
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                animation.snapTo(animation.value)
            }
        }
    }

    LaunchedEffect(animation.value.toInt(), animation.isRunning) {
        if (animation.isRunning) {
            state.animateScrollToPage(
                animation.value.toInt(), animationSpec = tween(
                    durationMillis = 50,
                    easing = LinearEasing
                )
            )
        }
    }

    LaunchedEffect(state.currentPage) {
        if (state.currentPage != selectItemIndex.intValue)
            selectItemIndex.intValue = state.currentPage
    }

    LaunchedEffect(selectItemIndex.intValue) {
        if (selectItemIndex.intValue != state.currentPage)
            state.animateScrollToPage(selectItemIndex.intValue)
    }

    HorizontalPager(
        userScrollEnabled = false,
        contentPadding = PaddingValues(start = startPadding, end = startPadding),
        state = state,
        pageSpacing = TinyPadding,
        pageSize = PageSize.Fixed(itemWidth),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Color.Transparent)
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
                                    println("测试:滚动 $scrollPage")
                                    animateFlag.intValue += 1
                                } else {
                                    addup = 0f
                                    scrollPage = max(-1f, -selectItemIndex.intValue.toFloat())
                                    println("测试:滚动 $scrollPage")
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
                        if (abs(speed) > 2000) { // 自定义滑动速度阈值
                            if (speed < 0) {
                                scrollPage = min(
                                    10f,
                                    items.size() - selectItemIndex.intValue.toFloat()
                                )
                                println("测试:滚动 $scrollPage")
                                animateFlag.intValue += 1
                            } else {
                                scrollPage = max(-10f, -selectItemIndex.intValue.toFloat())
                                println("测试:滚动 $scrollPage")
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
        items[page].let {
            val image = it.thumbnailState.value?.let { bitmap ->
                if (bitmap.isRecycled) it.thumbnail
                else bitmap
            } ?: it.thumbnail
            DisplayImage(
                bitmap = image ?: notPreview,
                context = context,
                modifier = Modifier
                    .background(Color.Transparent)
                    .clip(RoundedCornerShape(5.dp))
                    .height(itemHeight)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            scope.launch {
                                state.scrollToPage(page)
                            }
                        }, onPress = {
                        })
                    }
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

@Composable
fun View(
    viewModel: ViewMediaFileViewModel,
    modifier: Modifier = Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    context: Context,
) {
    val ani = remember {
        Animatable(initialValue = 1f)
    }
    val density = LocalDensity.current
    val loadBitmap = viewModel.application.loadThumbnailBitmap ?: viewModel.notPreviewIcon
    LaunchedEffect(Unit) {
        with(density) {
            val imageWidthPx = loadBitmap.width.toFloat()
            val targetScale = screenWidth.toPx() / imageWidthPx
            ani.animateTo(
                targetValue = targetScale,
                animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing)
            )
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        val loadPageParams by remember {
            derivedStateOf { viewModel.loadPageParams.value }
        }

        val pagerState: MutableState<PagerState?> = remember { mutableStateOf(null) }
        LaunchedEffect(loadPageParams) {
            if (loadPageParams.first != -1) {
                pagerState.value = PagerState(
                    currentPage = viewModel.loadPageParams.value.first
                ) {
                    viewModel.loadPageParams.value.second
                }
            }
        }
        ZoomViewImage(
            isRow = viewModel.isRow,
            items = viewModel.source.items,
            notPreview = viewModel.notPreviewIcon,
            screenWidth = screenWidth,
            source = viewModel.source,
            pageState = pagerState.value,
            selectItemIndex = viewModel.itemIndex,
            context = context,
            modifier = Modifier.graphicsLayer(
                alpha = if (ani.isRunning) 0f else 1f
            )
        )

        DisplayImage(
            bitmap = loadBitmap,
            context = context,
            contentScale = ContentScale.None,
            modifier = Modifier.graphicsLayer(
                scaleX = ani.value,
                scaleY = ani.value,
                alpha = if (ani.isRunning) 1f else 0f
            )
        )
    }
}

@Composable
fun ZoomViewImage(
    isRow: Boolean,
    items: DataList,
    notPreview: Bitmap,
    screenWidth: Dp,
    source: DataService<*>,
    pageState: PagerState?,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier,
) {
    pageState?.let { state ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset(0f, 0f)) }
        var pivot by remember { mutableStateOf(Offset(0f, 0f)) }
        LaunchedEffect(Unit) {
            source.loadImage(selectItemIndex.intValue)
        }

        LaunchedEffect(state.currentPage) {
            if (state.currentPage != selectItemIndex.intValue) {
                selectItemIndex.intValue = state.currentPage
                source.loadImage(selectItemIndex.intValue)
            }
        }

        LaunchedEffect(selectItemIndex.intValue) {
            if (selectItemIndex.intValue != state.currentPage) {
                state.scrollToPage(selectItemIndex.intValue)
                source.loadImage(selectItemIndex.intValue)
            }
        }

        val scope = rememberCoroutineScope()

        Box(contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var doubleClick = false
                        var previousClickTimer = 0L
                        var pressTimer = 0L
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val currentTime = System.currentTimeMillis()
                                    pressTimer = currentTime
                                    // 判断是否为双击
                                    doubleClick = if (previousClickTimer == 0L) {
                                        previousClickTimer = currentTime
                                        false
                                    } else {
                                        val doubleTimer = currentTime - previousClickTimer
                                        doubleTimer < 400
                                    }
                                    // 启动一个协程重置双击计时器
                                    scope.launch {
                                        delay(400)
                                        previousClickTimer = 0L
                                    }
                                }

                                PointerEventType.Release -> {
                                    // 如果是双击，消费事件
                                    if (doubleClick) {
                                        doubleClick = false
                                        event.changes.forEach { it.consume() }
                                    }
                                    // 如果长按超过400ms，消费事件
                                    if (System.currentTimeMillis() - pressTimer > 400
                                    ) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }

                                PointerEventType.Move -> {

                                }
                            }
                        }
                    }
                }) {
            if (isRow) {
                HorizontalPager(
                    state = state,
                    verticalAlignment = Alignment.CenterVertically,
                    pageSize = PageSize.Fixed(screenWidth),
                    modifier = modifier
                        .fillMaxHeight()
                ) { page ->
                    items[page].let {
                        val image = it.dataBitmap.value
                        val thumbnail = it.thumbnailState.value?.let { bitmap ->
                            if (bitmap.isRecycled) it.thumbnail
                            else bitmap
                        } ?: it.thumbnail
                        //println("测试:大图重组$page $image")
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val thumbnailAlpha by animateFloatAsState(
                                targetValue = if (image == null) 1f else 0f,
                                animationSpec = tween(durationMillis = 10), label = "" // 淡出动画
                            )
                            DisplayImage(
                                bitmap = thumbnail ?: notPreview, context = context,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        alpha = thumbnailAlpha
                                    )
                            )
                            DisplayImage(
                                bitmap = image ?: notPreview, context = context,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        alpha = if (image == null) 0f else 1f,
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y,
                                        transformOrigin = TransformOrigin(
                                            pivot.x / 1000f,
                                            pivot.y / 1000f
                                        )
                                    )
                            )
                        }
                    }
                }
            } else {
                VerticalPager(state = state) { }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    PhotoAlbumTheme {

    }
}
