package com.example.photoalbum.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photoalbum.R
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.ui.theme.LargePadding


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
fun EditLocalNetStorageDialog(
    localNetStorageInfo: LocalNetStorageInfo,
    onSubmit: (localNetStorageInfo: LocalNetStorageInfo) -> Unit
) {
    Dialog(onDismissRequest = {}) {
        ElevatedCard(modifier = Modifier) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(15.dp)
            ) {
                var ip by remember { mutableStateOf(localNetStorageInfo.ip) }
                val ipSource = remember { MutableInteractionSource() }
                val ipFocused = ipSource.collectIsFocusedAsState()

                var user by remember { mutableStateOf(localNetStorageInfo.user) }
                val userSource = remember { MutableInteractionSource() }
                val userFocused = userSource.collectIsFocusedAsState()

                var pwd by remember { mutableStateOf(localNetStorageInfo.password) }
                val pwdSource = remember { MutableInteractionSource() }
                val pwdFocused = pwdSource.collectIsFocusedAsState()

                var shared by remember { mutableStateOf(localNetStorageInfo.shared) }
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
                    TextButton(
                        onClick = {
                            onSubmit(
                                LocalNetStorageInfo(
                                    id = localNetStorageInfo.id,
                                    ip = ip,
                                    user = user,
                                    password = pwd,
                                    shared = shared,
                                    displayName = "$user://$shared"
                                )
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.submit),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressDialog(message: Int) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        ElevatedCard {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.padding(25.dp)) {
                Text(
                    text = stringResource(message),
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
