package com.example.photoalbum.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FavoriteScreen(viewModel: FavoriteScreenViewModel, modifier: Modifier = Modifier) {
    Column (modifier = modifier){
        Text(text = "favorite")
        Button(onClick = { viewModel.expand.value = !viewModel.expand.value}) {
            Text(text = "点我")
        }
    }
}