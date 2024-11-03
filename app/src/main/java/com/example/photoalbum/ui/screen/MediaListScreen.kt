package com.example.photoalbum.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photoalbum.ui.theme.PhotoAlbumTheme

@Composable
fun MediaListScreen(viewModel: MediaListScreenViewModel, modifier: Modifier = Modifier) {
   Box(modifier = modifier){
       Text("media list")
   }
}

@Preview
@Composable
fun MediaListScreenPreview(){
    PhotoAlbumTheme {
        val mediaListScreenViewModel = viewModel<MediaListScreenViewModel> (factory = BaseViewModel.Factory(modelClass = MediaListScreenViewModel::class.java) { MediaListScreenViewModel(it) })
        MediaListScreen(mediaListScreenViewModel)
    }
}
