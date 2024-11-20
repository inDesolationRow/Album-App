package com.example.photoalbum.ui.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.R
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.model.MediaListDialogEntity
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import com.example.photoalbum.ui.theme.LargePadding
import kotlinx.coroutines.launch

@Composable
fun AddLocalNetScreen(viewModel: AddLocalNetScreenViewModel, modifier: Modifier = Modifier){

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

    AddLocalNetStorage(onClick = { ip, user, pwd, shared ->
        viewModel.showDialog =
            MediaListDialogEntity(MediaListDialog.LOCAL_NET_CONNECTING, true)
        viewModel.viewModelScope.launch {
            val result = viewModel.connectSmb(ip, user, pwd, shared)
            result(result = result, viewModel = viewModel) {
                viewModel.addLocalNetStorageInfo(
                    LocalNetStorageInfo(
                        displayName = "$user://$shared",
                        ip = ip,
                        user = user,
                        password = pwd ?: "",
                        shared = shared
                    )
                )
                viewModel.showDialog =
                    MediaListDialogEntity(
                        mediaListDialog = MediaListDialog.LOCAL_NET_ADD_SUCCESS,
                        isShow = true,
                    )
            }
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
                is ConnectResult.Success -> {

                }
            }
        }
    }, modifier = modifier)
}

@Composable
fun AddLocalNetStorage(
    onClick: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        var ip by remember { mutableStateOf("") }
        val ipSource = remember { MutableInteractionSource() }
        val ipFocused = ipSource.collectIsFocusedAsState()

        var user by remember { mutableStateOf("") }
        val userSource = remember { MutableInteractionSource() }
        val userFocused = userSource.collectIsFocusedAsState()

        var pwd by remember { mutableStateOf("") }
        val pwdSource = remember { MutableInteractionSource() }
        val pwdFocused = pwdSource.collectIsFocusedAsState()

        var shared by remember { mutableStateOf("") }
        val sharedSource = remember { MutableInteractionSource() }
        val sharedFocused = sharedSource.collectIsFocusedAsState()

        OutlinedTextField(
            value = ip,
            onValueChange = { newValue -> ip = newValue },
            label = {
                if (ipFocused.value) Text(text = stringResource(R.string.ip)) else Text(
                    text = stringResource(
                        R.string.ip_input
                    )
                )
            },
            interactionSource = ipSource,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = user,
            onValueChange = { newValue -> user = newValue },
            label = {
                if (userFocused.value) Text(text = stringResource(R.string.user)) else Text(
                    text = stringResource(R.string.user_input)
                )
            },
            interactionSource = userSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = pwd,
            onValueChange = { newValue -> pwd = newValue },
            label = {
                if (pwdFocused.value) Text(text = stringResource(R.string.pwd)) else Text(
                    text = stringResource(
                        R.string.pwd_input
                    )
                )
            },
            interactionSource = pwdSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        OutlinedTextField(
            value = shared,
            onValueChange = { newValue -> shared = newValue },
            label = {
                if (sharedFocused.value) Text(text = stringResource(R.string.shared)) else Text(
                    text = stringResource(R.string.shared_input)
                )
            },
            interactionSource = sharedSource,
            modifier = Modifier.padding(top = LargePadding)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LargePadding)
        ) {
            TextButton(onClick = { onClick(ip.trim(), user.trim(), pwd.trim(), shared.trim()) }) {
                Text(
                    text = stringResource(R.string.submit),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun result(
    result: ConnectResult,
    viewModel: AddLocalNetScreenViewModel,
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