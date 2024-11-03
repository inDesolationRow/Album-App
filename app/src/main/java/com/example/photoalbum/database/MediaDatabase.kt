package com.example.photoalbum.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photoalbum.model.Album
import com.example.photoalbum.model.Directory
import com.example.photoalbum.model.DirectoryMediaFileCrossRef
import com.example.photoalbum.model.MediaFile
import com.example.photoalbum.model.PhotoInfo

@Database(entities = [Album::class,
    PhotoInfo::class,
    MediaFile::class,
    Directory::class,
    DirectoryMediaFileCrossRef::class], version = 1, exportSchema = false)
abstract class MediaDatabase: RoomDatabase() {

    abstract val albumDao: AlbumDao

    abstract val photoDao: PhotoDao

    abstract val mediaFileDao: MediaFileDao

    abstract val directoryDao: DirectoryDao

    abstract val directoryMediaFileCrossRefDao: DirectoryMediaFileCrossRefDao

    companion object {

        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getInstance(context: Context): MediaDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        MediaDatabase::class.java,
                        "media_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}