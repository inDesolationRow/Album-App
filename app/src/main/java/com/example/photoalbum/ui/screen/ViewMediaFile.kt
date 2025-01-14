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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.photoalbum.R
import com.example.photoalbum.data.DataList
import com.example.photoalbum.data.DataService
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.MessageDialog
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
    val screenHeightDp: Dp
    val screenWidthDp: Dp
    with(LocalDensity.current) {
        screenWidthDp = viewModel.settings.phoneSize?.width?.toDp() ?: LocalConfiguration.current.screenWidthDp.toDp()
        screenHeightDp = viewModel.settings.phoneSize?.height?.toDp() ?: LocalConfiguration.current.screenHeightDp.toDp()
    }
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
                screenWidth = screenWidthDp,
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
    val state =
        rememberPagerState(initialPage = selectItemIndex.intValue, pageCount = { items.size() })
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
            screenHeight = screenHeight,
            density = density,
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

/*
@Composable
fun ZoomViewImage(
    isRow: Boolean,
    items: DataList,
    notPreview: Bitmap,
    screenWidth: Dp,
    screenHeight: Dp,
    density: Density,
    source: DataService<*>,
    pageState: PagerState?,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier,
) {
    pageState?.let { state ->
        var transformOrigin by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
        var imageRatio by remember { mutableFloatStateOf(0f) }
        val doubleScale = 2f
        val maxScale = 4f
        var expand by remember { mutableStateOf(false) }
        var aniStartValue by remember { mutableFloatStateOf(1f) }
        var aniEndValue by remember { mutableFloatStateOf(1f) }
        var aniDuration by remember { mutableIntStateOf(300) }
        val aniScale = animateFloatAsState(
            targetValue = if (expand) aniEndValue else aniStartValue,
            tween(durationMillis = aniDuration, easing = LinearEasing),
            label = "缩放动画"
        )

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

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            if (isRow) {
                HorizontalPager(
                    state = state,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                var tapCenter: Offset? = null
                                var onePointerDrag = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            tapCenter = event.changes.first().position
                                        }

                                        PointerEventType.Release -> {
                                            if (onePointerDrag) {
                                                event.changes
                                                    .first()
                                                    .consume()
                                                onePointerDrag = false
                                            }
                                        }

                                        PointerEventType.Move -> {
                                            if (event.changes.size == 1 && tapCenter != null) {
                                                val pointer = event.changes.first()
                                                val move = (tapCenter - pointer.position).getDistance()
                                                if (move >= 5f) {
                                                    onePointerDrag = true
                                                }
                                            }
                                            if (event.changes.size == 2) {
                                                event.changes.forEach { pointer ->
                                                    pointer.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .fillMaxHeight()
                ) { page ->
                    items[page].let {
                        val image = it.dataBitmap.value
                        val thumbnail = it.thumbnailState.value?.let { bitmap ->
                            if (bitmap.isRecycled) it.thumbnail
                            else bitmap
                        } ?: it.thumbnail
                        imageRatio = thumbnail?.let { t ->
                            t.width.toFloat() / t.height.toFloat()
                        } ?: 0f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        var doubleClick = false
                                        var multiTouch = false
                                        var tapCenter: Offset? = null
                                        var pressTimer = 0L
                                        var previousClickTimer = 0L
                                        var pointerNum = 0
                                        var initialSpan = 0f
                                        var maxSpan = 0f
                                        var previousSpan = 0f

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Press -> {
                                                    pointerNum++
                                                    multiTouch = pointerNum > 1
                                                    tapCenter = event.changes.first().position
                                                    val currentTime = System.currentTimeMillis()
                                                    pressTimer = currentTime
                                                    // 判断是否为双击
                                                    doubleClick = if (previousClickTimer == 0L) {
                                                        previousClickTimer = currentTime
                                                        // 启动一个协程重置双击计时器，且可以限制双击缩放的频率400ms内最多一次
                                                        scope.launch {
                                                            delay(400)
                                                            previousClickTimer = 0L
                                                        }
                                                        false
                                                    } else if (!multiTouch) {
                                                        val doubleTimer = currentTime - previousClickTimer
                                                        doubleTimer < 400
                                                    } else {
                                                        //如果是多点触控或400ms内点击频率过高，则不予处理
                                                        false
                                                    }
                                                    //计算两个触摸点之间的间隔
                                                    if (event.changes.size == 2) {
                                                        initialSpan = event.changes.let { touchPoint ->
                                                            (touchPoint[0].position - touchPoint[1].position).getDistance()
                                                        }
                                                        maxSpan = initialSpan
                                                    }
                                                }

                                                PointerEventType.Move -> {
                                                    if (event.changes.size == 1 && tapCenter != null) {
                                                        val pointer = event.changes.first()
                                                        val move = (tapCenter!! - pointer.position).getDistance()
                                                        if (move >= 1f) {

                                                        }
                                                    }

                                                    if (event.changes.size == 2) {
                                                        val pointer1 = event.changes[0]
                                                        val pointer2 = event.changes[1]

                                                        //计算缩放中心
                                                        if (aniScale.value != maxScale){
                                                            val originX: Float = event.changes.first().position.x.let { tap ->
                                                                if (aniScale.value == 1f)
                                                                    tap / screenWidth.toPx()
                                                                else {
                                                                    val ratio = tap / screenWidth.toPx()
                                                                    val span = ratio - transformOrigin.x
                                                                    if (span<0){
                                                                        transformOrigin.x - abs(span) / aniScale.value
                                                                    }else{
                                                                        transformOrigin.x + abs(span) / aniScale.value
                                                                    }
                                                                }
                                                            }
                                                            transformOrigin = Offset(originX, 0.5f)
                                                        }

                                                        // 计算当前两点间的距离（span）
                                                        val currentSpan =
                                                            (pointer1.position - pointer2.position).getDistance()

                                                        // 计算缩放比例
                                                        val multiTouchScale = if (currentSpan > previousSpan) {
                                                            previousSpan = currentSpan
                                                            maxSpan = currentSpan
                                                            ((currentSpan - initialSpan) / 100).coerceIn(max(1f, aniScale.value), maxScale)
                                                        } else {
                                                            previousSpan = currentSpan
                                                            ((currentSpan - maxSpan) / 100 + maxScale).coerceIn(1f, aniScale.value)
                                                        }

                                                        aniStartValue = multiTouchScale
                                                        aniEndValue = multiTouchScale
                                                        aniDuration = 0
                                                        expand = multiTouchScale != 1f
                                                        // 更新偏移量以适配缩放
                                                    }
                                                }

                                                PointerEventType.Release -> {
                                                    // 如果是双击，消费事件
                                                    if (doubleClick) {
                                                        doubleClick = false
                                                        if (!expand) {
                                                            val originX: Float = tapCenter?.x?.let { tap ->
                                                                tap / screenWidth.toPx()
                                                            } ?: 0.5f

                                                            val originY: Float = tapCenter?.y?.let { tap ->
                                                                val imageHeight = screenWidth.toPx() / imageRatio
                                                                val halfScreenHeight = screenHeight.toPx() / 2
                                                                if (imageHeight * doubleScale <= screenHeight.toPx()) {
                                                                    0.5f
                                                                } else {
                                                                    val pointerY = tap.coerceIn(
                                                                        halfScreenHeight - imageHeight / 2,
                                                                        halfScreenHeight + imageHeight / 2
                                                                    )
                                                                    val tapRatio =
                                                                        if (imageHeight <= screenHeight.toPx())
                                                                            (pointerY - (halfScreenHeight - imageHeight / 2)) / imageHeight
                                                                        else {
                                                                            val overflow = (imageHeight - screenHeight.toPx()) / 2
                                                                            (overflow + pointerY) / imageHeight
                                                                        }
                                                                    var topEdge =
                                                                        (screenHeight.toPx() - imageHeight) / (2 * imageHeight * (doubleScale - 1))
                                                                    var bottomEdge = 1 - topEdge
                                                                    if (topEdge < 0f) {
                                                                        topEdge = 0f
                                                                        bottomEdge = 1f
                                                                        //tapRatio = if (tapRatio >= 0.95) 1f else if (tapRatio <= 0.5) 0f else tapRatio
                                                                    }
                                                                    tapRatio.coerceIn(topEdge, bottomEdge)
                                                                }
                                                            } ?: 0.5f
                                                            transformOrigin = Offset(originX, originY)
                                                        }

                                                        if (expand) {
                                                            aniStartValue = 1f
                                                            aniDuration = 300
                                                            expand = false
                                                        } else {
                                                            aniStartValue = 1f
                                                            aniEndValue = doubleScale
                                                            aniDuration = 300
                                                            expand = true
                                                        }
                                                        event.changes.forEach { change -> change.consume() }
                                                    }
                                                    // 如果长按超过400ms，消费事件
                                                    if (System.currentTimeMillis() - pressTimer > 400 || multiTouch) {
                                                        event.changes.forEach { change ->
                                                            change.consume()
                                                        }
                                                    }
                                                    pointerNum--
                                                    if (pointerNum == 0) {
                                                        multiTouch = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                        scaleX = aniScale.value,
                                        scaleY = aniScale.value,
                                        transformOrigin = TransformOrigin(transformOrigin.x, transformOrigin.y)
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
*/
@Composable
fun ZoomViewImage(
    isRow: Boolean,
    items: DataList,
    notPreview: Bitmap,
    screenWidth: Dp,
    screenHeight: Dp,
    density: Density,
    source: DataService<*>,
    pageState: PagerState?,
    selectItemIndex: MutableIntState,
    context: Context,
    modifier: Modifier = Modifier,
) {
    pageState?.let { state ->
        var transformOrigin by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
        var imageRatio by remember { mutableFloatStateOf(0f) }
        val doubleScale = 2f
        val maxScale = 4f
        var expand by remember { mutableStateOf(false) }
        var aniStartValue by remember { mutableFloatStateOf(1f) }
        var aniEndValue by remember { mutableFloatStateOf(1f) }
        var aniDuration by remember { mutableIntStateOf(300) }
        var aniFlag = remember { mutableStateOf(0) }
        val aniScale = remember { Animatable(initialValue = aniStartValue) }

        LaunchedEffect(aniFlag.value) {
            if (!aniScale.isRunning && aniFlag.value > 0) {
                aniScale.animateTo(
                    targetValue = aniEndValue,
                    animationSpec = tween(durationMillis = aniDuration, easing = LinearEasing)
                )
            }
        }

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

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            if (isRow) {
                HorizontalPager(
                    state = state,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                var tapCenter: Offset? = null
                                var onePointerDrag = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            tapCenter = event.changes.first().position
                                        }

                                        PointerEventType.Release -> {
                                            if (onePointerDrag) {
                                                event.changes
                                                    .first()
                                                    .consume()
                                                onePointerDrag = false
                                            }
                                        }

                                        PointerEventType.Move -> {
                                            if (event.changes.size == 1 && tapCenter != null) {
                                                val pointer = event.changes.first()
                                                val move = (tapCenter - pointer.position).getDistance()
                                                if (move >= 5f) {
                                                    onePointerDrag = true
                                                }
                                            }
                                            if (event.changes.size == 2) {
                                                event.changes.forEach { pointer ->
                                                    pointer.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .fillMaxHeight()
                ) { page ->
                    items[page].let {
                        val image = it.dataBitmap.value
                        val thumbnail = it.thumbnailState.value?.let { bitmap ->
                            if (bitmap.isRecycled) it.thumbnail
                            else bitmap
                        } ?: it.thumbnail
                        imageRatio = thumbnail?.let { t ->
                            t.width.toFloat() / t.height.toFloat()
                        } ?: 0f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        var doubleClick = false
                                        var multiTouch = false
                                        var tapCenter: Offset? = null
                                        var pressTimer = 0L
                                        var previousClickTimer = 0L
                                        var pointerNum = 0
                                        var initialSpan = 0f
                                        var maxSpan = 0f
                                        var previousSpan = 0f
                                        var pressPointer: Offset? = null

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Press -> {
                                                    pointerNum++
                                                    multiTouch = pointerNum > 1
                                                    tapCenter = event.changes.first().position
                                                    val currentTime = System.currentTimeMillis()
                                                    pressTimer = currentTime
                                                    // 判断是否为双击
                                                    doubleClick = if (previousClickTimer == 0L) {
                                                        previousClickTimer = currentTime
                                                        // 启动一个协程重置双击计时器，且可以限制双击缩放的频率400ms内最多一次
                                                        scope.launch {
                                                            delay(400)
                                                            previousClickTimer = 0L
                                                        }
                                                        false
                                                    } else if (!multiTouch) {
                                                        val doubleTimer = currentTime - previousClickTimer
                                                        doubleTimer < 400
                                                    } else {
                                                        //如果是多点触控或400ms内点击频率过高，则不予处理
                                                        false
                                                    }
                                                    //计算两个触摸点之间的间隔
                                                    if (event.changes.size == 2) {
                                                        initialSpan = event.changes.let { touchPoint ->
                                                            (touchPoint[0].position - touchPoint[1].position).getDistance()
                                                        }
                                                        maxSpan = initialSpan
                                                    }
                                                }

                                                PointerEventType.Move -> {
                                                    if (event.changes.size == 1 && tapCenter != null) {
                                                        val pointer = event.changes.first()
                                                        val move = (tapCenter!! - pointer.position).getDistance()
                                                        if (move >= 1f) {

                                                        }
                                                    }

                                                    if (event.changes.size == 2) {
                                                        val pointer1 = event.changes[0]
                                                        val pointer2 = event.changes[1]
                                                        if (pressPointer == null) pressPointer = pointer1.position

                                                        //计算缩放中心
                                                        if (aniScale.value != maxScale) {
                                                            val originX: Float = event.changes.first().position.x.let { tap ->
                                                                if (aniScale.value == 1f)
                                                                    tap / screenWidth.toPx()
                                                                else {
                                                                    if (abs(pressPointer!!.x - tap) < 10f) {
                                                                        transformOrigin.x
                                                                    } else {
                                                                        val ratio = tap / screenWidth.toPx()
                                                                        val span = ratio - transformOrigin.x
                                                                        if (span < 0) {
                                                                            transformOrigin.x - abs(span) / aniScale.value
                                                                        } else {
                                                                            transformOrigin.x + abs(span) / aniScale.value
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            transformOrigin = Offset(originX, 0.5f)
                                                        }

                                                        // 计算当前两点间的距离（span）
                                                        val currentSpan =
                                                            (pointer1.position - pointer2.position).getDistance()

                                                        // 计算缩放比例
                                                        val multiTouchScale = if (currentSpan > previousSpan) {
                                                            previousSpan = currentSpan
                                                            maxSpan = currentSpan
                                                            ((currentSpan - initialSpan) / 100).coerceIn(max(1f, aniScale.value), maxScale)

                                                        } else {
                                                            previousSpan = currentSpan
                                                            val result = ((currentSpan - maxSpan) / 100 + maxScale).coerceIn(1f, aniScale.value)
                                                            if (result == 1f) initialSpan = currentSpan
                                                            result
                                                        }

                                                        aniStartValue = multiTouchScale
                                                        aniEndValue = multiTouchScale
                                                        aniDuration = 0
                                                        aniFlag.value += 1
                                                        expand = multiTouchScale != 1f
                                                        // 更新偏移量以适配缩放
                                                    }
                                                }

                                                PointerEventType.Release -> {
                                                    // 如果是双击，消费事件
                                                    if (doubleClick) {
                                                        doubleClick = false
                                                        if (!expand) {
                                                            val originX: Float = tapCenter?.x?.let { tap ->
                                                                tap / screenWidth.toPx()
                                                            } ?: 0.5f

                                                            val originY: Float = tapCenter?.y?.let { tap ->
                                                                val imageHeight = screenWidth.toPx() / imageRatio
                                                                val halfScreenHeight = screenHeight.toPx() / 2
                                                                if (imageHeight * doubleScale <= screenHeight.toPx()) {
                                                                    0.5f
                                                                } else {
                                                                    val pointerY = tap.coerceIn(
                                                                        halfScreenHeight - imageHeight / 2,
                                                                        halfScreenHeight + imageHeight / 2
                                                                    )
                                                                    val tapRatio =
                                                                        if (imageHeight <= screenHeight.toPx())
                                                                            (pointerY - (halfScreenHeight - imageHeight / 2)) / imageHeight
                                                                        else {
                                                                            val overflow = (imageHeight - screenHeight.toPx()) / 2
                                                                            (overflow + pointerY) / imageHeight
                                                                        }
                                                                    var topEdge =
                                                                        (screenHeight.toPx() - imageHeight) / (2 * imageHeight * (doubleScale - 1))
                                                                    var bottomEdge = 1 - topEdge
                                                                    if (topEdge < 0f) {
                                                                        topEdge = 0f
                                                                        bottomEdge = 1f
                                                                        //tapRatio = if (tapRatio >= 0.95) 1f else if (tapRatio <= 0.5) 0f else tapRatio
                                                                    }
                                                                    tapRatio.coerceIn(topEdge, bottomEdge)
                                                                }
                                                            } ?: 0.5f
                                                            transformOrigin = Offset(originX, originY)
                                                        }
                                                        println("动画运行状态 ${aniScale.isRunning} 展开状态$expand")
                                                        if (!aniScale.isRunning) {
                                                            if (expand) {
                                                                aniEndValue = 1f
                                                                aniDuration = 300
                                                                aniFlag.value += 1
                                                                expand = false
                                                            } else {
                                                                aniStartValue = 1f
                                                                aniEndValue = doubleScale
                                                                aniDuration = 300
                                                                aniFlag.value += 1
                                                                expand = true
                                                            }
                                                        }
                                                        event.changes.forEach { change -> change.consume() }
                                                    }
                                                    // 如果长按超过400ms，消费事件
                                                    if (System.currentTimeMillis() - pressTimer > 400 || multiTouch) {
                                                        event.changes.forEach { change ->
                                                            change.consume()
                                                        }
                                                    }
                                                    pointerNum--
                                                    if (pointerNum == 0) {
                                                        multiTouch = false
                                                        pressPointer = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                        scaleX = aniScale.value,
                                        scaleY = aniScale.value,
                                        transformOrigin = TransformOrigin(transformOrigin.x, transformOrigin.y)
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



