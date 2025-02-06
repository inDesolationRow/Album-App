package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoalbum.R
import com.example.photoalbum.enums.ScanMode
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.enums.SettingsDialog
import com.example.photoalbum.model.SettingsDialogEntity
import com.example.photoalbum.ui.common.MessageDialog
import com.example.photoalbum.ui.common.ProgressDialog
import com.example.photoalbum.ui.theme.MediumPadding
import com.example.photoalbum.ui.theme.SmallPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel,
    modifier: Modifier,
) {
    if (viewModel.showDialog.isShow) {
        ShowDialog(
            viewModel,
            viewModel.showDialog.settingsDialog,
            viewModel.showDialog.scanMode
        )
    }
    when (viewModel.scanResult) {

        ScanResult.SCANNING -> viewModel.showDialog =
            SettingsDialogEntity(SettingsDialog.SCANNING, true)


        ScanResult.SUCCESS -> {
            viewModel.showDialog =
                SettingsDialogEntity(SettingsDialog.SCAN_SUCCESS, true)
            viewModel.scanResult = ScanResult.NONE
        }

        ScanResult.FAILED -> viewModel.showDialog =
            SettingsDialogEntity(SettingsDialog.SCAN_FAILED, true)

        ScanResult.NONE -> {}
    }

    val mutex = Mutex()
    SettingsList(viewModel = viewModel, mutex = mutex, modifier)
}

@Composable
fun ShowDialog(
    viewModel: SettingsScreenViewModel,
    settingsDialog: SettingsDialog,
    scanMode: ScanMode,
) {
    when (settingsDialog) {

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
            LaunchedEffect(Unit) {
                println("开始扫描")
                viewModel.scanLocalStorage(scanMode)
            }
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

@Composable
fun SettingsList(
    viewModel: SettingsScreenViewModel,
    mutex: Mutex,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6F6))
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.height(100.dp)
        ) {
            Text(
                "全局设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(start = MediumPadding)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFFFCFCFC))
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                title = stringResource(R.string.scan_mode_1),
                label = stringResource(R.string.scan_mode_1_label),
                onClick = {
                    if (viewModel.checkPermissions()) {
                        println("发送指令扫描")
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.SCANNING,
                            isShow = true,
                            scanMode = ScanMode.MODE_1
                        )
                    } else {
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.GO_TO_APP_SETTINGS,
                            isShow = true
                        )
                    }
                }
            )
            SettingItem(
                title = stringResource(R.string.scan_mode_2),
                label = stringResource(R.string.scan_mode_2_label),
                onClick = {
                    if (viewModel.checkPermissions()) {
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.SCANNING,
                            isShow = true,
                            scanMode = ScanMode.MODE_2
                        )
                    } else {
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.GO_TO_APP_SETTINGS,
                            isShow = true
                        )
                    }
                }
            )
            SettingItem(
                title = stringResource(R.string.scan_mode_3),
                label = stringResource(R.string.scan_mode_3_label),
                onClick = {
                    if (viewModel.checkPermissions()) {
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.SCANNING,
                            isShow = true,
                            scanMode = ScanMode.MODE_3
                        )
                    } else {
                        viewModel.showDialog = SettingsDialogEntity(
                            settingsDialog = SettingsDialog.GO_TO_APP_SETTINGS,
                            isShow = true
                        )
                    }
                }
            )
            val lowMemoryMode = remember { mutableStateOf(viewModel.settings.lowMemoryMode) }
            SettingItem(
                title = stringResource(R.string.low_memory_mode),
                label = stringResource(R.string.low_memory_mode_label),
                checked = lowMemoryMode,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        viewModel.settings.lowMemoryMode = lowMemoryMode.value
                        mutex.withLock {
                            viewModel.application.mediaDatabase.settingsDao.insertOrUpdate(settings = viewModel.settings)
                            viewModel.application.settings = viewModel.settings
                        }
                    }
                }
            )
            val highPixelThumbnail = remember { mutableStateOf(viewModel.settings.highPixelThumbnail) }
            SettingItem(
                title = stringResource(R.string.high_thumbnail_mode),
                label = stringResource(R.string.high_thumbnail_mode_label),
                checked = highPixelThumbnail,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        viewModel.settings.highPixelThumbnail = highPixelThumbnail.value
                        mutex.withLock {
                            viewModel.application.mediaDatabase.settingsDao.insertOrUpdate(settings = viewModel.settings)
                            viewModel.application.settings = viewModel.settings
                        }
                    }
                }
            )
            val smbChecked = remember { mutableStateOf(viewModel.settings.smbMode) }
            SettingItem(
                title = stringResource(R.string.smb_mode),
                label = stringResource(R.string.smb_mode_label),
                checked = smbChecked,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        viewModel.settings.smbMode = smbChecked.value
                        mutex.withLock {
                            viewModel.application.mediaDatabase.settingsDao.insertOrUpdate(settings = viewModel.settings)
                            viewModel.application.settings = viewModel.settings
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    label: String,
    checked: MutableState<Boolean>? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
                checked?.let {
                    checked.value = !it.value
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediumPadding)
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8A8A8A)
            )
        }
        if (checked != null)
            Switch(
                checked = checked.value,
                onCheckedChange = {
                    checked.value = it
                },
                modifier = Modifier.padding(end = MediumPadding)
            )
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = SmallPadding, end = SmallPadding)
            .background(Color(0xFFE9E9E9))
    )
}