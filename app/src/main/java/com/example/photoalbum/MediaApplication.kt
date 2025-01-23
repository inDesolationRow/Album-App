package com.example.photoalbum

import android.app.Application
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.photoalbum.data.MediaDatabase
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.repository.MediaStoreContainer
import com.example.photoalbum.repository.MediaStoreContainerImpl
import com.example.photoalbum.utils.PermissionHelper

class MediaApplication: Application(){

    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_state")

    lateinit var appPermission: PermissionHelper

    lateinit var mediaDatabase: MediaDatabase

    lateinit var mediaStoreContainer: MediaStoreContainer

    var localNetStorageInfo : LocalNetStorageInfo? = null

    var loadThumbnailBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        appPermission = PermissionHelper(applicationContext)
        mediaDatabase = MediaDatabase.getInstance(applicationContext)
        mediaStoreContainer = MediaStoreContainerImpl(applicationContext)

    }

}