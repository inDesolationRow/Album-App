package com.example.photoalbum.ui.screen

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.R
import com.example.photoalbum.data.model.Album
import com.example.photoalbum.data.model.AlbumMediaFileCrossRef
import com.example.photoalbum.ui.common.DisplayImage
import com.example.photoalbum.ui.theme.SmallPadding
import com.example.photoalbum.ui.theme.TinyPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlbumGrouping(
    showPopup: MutableState<Boolean>,
    isPopupVisible: MutableState<Boolean>,
    size: DpSize,
    application: MediaApplication,
    directoryIcon: Bitmap,
    groupingList: List<String>,
    onAddGrouping: ()->Unit
) {
    val width = remember { (size.width.value * 0.9f).dp }
    val height = remember { (size.height.value * 0.6f).dp }
    val density = LocalDensity.current

    //加载数据
    val scope = rememberCoroutineScope()
    val items: SnapshotStateList<Album> = remember { mutableStateListOf() }
    var queryJob by remember { mutableStateOf<Job?>(null) }
    var addJob by remember { mutableStateOf<Job?>(null) }
    val gridState = rememberLazyGridState()

    //控制显示
    val transition = updateTransition(targetState = showPopup.value, label = "popup")
    val alpha by transition.animateFloat(
        label = "popupOffset",
        transitionSpec = { tween(durationMillis = 500) }
    ) { state -> if (state) 1f else 0f }

    // 监听 showPopup 的变化，管理 Popup 显示状态
    LaunchedEffect(showPopup.value) {
        queryJob?.cancelAndJoin()
        if (showPopup.value) {
            queryJob = scope.launch(Dispatchers.IO) {
                val result = application.mediaDatabase.albumDao.queryByParentId(-1)
                if (result != null) items.addAll(result)
            }
            isPopupVisible.value = true
        } else {
            delay(500)
            isPopupVisible.value = false
            queryJob?.cancel()
            println("摧毁popup")
        }
    }

    val choiceChecked: MutableState<Pair<Int, MutableState<Boolean>>?> = remember { mutableStateOf(null) }
    if (isPopupVisible.value) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = {
                showPopup.value = false
            },
            properties = PopupProperties(focusable = true)
        ) {
            Column(
                modifier = Modifier
                    .size(width, height)
                    .graphicsLayer(alpha = alpha)
                    .background(Color(0xE6FFFFFF), shape = RoundedCornerShape(16.dp))
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(items.size) { index ->
                        items[index].let { item ->
                            val checked = remember { mutableStateOf(false) }
                            Box(
                                contentAlignment = Alignment.TopEnd,
                                modifier = Modifier
                                    .padding(end = TinyPadding, top = TinyPadding)
                                    .clickable {
                                        if (checked.value) {
                                            checked.value = false
                                        } else {
                                            choiceChecked.value?.second?.value = false
                                            choiceChecked.value = index to checked
                                            checked.value = true
                                        }
                                    }
                            ) {
                                Column {
                                    DisplayImage(
                                        bitmap = directoryIcon,
                                        context = application.baseContext,
                                        modifier = Modifier.aspectRatio(1f)
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = SmallPadding)
                                    )
                                }

                                IconToggleButton(
                                    checked = checked.value,
                                    colors = IconButtonDefaults.iconToggleButtonColors(
                                        containerColor = Color(0x80808080), // 选中时图标的颜色
                                        contentColor = Color.White,              // 未选中时图标的颜色
                                        checkedContainerColor = Color.White,    // 选中时背景颜色
                                        checkedContentColor = Color.DarkGray   // 未选中时背景颜色
                                    ),
                                    onCheckedChange = {
                                        if (checked.value) {
                                            checked.value = false
                                        } else {
                                            choiceChecked.value?.second?.value = false
                                            choiceChecked.value = index to checked
                                            checked.value = true
                                        }
                                    },
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                        .offset(x = (-10).dp, y = 10.dp)
                                ) {
                                    if (checked.value) {
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
                val driver = remember { mutableStateOf(0.dp) }
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .onGloballyPositioned { layout ->
                            with(density) {
                                driver.value = layout.size.height.toDp()
                            }
                        }
                        .background(Color(0xE6D3D3D3), shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            addJob?.join()
                            val index = choiceChecked.value?.first
                            index?.let { n ->
                                addJob = scope.launch {
                                    val list: MutableList<AlbumMediaFileCrossRef> = mutableListOf()
                                    val groupingId = items[n].id
                                    groupingList.forEach { fileId ->
                                        val info = fileId.split("_")
                                        list.add(
                                            AlbumMediaFileCrossRef(
                                                id = groupingId,
                                                mediaFileId = info.first().trim().toLong(),
                                                type = info.last().trim().toInt()
                                            )
                                        )
                                    }
                                    application.mediaDatabase.albumMediaFileCrossRefDao.insert(list)
                                }
                                showPopup.value = false
                                onAddGrouping()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.popup_select_grouping))
                    }
                    Text(
                        "", modifier = Modifier
                            .width(1.dp)
                            .height(driver.value)
                            .background(Color(0xE6FFFFFF))
                    )
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            addJob?.join()
                            addJob = scope.launch {
                                val groupingNum = items.size
                                val album = Album(name = "分组${groupingNum + 1}")
                                val id = application.mediaDatabase.albumDao.insert(album)
                                album.id = id
                                items.add(album)
                            }
                        }
                    }) {
                        Text(stringResource(R.string.popup_add_grouping))
                    }
                }
            }
        }
    }
}
