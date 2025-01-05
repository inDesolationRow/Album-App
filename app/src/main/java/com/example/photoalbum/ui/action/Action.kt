package com.example.photoalbum.ui.action

sealed class UserAction {
    data class ScanAction(val end: Boolean) : UserAction()
    data class ExpandStatusBarAction(val expand: Boolean, val recomposeKey: Int = 0, val duration: Int = 400) : UserAction()
    data class OpenImage(val directory: Any, val imageId: Long, val localNetId: Int? = null) : UserAction()
    data object Back : UserAction()
    data object NoneAction : UserAction()
}

sealed class ConnectResult {
    data object Success : ConnectResult()
    data class IPError(val message: String) : ConnectResult()
    data class AuthenticateError(val message: String) : ConnectResult()
    data class SharedError(val message: String) : ConnectResult()
    data class ConnectError(val message: String) : ConnectResult()
}