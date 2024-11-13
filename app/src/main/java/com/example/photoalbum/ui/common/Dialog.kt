package com.example.photoalbum.ui.common

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photoalbum.R


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
fun ProgressDialog(message: Int) {
    Dialog(onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false) ) {
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
