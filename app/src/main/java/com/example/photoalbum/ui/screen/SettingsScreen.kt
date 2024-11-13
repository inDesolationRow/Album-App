package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.photoalbum.R
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.enums.SettingsDialog
import com.example.photoalbum.model.SettingsDialogEntity
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import com.example.photoalbum.ui.theme.PhotoAlbumTheme

@Composable
fun SettingsScreen(viewModel: SettingsScreenViewModel, modifier: Modifier) {
    if (viewModel.showDialog.isShow) {
        ShowDialog(viewModel, viewModel.showDialog.settingsDialog)
    }
    when (viewModel.scanResult) {
        ScanResult.SUCCESS -> {viewModel.showDialog =
            SettingsDialogEntity(SettingsDialog.SCAN_SUCCESS, true)
            viewModel.scanResult = ScanResult.NONE
        }

        ScanResult.FAILED -> viewModel.showDialog =
            SettingsDialogEntity(SettingsDialog.SCAN_FAILED, true)

        ScanResult.SCANNING -> viewModel.showDialog =
            SettingsDialogEntity(SettingsDialog.SCANNING, true)

        ScanResult.NONE -> {}
    }
    Box(modifier = modifier) {
        Column {
            Button(onClick = {
                if (viewModel.checkPermissions()) {
                    viewModel.showDialog = SettingsDialogEntity(
                        settingsDialog = SettingsDialog.SCAN_LOCAL_STORAGE,
                        isShow = true
                    )
                } else {
                    viewModel.showDialog = SettingsDialogEntity(
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
                onDismiss = { viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false) },
                clickConfirm = {
                    viewModel.scanResult = ScanResult.SCANNING
                    viewModel.scanLocalStorage()
                },
                clickCancel = { viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false) })
        }

        SettingsDialog.GO_TO_APP_SETTINGS -> {
            val activity = LocalContext.current as Activity
            MessageDialog(
                messageRes = R.string.please_go_to_settings,
                onDismiss = {
                    viewModel.application.appPermission.goToSettings(activity)
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                },
                clickConfirm = {
                    viewModel.application.appPermission.goToSettings(activity)
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                })
        }

        SettingsDialog.SCANNING -> {
            ProgressDialog(R.string.scanning)
        }

        SettingsDialog.SCAN_SUCCESS -> {
            MessageDialog(
                messageRes = R.string.scan_success,
                onDismiss = {
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                },
                clickConfirm = {
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                })
        }

        SettingsDialog.SCAN_FAILED -> {
            MessageDialog(
                messageRes = R.string.scan_failed,
                onDismiss = {
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                },
                clickConfirm = {
                    viewModel.showDialog = SettingsDialogEntity(SettingsDialog.NONE, false)
                    viewModel.scanResult = ScanResult.NONE
                })
        }

        SettingsDialog.NONE -> {}
    }
}

@Preview
@Composable
fun DialogPreview() {
    PhotoAlbumTheme {
        ProgressDialog(R.string.scanning)
    }
}
