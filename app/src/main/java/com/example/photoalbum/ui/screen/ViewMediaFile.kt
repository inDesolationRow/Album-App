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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.example.photoalbum.R
import com.example.photoalbum.data.DataList
import com.example.photoalbum.data.DataService
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayBlockImage
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.PlayerSurface
import com.example.photoalbum.ui.common.SURFACE_TYPE_TEXTURE_VIEW
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
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
                var addUp = 0f
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
                            addUp += dragAmount
                            if (abs(addUp) >= itemWidth.toPx() / 2 && !animation.isRunning) {
                                if (addUp < 0) {
                                    addUp = 0f
                                    scrollPage = min(
                                        1f,
                                        items.size() - selectItemIndex.intValue.toFloat() - 1
                                    )
                                    animateFlag.intValue += 1
                                } else {
                                    addUp = 0f
                                    scrollPage = max(-1f, -selectItemIndex.intValue.toFloat())
                                    animateFlag.intValue += 1
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        // 拖动结束，切换到滑动逻辑
                        isDragging = false
                        addUp = 0f
                        val velocity = velocityTracker.calculateVelocity() // 计算滑动速度
                        val speed = velocity.x // 水平方向的速度
                        if (abs(speed) > 2000) { // 自定义滑动速度阈值
                            if (speed < 0) {
                                scrollPage = min(
                                    10f,
                                    items.size() - selectItemIndex.intValue.toFloat()
                                )
                                animateFlag.intValue += 1
                            } else {
                                scrollPage = max(-10f, -selectItemIndex.intValue.toFloat())
                                animateFlag.intValue += 1
                            }
                        }
                    },
                    onDragCancel = {
                        // 拖动取消的处理
                        addUp = 0f
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
            val pageOffset = state.currentPage - page + state.currentPageOffsetFraction
            DisplayImage(
                bitmap = image ?: notPreview,
                context = context,
                modifier = Modifier
                    .background(Color.Transparent)
                    .clip(RoundedCornerShape(5.dp))
                    .height(itemHeight)
                    .fillMaxSize()
                    .graphicsLayer {
                        /*alpha = lerp(
                            start = 0.7f,
                            stop = 1f,
                            fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f),
                        )*/

                        lerp(
                            start = 0.8f,
                            stop = 1f,
                            fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f),
                        ).also { scale ->
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            scope.launch {
                                state.scrollToPage(page)
                            }
                        }, onPress = {
                        })
                    }
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
    val imageWidthPx = loadBitmap.width.toFloat()
    val imageHeightPx = loadBitmap.height.toFloat()
    val heightAdapter = screenWidth / imageWidthPx * imageHeightPx > screenHeight
    LaunchedEffect(Unit) {
        with(density) {
            val targetScale = screenWidth.toPx().let { width ->
                if (heightAdapter)
                    screenHeight.toPx() / imageHeightPx
                else
                    width / imageWidthPx
            }

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
        MediaDisplay(
            isRow = viewModel.isRow,
            items = viewModel.source.items,
            notPreview = viewModel.notPreviewIcon,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            source = viewModel.source,
            pageState = pagerState.value,
            selectItemIndex = viewModel.itemIndex,
            player = viewModel.source.exoPlayer,
            context = context,
            modifier = Modifier.graphicsLayer(
                alpha = if (ani.isRunning) 0f else 1f
            )
        )

        DisplayImage(
            bitmap = loadBitmap,
            context = context,
            contentScale = if (heightAdapter) ContentScale.FillHeight else ContentScale.FillWidth,
            modifier = Modifier.graphicsLayer(
                scaleX = ani.value,
                scaleY = ani.value,
                alpha = if (ani.isRunning) 1f else 0f
            )
        )
    }
}

@Composable
fun MediaDisplay(
    isRow: Boolean,
    items: DataList,
    notPreview: Bitmap,
    screenWidth: Dp,
    screenHeight: Dp,
    player: ExoPlayer,
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
        var aniScaleStartValue by remember { mutableFloatStateOf(1f) }
        var aniScaleEndValue by remember { mutableFloatStateOf(1f) }
        var aniScaleDuration by remember { mutableIntStateOf(300) }
        val aniScaleFlag = remember { mutableIntStateOf(0) }
        val aniScale = remember { Animatable(initialValue = aniScaleStartValue) }

        var aniTransOriginXStartVal by remember { mutableFloatStateOf(-1f) }
        var aniTransOriginXEndVal by remember { mutableFloatStateOf(-1f) }
        var aniTransOriginXDuration by remember { mutableIntStateOf(200) }
        val aniTransOriginX = remember(aniTransOriginXStartVal) { Animatable(aniTransOriginXStartVal) }
        val aniTransOriginXFlag = remember { mutableIntStateOf(0) }

        var aniTransOriginYStartVal by remember { mutableFloatStateOf(-1f) }
        var aniTransOriginYEndVal by remember { mutableFloatStateOf(-1f) }
        var aniTransOriginYDuration by remember { mutableIntStateOf(200) }
        val aniTransOriginY = remember(aniTransOriginYStartVal) { Animatable(aniTransOriginYStartVal) }
        val aniTransOriginYFlag = remember { mutableIntStateOf(0) }

        val screenWidthPx: Float
        val screenHeightPx: Float
        with(LocalDensity.current) {
            screenWidthPx = screenWidth.toPx()
            screenHeightPx = screenHeight.toPx()
        }

        val clearParams = {
            transformOrigin = Offset(0.5f, 0.5f)
            aniScaleStartValue = 1f
            aniScaleEndValue = 1f
            aniScaleDuration = 0
            aniScaleFlag.intValue += 1
            expand = false
        }

        LaunchedEffect(aniScaleFlag.intValue) {
            if (!aniScale.isRunning && aniScaleFlag.intValue > 0) {
                if (aniScaleStartValue == aniScaleEndValue) {
                    aniScale.snapTo(aniScaleStartValue)
                } else {
                    aniScale.snapTo(aniScaleStartValue)
                    aniScale.animateTo(
                        targetValue = aniScaleEndValue,
                        animationSpec = tween(durationMillis = aniScaleDuration, easing = LinearEasing)
                    )
                }
            }
        }

        LaunchedEffect(aniTransOriginXFlag.intValue) {
            if (aniTransOriginXFlag.intValue != 0) {
                aniTransOriginX.animateTo(
                    targetValue = aniTransOriginXEndVal,
                    tween(durationMillis = aniTransOriginXDuration, easing = LinearEasing)
                )
            }
        }

        LaunchedEffect(aniTransOriginYFlag.intValue) {
            if (aniTransOriginYFlag.intValue != 0) {
                aniTransOriginY.animateTo(
                    targetValue = aniTransOriginYEndVal,
                    tween(durationMillis = aniTransOriginYDuration, easing = LinearEasing)
                )
            }
        }

        LaunchedEffect(aniTransOriginX.value, aniTransOriginY.value) {
            if (aniTransOriginX.value >= 0f) {
                transformOrigin = Offset(aniTransOriginX.value, transformOrigin.y)
            }
            if (aniTransOriginY.value >= 0f && aniTransOriginY.isRunning) {
                transformOrigin = Offset(transformOrigin.x, aniTransOriginY.value)
            }
        }

        LaunchedEffect(Unit) {
            source.loadMediaFile(selectItemIndex.intValue)
        }

        LaunchedEffect(state.currentPage) {
            if (state.currentPage != selectItemIndex.intValue) {
                clearParams()
                selectItemIndex.intValue = state.currentPage
                source.loadMediaFile(selectItemIndex.intValue)
            }
        }

        LaunchedEffect(selectItemIndex.intValue) {
            if (selectItemIndex.intValue != state.currentPage) {
                clearParams()
                state.scrollToPage(selectItemIndex.intValue)
                source.loadMediaFile(selectItemIndex.intValue)
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
                                var onePointerDrag = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Release -> {
                                            if (onePointerDrag) {
                                                event.changes.forEach { pointer ->
                                                    pointer.consume()
                                                }
                                                onePointerDrag = false
                                            }
                                        }

                                        PointerEventType.Move -> {
                                            if (event.changes.size == 1) {
                                                event.changes.forEach { pointer ->
                                                    if (pointer.isConsumed) {
                                                        onePointerDrag = true
                                                    }
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
                    items[page].let { item ->
                        val image = item.dataBitmap.value
                        val thumbnail = item.thumbnailState.value?.let { bitmap ->
                            if (bitmap.isRecycled) item.thumbnail
                            else bitmap
                        } ?: item.thumbnail

                        imageRatio = item.imageRatio ?: thumbnail?.let { t ->
                            t.width.toFloat() / t.height.toFloat()
                        } ?: 0f

                        val heightAdapter = thumbnail?.let { t ->
                            screenWidthPx / t.width * t.height > screenHeightPx
                        } ?: false
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(page to item.dataBitmap.value) {
                                    if (item.type == ItemType.IMAGE)
                                        awaitPointerEventScope {
                                            var doubleClick = false //双击
                                            var multiTouch = false //多指手势
                                            var tapDrag = false //单指拖动
                                            var pointer1Id = -1L
                                            var pointer2Id = -2L
                                            var pressTimer = 0L
                                            var previousClickTimer = 0L
                                            var initialSpan = 0f
                                            var maxSpan = 0f
                                            var previousSpan = 0f
                                            val imageHeight = screenWidth.toPx() / imageRatio
                                            val halfScreenHeight = screenHeight.toPx() / 2
                                            val velocityTracker = VelocityTracker()

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        velocityTracker.resetTracking()
                                                        multiTouch = event.changes.size > 1
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
                                                        if (event.changes.size == 1) {
                                                            val pointer = event.changes.first()
                                                            val oldPointer = pointer.previousPosition
                                                            val deltaX = pointer.position.x - oldPointer.x
                                                            val deltaY = pointer.position.y - oldPointer.y
                                                            val move = (pointer.position - oldPointer).getDistance()
                                                            if (move > 5f)
                                                                tapDrag = true
                                                            if (aniScale.value > 1f && tapDrag) {
                                                                event.changes.forEach { change ->
                                                                    change.consume()
                                                                }
                                                            }
                                                            velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)
                                                            val ratioX = deltaX / 1000 / aniScale.value
                                                            val ratioY = deltaY / 1000 / aniScale.value
                                                            val transformOriginX = ratioX.let { x ->
                                                                if (x < 0) {
                                                                    if (abs(x) + transformOrigin.x <= 1f) transformOrigin.x + abs(x) else 1f
                                                                } else {
                                                                    if (transformOrigin.x - x >= 0f) transformOrigin.x - x else 0f
                                                                }
                                                            }
                                                            val transformOriginY = ratioY.let { y ->
                                                                if (imageHeight * aniScale.value <= screenHeight.toPx()) {
                                                                    0.5f
                                                                } else {
                                                                    var topEdge = imageHeight.let { height ->
                                                                        if (height > screenHeight.toPx() && aniScale.value == 1f)
                                                                            0f
                                                                        else
                                                                            (screenHeight.toPx() - height) / (2 * height * (aniScale.value - 1))
                                                                    }
                                                                    var bottomEdge = 1 - topEdge
                                                                    if (topEdge < 0f) {
                                                                        topEdge = 0f
                                                                        bottomEdge = 1f
                                                                    }
                                                                    if (y < 0) {
                                                                        if (abs(y) + transformOrigin.y <= bottomEdge) transformOrigin.y + abs(y) else bottomEdge
                                                                    } else {
                                                                        if (transformOrigin.y - y >= topEdge) transformOrigin.y - y else topEdge
                                                                    }
                                                                }
                                                            }
                                                            transformOrigin = Offset(transformOriginX, transformOriginY)
                                                        }

                                                        if (event.changes.size == 2) {
                                                            var transOriginChange = false

                                                            val pointer1 = event.changes.first()
                                                            val pointer2 = event.changes.last()

                                                            val set = event.changes
                                                                .map { it.id.value }
                                                                .toSet()
                                                            if (pointer1Id !in set || pointer2Id !in set) {
                                                                transOriginChange = true
                                                            }

                                                            // 计算当前两点间的距离（span）
                                                            val currentSpan =
                                                                (pointer1.position - pointer2.position).getDistance()

                                                            // 计算缩放比例
                                                            val multiTouchScale = if (currentSpan > previousSpan) {
                                                                val result =
                                                                    ((currentSpan - initialSpan) / 150).coerceIn(max(1f, aniScale.value), maxScale)
                                                                if (abs(result - aniScale.value) < maxScale / 10) {
                                                                    previousSpan = currentSpan
                                                                    maxSpan = currentSpan
                                                                    result
                                                                } else {
                                                                    aniScale.value
                                                                }
                                                            } else {
                                                                val result = ((currentSpan - maxSpan) / 150 + maxScale).coerceIn(1f, aniScale.value)
                                                                if (abs(result - aniScale.value) < maxScale / 10) {
                                                                    previousSpan = currentSpan
                                                                    if (result == 1f) initialSpan = currentSpan
                                                                    result
                                                                } else {
                                                                    aniScale.value
                                                                }
                                                            }
                                                            if (multiTouchScale > aniScale.value) {
                                                                pointer1Id = pointer1.id.value
                                                                pointer2Id = pointer2.id.value
                                                                var topEdge = imageHeight.let { height ->
                                                                    if (heightAdapter && aniScale.value == 1f)
                                                                        0.5f
                                                                    else if (heightAdapter && aniScale.value > 1f)
                                                                        0f
                                                                    else if (!heightAdapter && height * aniScale.value > screenHeightPx)
                                                                        (screenHeight.toPx() - height) / (2 * height * (aniScale.value - 1))
                                                                    else
                                                                        0.5f
                                                                }
                                                                var bottomEdge = 1 - topEdge
                                                                if (topEdge < 0f) {
                                                                    topEdge = 0f
                                                                    bottomEdge = 1f
                                                                }

                                                                val p1 = event.changes.first().position
                                                                val p2 = event.changes.last().position
                                                                if (aniScale.value == 1f && multiTouchScale > aniScale.value) {
                                                                    val x = ((p1.x + p2.x) / 2) / screenWidthPx
                                                                    val y = (((p1.y + p2.y) / 2) / screenHeightPx).coerceIn(topEdge, bottomEdge)
                                                                    transformOrigin = Offset(x, y)
                                                                }
                                                                if (multiTouchScale > aniScale.value && transOriginChange) {
                                                                    val x =
                                                                        (((p1.x + p2.x) / 2 / screenWidthPx) - transformOrigin.x) / aniScale.value + transformOrigin.x
                                                                    val y =
                                                                        ((((p1.y + p2.y) / 2 / screenHeightPx) - transformOrigin.y) / aniScale.value + transformOrigin.y).coerceIn(
                                                                            topEdge,
                                                                            bottomEdge
                                                                        )
                                                                    aniTransOriginXStartVal = transformOrigin.x
                                                                    aniTransOriginXEndVal = x
                                                                    aniTransOriginXDuration = (abs(x - transformOrigin.x) * 300).toInt()
                                                                    aniTransOriginXFlag.intValue += 1

                                                                    aniTransOriginYStartVal = transformOrigin.y
                                                                    aniTransOriginYEndVal = y
                                                                    aniTransOriginYDuration = (abs(y - transformOrigin.y) * 300).toInt()
                                                                    aniTransOriginYFlag.intValue += 1
                                                                }
                                                            }
                                                            aniScaleStartValue = multiTouchScale
                                                            aniScaleEndValue = multiTouchScale
                                                            aniScaleDuration = 0
                                                            aniScaleFlag.intValue += 1
                                                            expand = multiTouchScale != 1f
                                                            // 更新偏移量以适配缩放
                                                        }
                                                    }

                                                    PointerEventType.Release -> {
                                                        // 如果是双击，消费事件
                                                        if (doubleClick && !tapDrag) {
                                                            doubleClick = false
                                                            if (!expand) {
                                                                val originX: Float = event.changes.first().position.x.let { tap ->
                                                                    tap / screenWidth.toPx()
                                                                }

                                                                val originY: Float = event.changes.first().position.y.let { tap ->
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
                                                                        }
                                                                        tapRatio.coerceIn(topEdge, bottomEdge)
                                                                    }
                                                                }
                                                                transformOrigin = Offset(originX, originY)
                                                            }

                                                            if (!aniScale.isRunning) {
                                                                if (expand) {
                                                                    if (aniScaleStartValue == 1f)
                                                                        aniScaleStartValue = doubleScale
                                                                    aniScaleEndValue = 1f
                                                                    aniScaleDuration = 300
                                                                    aniScaleFlag.intValue += 1
                                                                    expand = false
                                                                } else {
                                                                    aniScaleStartValue = 1f
                                                                    aniScaleEndValue = doubleScale
                                                                    aniScaleDuration = 300
                                                                    aniScaleFlag.intValue += 1
                                                                    expand = true
                                                                }
                                                            }
                                                            event.changes.forEach { change -> change.consume() }
                                                        }

                                                        if (tapDrag && !doubleClick && !multiTouch && aniScale.value > 1f) {
                                                            val velocity = velocityTracker.calculateVelocity()
                                                            val speedX = velocity.x // 水平方向的速度
                                                            val speedY = velocity.y
                                                            var topEdge = imageHeight.let { height ->
                                                                if (heightAdapter && aniScale.value == 1f)
                                                                    0.5f
                                                                else if (heightAdapter && aniScale.value > 1f)
                                                                    0f
                                                                else if (!heightAdapter && height * aniScale.value > screenHeightPx)
                                                                    (screenHeight.toPx() - height) / (2 * height * (aniScale.value - 1))
                                                                else
                                                                    0.5f
                                                            }
                                                            var bottomEdge = 1 - topEdge
                                                            if (topEdge < 0f) {
                                                                topEdge = 0f
                                                                bottomEdge = 1f
                                                            }
                                                            val transformOriginX = speedX.let {
                                                                if (abs(speedX) > 2000) {
                                                                    val ratio = speedX / 10000
                                                                    if (ratio < 0) {
                                                                        if (abs(ratio) + transformOrigin.x < 1f)
                                                                            abs(ratio) + transformOrigin.x
                                                                        else
                                                                            1f
                                                                    } else {
                                                                        if (transformOrigin.x - ratio > 0f)
                                                                            transformOrigin.x - ratio
                                                                        else
                                                                            0f
                                                                    }
                                                                } else
                                                                    transformOrigin.x
                                                            }

                                                            val transformOriginY = speedY.let {
                                                                if (abs(speedY) > 2000 && topEdge != 0.5f) {
                                                                    val ratio = speedY / 10000
                                                                    if (ratio < 0) {
                                                                        if (transformOrigin.y - abs(ratio) >= bottomEdge)
                                                                            transformOrigin.y - abs(ratio)
                                                                        else
                                                                            bottomEdge
                                                                    } else {
                                                                        if (transformOrigin.y + ratio <= topEdge)
                                                                            transformOrigin.y + ratio
                                                                        else
                                                                            topEdge
                                                                    }
                                                                } else {
                                                                    transformOrigin.y
                                                                }
                                                            }
                                                            if (transformOriginX != transformOrigin.x) {
                                                                aniTransOriginXStartVal = transformOrigin.x
                                                                aniTransOriginXEndVal = transformOriginX
                                                                aniTransOriginXDuration = (abs(transformOriginX - transformOrigin.x) * 300).toInt()
                                                                aniTransOriginXFlag.intValue += 1
                                                            }

                                                            if (transformOriginY != transformOrigin.y) {
                                                                aniTransOriginYStartVal = transformOrigin.y
                                                                aniTransOriginYEndVal = transformOriginY
                                                                aniTransOriginYDuration = (abs(transformOriginY - transformOrigin.y) * 300).toInt()
                                                                aniTransOriginYFlag.intValue += 1
                                                            }
                                                        }
                                                        if (System.currentTimeMillis() - pressTimer > 400 || multiTouch) {
                                                            event.changes.forEach { change ->
                                                                change.consume()
                                                            }
                                                        }

                                                        if (event.changes.size == 1) {
                                                            multiTouch = false
                                                            tapDrag = false
                                                            initialSpan = 0f
                                                            maxSpan = 0f
                                                            previousSpan = 0f
                                                            pointer1Id = -1L
                                                            pointer2Id = -1L
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                }
                        ) {
                            val thumbnailAlpha by animateFloatAsState(
                                targetValue = if (image == null && !item.videoWhenReady.value) 1f else 0f,
                                animationSpec = tween(durationMillis = 10), label = "" // 淡出动画
                            )
                            DisplayImage(
                                bitmap = thumbnail ?: notPreview,
                                context = context,
                                contentScale = if (heightAdapter) ContentScale.FillHeight else ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        alpha = thumbnailAlpha
                                    )
                            )
                            if (item.type == ItemType.IMAGE) {
                                DisplayImage(
                                    bitmap = image ?: notPreview,
                                    context = context,
                                    contentScale = if (heightAdapter) ContentScale.FillHeight else ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .graphicsLayer(
                                            alpha = if (image == null && !item.videoWhenReady.value) 0f else 1f,
                                            scaleX = aniScale.value,
                                            scaleY = aniScale.value,
                                            transformOrigin = TransformOrigin(transformOrigin.x, transformOrigin.y)
                                        )
                                )
                                /*DisplayBlockImage(
                                    imageBlockList = item.imageBlockList,
                                    context = context,
                                    contentScale = if (heightAdapter) ContentScale.FillHeight else ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .graphicsLayer(
                                            alpha = if (item.imageBlockList.isEmpty()) 0f else 1f,
                                            scaleX = aniScale.value,
                                            scaleY = aniScale.value,
                                            transformOrigin = TransformOrigin(transformOrigin.x, transformOrigin.y)
                                        )
                                )*/
                            } else if (item.type == ItemType.VIDEO && item.videoWhenReady.value) {
                                PlayerSurface(
                                    player = player,
                                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                                    modifier = Modifier.height(with(LocalDensity.current) {
                                        val width = item.resolution.split("×").first().toFloat().toDp()
                                        val height = item.resolution.split("×").last().toFloat().toDp()
                                        item.orientation.let {
                                            if (it == 90 || it == 270)
                                                width * (screenWidth / height)
                                            else
                                                height * (screenWidth / width)
                                        }
                                    })
                                )
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.Main) {
                                        delay(100)
                                        player.play()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                VerticalPager(state = state) { }
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                }

                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}



