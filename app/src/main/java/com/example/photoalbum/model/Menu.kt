package com.example.photoalbum.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Menu(
    val id: Int,
    val displayName: String,
    val usable: Boolean,
    val icon: ImageVector
)