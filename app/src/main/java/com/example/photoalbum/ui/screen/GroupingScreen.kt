package com.example.photoalbum.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.transition.Transition
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.model.Album
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GroupingScreen(viewModel: GroupingScreenViewModel, modifier: Modifier = Modifier) {
    val multipleChoiceMode = remember { mutableStateOf(false) }
    val multipleChoiceList: SnapshotStateList<String> = remember { mutableStateListOf() }
    val selectAll = remember { mutableStateOf(false) }
    val showPopup = remember { mutableStateOf(false) }
    val isPopupVisible = remember { mutableStateOf(false) } // 控制是否真正显示 Popup
    val exitMultipleChoiceMode = {
        multipleChoiceMode.value = false
        multipleChoiceList.clear()
        selectAll.value = false
    }

    val density = LocalDensity.current
    val actionState = viewModel.userAction.collectAsState()
    var hostHeight by rememberSaveable(saver = dpSaver) { mutableStateOf(0.dp) }
    var getNavHostHeight by rememberSaveable { mutableStateOf(false) }
    val topBarAnimateDp: State<Dp>? = if (getNavHostHeight) {
        animateDpAsState(
            targetValue = if (multipleChoiceMode.value || viewModel.expand) hostHeight else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "隐藏或显示bottomBar"
        )
    } else {
        null
    }
    if (actionState.value is UserAction.ExpandStatusBarAction) {
        viewModel.expand = (actionState.value as UserAction.ExpandStatusBarAction).expand
    }

    BackHandler {
        if (multipleChoiceMode.value)
            exitMultipleChoiceMode()
    }

    Scaffold(
        topBar = {
            TopBar(
                viewModel = viewModel,
                multipleChoiceMode = multipleChoiceMode,
                multipleChoiceList = multipleChoiceList,
                selectAll = selectAll,
                showPopup = showPopup,
                isPopupVisible = isPopupVisible,
                modifier = Modifier
                    .then(
                        if (topBarAnimateDp != null) {
                            Modifier.height(topBarAnimateDp.value)
                        } else {
                            Modifier
                        }
                    )
                    .padding(start = MediumPadding)
                    .onGloballyPositioned { layout ->
                        with(density) {
                            if (!getNavHostHeight) {
                                hostHeight = layout.size.height.toDp()
                                getNavHostHeight = true
                            }
                        }
                    }
            )
        },
        modifier = modifier
    ) { padding ->
        if (viewModel.currentDirectoryInfo.value.first == -1L) {
            GroupingList(
                items = viewModel.groupingList,
                directoryIcon = viewModel.directoryIcon,
                application = viewModel.application,
                multipleChoiceMode = multipleChoiceMode,
                multipleChoiceList = multipleChoiceList,
                modifier = Modifier.padding(padding),
                expand = { expand ->
                    viewModel.userAction.value = UserAction.ExpandStatusBarAction(expand)
                }
            ) { album ->
                viewModel.loadGrouping(album)
            }
        } else {

        }
    }
}

@Composable
fun GroupingList(
    items: SnapshotStateList<Album>,
    directoryIcon: Bitmap,
    multipleChoiceList: SnapshotStateList<String>,
    multipleChoiceMode: MutableState<Boolean>,
    application: MediaApplication,
    modifier: Modifier = Modifier,
    expand: (Boolean) -> Unit,
    onClickGrouping: (Album) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var soleTextFieldEnable: MutableState<Boolean>? = null
    val saveGroupingName: (TextFieldValue, Album) -> Unit = { textFieldValueState, item ->
        scope.launch(Dispatchers.IO) {
            item.name = textFieldValueState.text.drop(1)
            application.mediaDatabase.albumDao.update(item)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
    ) {
        items(items.size) { index ->
            items[index].let { item ->
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier
                        .padding(end = TinyPadding, top = TinyPadding)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    multipleChoiceMode.value = true
                                    expand(false)
                                },
                                onTap = {
                                    if (multipleChoiceMode.value) {
                                        if (multipleChoiceList.contains("${item.id}"))
                                            multipleChoiceList.remove("${item.id}")
                                        else
                                            multipleChoiceList.add("${item.id}")
                                    } else {
                                        onClickGrouping(item)
                                    }
                                }
                            )
                        }
                ) {
                    Column {
                        DisplayImage(
                            bitmap = directoryIcon,
                            context = application.baseContext,
                            modifier = Modifier.aspectRatio(1f)
                        )
                        val focusRequester = remember { FocusRequester() }
                        val textFieldEnable = remember { mutableStateOf(false) }
                        val textFieldValueState = remember { mutableStateOf(TextFieldValue(text = " ${item.name}")) }
                        LaunchedEffect(Unit) {
                            textFieldValueState.value = textFieldValueState.value.copy(
                                selection = TextRange(textFieldValueState.value.text.length)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = textFieldValueState.value,
                                enabled = textFieldEnable.value,
                                singleLine = true,
                                onValueChange = { input ->
                                    if (input.text.length in 1..10)
                                        textFieldValueState.value = input
                                },
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        textFieldEnable.value = false
                                        saveGroupingName(textFieldValueState.value, item)
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier
                                    .nestedScroll(HorizontalScrollConsumer)
                                    .padding(start = MediumPadding, end = SmallPadding)
                                    .focusRequester(focusRequester),
                            )
                            LaunchedEffect(textFieldEnable.value) {
                                if (textFieldEnable.value)
                                    focusRequester.requestFocus()
                                else
                                    saveGroupingName(textFieldValueState.value, item)
                            }
                            IconToggleButton(
                                checked = textFieldEnable.value,
                                onCheckedChange = {
                                    if (textFieldEnable.value) {
                                        textFieldEnable.value = false
                                        saveGroupingName(textFieldValueState.value, item)
                                    } else {
                                        soleTextFieldEnable?.value = false
                                        soleTextFieldEnable = textFieldEnable
                                        textFieldEnable.value = true
                                    }
                                },
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                                    .padding(start = 0.dp)
                            ) {
                                if (textFieldEnable.value)
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 0.dp)
                                    )
                                else
                                    Icon(
                                        Icons.Filled.Create,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 0.dp)
                                    )
                            }
                        }
                    }
                    if (multipleChoiceMode.value) {
                        val checked = multipleChoiceList.contains("${item.id}")
                        IconToggleButton(
                            checked = checked,
                            colors = IconButtonDefaults.iconToggleButtonColors(
                                containerColor = Color(0x80808080), // 选中时图标的颜色
                                contentColor = Color.White,              // 未选中时图标的颜色
                                checkedContainerColor = Color.White,    // 选中时背景颜色
                                checkedContentColor = Color.DarkGray   // 未选中时背景颜色
                            ),
                            onCheckedChange = {
                                if (checked)
                                    multipleChoiceList.remove("${item.id}")
                                else
                                    multipleChoiceList.add("${item.id}")
                            },
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp)
                                .offset(x = (-10).dp, y = 10.dp)
                        ) {
                            if (checked) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "选中",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "未选中",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    var invisibleStatusBar by remember { mutableStateOf(false) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }.collect {
            invisibleStatusBar = gridState.firstVisibleItemIndex > 0
        }
    }
    var lifecycle by remember {
        mutableStateOf(true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    lifecycle = true
                }

                Lifecycle.Event.ON_PAUSE -> {
                    lifecycle = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (lifecycle && !multipleChoiceMode.value) {
        expand(!invisibleStatusBar)
    }
}

@Composable
private fun TopBar(
    viewModel: GroupingScreenViewModel,
    multipleChoiceMode: MutableState<Boolean>,
    multipleChoiceList: SnapshotStateList<String>,
    selectAll: MutableState<Boolean>,
    showPopup: MutableState<Boolean>,
    isPopupVisible: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 5.dp, bottom = 5.dp)
            .fillMaxWidth()
    ) {
        if (!multipleChoiceMode.value) {
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = viewModel.directoryName.value,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.top_bar_directory_info_2,
                        viewModel.directoryNum.intValue,
                        viewModel.typeName.value,
                        viewModel.photosNum.intValue
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            //分组根目录
            if (viewModel.currentDirectoryInfo.value.first == -1L) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                viewModel.application.mediaDatabase.runInTransaction {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val deleteList = multipleChoiceList.map { it.toLong() }
                                        viewModel.application.mediaDatabase.albumDao.deleteByIds(deleteList)
                                        viewModel.application.mediaDatabase.albumMediaFileCrossRefDao.deleteByIds(deleteList)
                                        viewModel.groupingList.removeIf { album ->
                                            deleteList.contains(album.id)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "")
                    }
                }
            }
        }

        /*else {
            Box(contentAlignment = Alignment.BottomCenter) {
                IconToggleButton(
                    checked = selectAll.value,
                    onCheckedChange = {
                        selectAll.value = !selectAll.value
                        if (selectAll.value) {
                            val all = viewModel.localMediaFileService.allData
                            val filter = multipleChoiceList.toSet()
                            all.map { item ->
                                if (!filter.contains("${item.id}_${item.type.value}")) {
                                    multipleChoiceList.add("${item.id}_${item.type.value}")
                                }
                            }
                        } else {
                            multipleChoiceList.clear()
                        }
                    },
                    modifier = Modifier.zIndex(2f)
                ) {
                    if (selectAll.value) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "选中", tint = Color.DarkGray)
                    } else {
                        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = "未选中", tint = Color.LightGray)
                    }
                }
                Text(
                    stringResource(R.string.check_all),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                stringResource(R.string.check_num, multipleChoiceList.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Box(contentAlignment = Alignment.BottomCenter) {
                IconButton(onClick = {
                    showPopup.value = true
                    isPopupVisible.value = true
                }) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Check),
                        contentDescription = null
                    )
                }
                Text(
                    stringResource(R.string.top_bar_add_grouping),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }*/
    }
}

private val HorizontalScrollConsumer = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource) = available.copy(y = 0f)
    override suspend fun onPreFling(available: Velocity) = available.copy(y = 0f)
}
