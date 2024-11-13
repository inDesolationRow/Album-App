package com.example.photoalbum.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.photoalbum.ui.action.UserAction

@Composable
fun FavoriteScreen(viewModel: FavoriteScreenViewModel, modifier: Modifier = Modifier) {
    val userAction = viewModel.userAction.collectAsState()
    when (val action = userAction.value) {
        is UserAction.ExpandStatusBarAction -> {
            viewModel.expand = action.expand
        }
        is UserAction.ScanAction -> {}
        UserAction.NoneAction -> {}
    }
    Column(modifier = modifier) {
        Text(text = "favorite")
        Button(onClick = {
            viewModel.userAction.value = UserAction.ExpandStatusBarAction(!viewModel.expand)
        }) {
            Text(text = "点我")
        }
    }
}