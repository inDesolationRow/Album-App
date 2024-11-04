package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photoalbum.R
import com.example.photoalbum.enum.ScanResult
import com.example.photoalbum.enum.SettingsDialog
import com.example.photoalbum.model.DialogEntity
import com.example.photoalbum.ui.theme.PhotoAlbumTheme

@Composable
fun SettingsScreen(viewModel: SettingsScreenViewModel, modifier: Modifier) {
    if (viewModel.showDialog.isShow) {
        ShowDialog(viewModel, viewModel.showDialog.settingsDialog)
    }
    when (viewModel.scanResult) {
        ScanResult.SUCCESS -> {viewModel.showDialog =
            DialogEntity(SettingsDialog.SCAN_SUCCESS, true)
            viewModel.scanResult = ScanResult.NONE
        }

        ScanResult.FAILED -> viewModel.showDialog =
            DialogEntity(SettingsDialog.SCAN_FAILED, true)

        ScanResult.SCANNING -> viewModel.showDialog =
            DialogEntity(SettingsDialog.SCANNING, true)

        ScanResult.NONE -> {}
    }
    Box(modifier = modifier) {
        val activity = LocalContext.current as Activity
        Column {
            Button(onClick = {
                if (viewModel.checkPermissions()) {
                    viewModel.showDialog = DialogEntity(
                        settingsDialog = SettingsDialog.SCAN_LOCAL_STORAGE,
                        isShow = true
                    )
                } else {
                    viewModel.showDialog = DialogEntity(
                        settingsDialog = SettingsDialog.GO_TO_APP_SETTINGS,
                        isShow = true
                    )
                }
            }) {
                Text(text = stringResource(R.string.settings_scan_local_storage))
            }
            Text("Settings")
        }
    }
}

@Composable
fun ShowDialog(viewModel: SettingsScreenViewModel, settingsDialog: SettingsDialog) {
    when (settingsDialog) {
        SettingsDialog.SCAN_LOCAL_STORAGE -> {
            MessageDialog(
                messageRes = R.string.settings_scan_local_storage_message,
                onDismiss = { viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false) },
                clickConfirm = {
                    viewModel.scanResult = ScanResult.SCANNING
                    viewModel.scanLocalStorage()
                },
                clickCancel = { viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false) })
        }

        SettingsDialog.GO_TO_APP_SETTINGS -> {
            val activity = LocalContext.current as Activity
            MessageDialog(
                messageRes = R.string.please_go_to_settings,
                onDismiss = {
                    viewModel.application.appPermission.goToSettings(activity)
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                },
                clickConfirm = {
                    viewModel.application.appPermission.goToSettings(activity)
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                })
        }

        SettingsDialog.SCANNING -> {
            ProgressDialog()
        }

        SettingsDialog.SCAN_SUCCESS -> {
            MessageDialog(
                messageRes = R.string.scan_success,
                onDismiss = {
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                },
                clickConfirm = {
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                })
        }

        SettingsDialog.SCAN_FAILED -> {
            MessageDialog(
                messageRes = R.string.scan_failed,
                onDismiss = {
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                },
                clickConfirm = {
                    viewModel.showDialog = DialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                })
        }

        SettingsDialog.NONE -> {}
    }
}

@Composable
fun MessageDialog(
    messageRes: Int,
    clickConfirm: (() -> Unit)? = null,
    clickCancel: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(modifier = Modifier) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(15.dp)
            ) {
                Text(
                    text = stringResource(messageRes),
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                Row(
                    horizontalArrangement = if (clickConfirm != null && clickCancel != null) Arrangement.SpaceAround else Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (clickConfirm != null) {
                        Button(
                            onClick = clickConfirm,
                            modifier = Modifier
                        ) { Text(text = stringResource(R.string.yes)) }
                    }
                    if (clickCancel != null) {
                        Button(
                            onClick = clickCancel,
                            modifier = Modifier
                        ) { Text(text = stringResource(R.string.no)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressDialog() {
    Dialog(onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false) ) {
        ElevatedCard {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.padding(25.dp)) {
                Text(
                    text = stringResource(R.string.scanning),
                    modifier = Modifier.padding(bottom = 15.dp)
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun DialogPreview() {
    PhotoAlbumTheme {
        ProgressDialog()
    }
}
