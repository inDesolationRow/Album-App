package com.example.photoalbum.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DirectoryWithMediaFile (
    @Embedded val directory: Directory,
    @Relation(
        parentColumn = "directory_id",
        entityColumn = "media_file_id",
        associateBy = Junction(DirectoryMediaFileCrossRef::class)
    ) val mediaFileList: List<MediaFile>
)