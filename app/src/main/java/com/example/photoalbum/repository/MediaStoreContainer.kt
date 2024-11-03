package com.example.photoalbum.repository

import android.content.Context
import com.example.photoalbum.MediaApplication

interface MediaStoreContainer {

    var imageStoreRepository: MediaStoreRepository

}

class MediaStoreContainerImpl(context: Context) : MediaStoreContainer{

    override var imageStoreRepository: MediaStoreRepository = ImageStoreImageRepositoryImpl(context)

}