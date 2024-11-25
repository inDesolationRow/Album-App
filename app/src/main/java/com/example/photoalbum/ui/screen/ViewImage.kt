package com.example.photoalbum.ui.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.photoalbum.ui.action.UserAction

@Composable
fun ViewImage(viewModel: ViewImageViewModel, modifier: Modifier) {
    viewModel.expandBar(false)
    /*Scaffold {

    }*/
}