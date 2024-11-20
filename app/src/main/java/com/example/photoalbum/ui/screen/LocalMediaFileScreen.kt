package com.example.photoalbum.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LocalMediaFileScreen(
    viewModel: LocalMediaFileScreenViewModel,
    recomposeKey: MutableStateFlow<Int>,
    modifier: Modifier = Modifier
) {

    BackHandler(viewModel.levelStack.size >= 2) {
        viewModel.localMediaFileStackBack()
    }

    val recomposeKeyState = recomposeKey.collectAsState()
/*
    LaunchedEffect(recomposeKeyState) {
        viewModel.updateForUpdateKey(viewModel.currentDirectoryId.value, recomposeKeyState.value)
    }
*/

    viewModel.localMediaFileFlow?.let {
        val items = it.value.collectAsLazyPagingItems()
        MediaList(
            itemList = items,
            nullPreviewIcon = viewModel.notPreviewIcon,
            directoryIcon = viewModel.directoryIcon,
            gridColumn = viewModel.settings.gridColumnNumState.intValue,
            clickId = { id, type ->
                if (type == ItemType.DIRECTORY) viewModel.currentDirectoryId.value =
                    id
            },
            expand = { expand ->
                viewModel.userAction.value = UserAction.ExpandStatusBarAction(expand)
            },
            modifier = modifier.fillMaxHeight()
        )
    }

}