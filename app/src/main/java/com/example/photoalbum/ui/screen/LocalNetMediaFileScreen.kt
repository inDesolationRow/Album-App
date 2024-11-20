package com.example.photoalbum.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.R
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LocalNetMediaFileScreen(
    viewModel: LocalNetMediaFileScreenViewModel,
    modifier: Modifier = Modifier,
    localNetId: Int,
    recomposeKey: MutableStateFlow<Int>
) {
    BackHandler(viewModel.smbClient.pathStackSize() >= 2) {
        val path = viewModel.smbClient.back()
        if (viewModel.smbClient.isConnect()) {
            viewModel.initLocalNetMediaFilePaging(path)
        } else {
            viewModel.smbClient.rollback()
            viewModel.showDialog = MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                isShow = true
            )
        }
    }

    if (viewModel.showDialog.isShow) {
        when (viewModel.showDialog.mediaListDialog) {
            MediaListDialog.LOCAL_NET_IP_ERROR -> MessageDialog(
                messageRes = R.string.connect_failed_ip_error,
                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

            MediaListDialog.LOCAL_NET_USER_ERROR -> MessageDialog(
                messageRes = R.string.connect_failed_user_error,
                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

            MediaListDialog.LOCAL_NET_SHARED_ERROR -> MessageDialog(
                messageRes = R.string.connect_failed_shared_error,
                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

            MediaListDialog.LOCAL_NET_ADD_SUCCESS -> MessageDialog(
                messageRes = R.string.connect_success,
                clickConfirm = {
                    viewModel.showDialog.onClick()
                    viewModel.showDialog = MediaListDialogEntity()
                },
                onDismiss = {
                    viewModel.showDialog.onClick()
                    viewModel.showDialog = MediaListDialogEntity()
                })

            MediaListDialog.LOCAL_NET_CONNECTING ->
                ProgressDialog(R.string.connecting)

            MediaListDialog.LOCAL_NET_OFFLINE -> MessageDialog(
                messageRes = R.string.local_net_offline,
                clickConfirm = { viewModel.showDialog = MediaListDialogEntity() },
                onDismiss = { viewModel.showDialog = MediaListDialogEntity() })

            MediaListDialog.NONE -> {}
        }
    }

    val recomposeKeyState = recomposeKey.collectAsState()
    LaunchedEffect(localNetId, recomposeKeyState.value) {
        val result = viewModel.connectSmb(localNetId)
        result(result = result, viewModel = viewModel) {
            viewModel.initLocalNetMediaFilePaging()
        }
    }

    val items = viewModel.localNetMediaFileFlow.value.collectAsLazyPagingItems()
    MediaList(
        itemList = items,
        nullPreviewIcon = viewModel.notPreviewIcon,
        directoryIcon = viewModel.directoryIcon,
        gridColumn = viewModel.settings.gridColumnNumState.intValue,
        expand = {
            viewModel.userAction.value = UserAction.ExpandStatusBarAction(it)
        },
        clickString = { name, type ->
            if (viewModel.isConnect()) {
                if (type == ItemType.DIRECTORY) viewModel.initLocalNetMediaFilePaging(
                    name
                )
            } else {
                viewModel.showDialog = MediaListDialogEntity(
                    mediaListDialog = MediaListDialog.LOCAL_NET_OFFLINE,
                    isShow = true
                )
            }
        },
        modifier = modifier.fillMaxHeight()
    )
}

private fun result(
    result: ConnectResult,
    viewModel: LocalNetMediaFileScreenViewModel,
    onSuccess: () -> Unit
) {
    when (result) {
        is ConnectResult.IPError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_IP_ERROR,
                isShow = true
            )

        is ConnectResult.AuthenticateError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_USER_ERROR,
                isShow = true
            )


        is ConnectResult.SharedError -> viewModel.showDialog =
            MediaListDialogEntity(
                mediaListDialog = MediaListDialog.LOCAL_NET_SHARED_ERROR,
                isShow = true
            )


        is ConnectResult.ConnectError -> {}
        is ConnectResult.Success -> onSuccess()
    }
}