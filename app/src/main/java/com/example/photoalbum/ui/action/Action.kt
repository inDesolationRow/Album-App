package com.example.photoalbum.ui.action

sealed class UserAction {
    data class ScanAction(val end: Boolean) : UserAction()
    data class ExpandStatusBarAction(val expand: Boolean) : UserAction()
    data object NoneAction : UserAction()
}