package com.example.photoalbum.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photoalbum.data.dao.AlbumDao
import com.example.photoalbum.data.dao.DirectoryDao
import com.example.photoalbum.data.dao.DirectoryMediaFileCrossRefDao
import com.example.photoalbum.data.dao.LocalNetStorageInfoDao
import com.example.photoalbum.data.dao.MediaFileDao
import com.example.photoalbum.data.dao.PhotoDao
import com.example.photoalbum.data.dao.SettingsDao
import com.example.photoalbum.data.model.Album
import com.example.photoalbum.data.model.Directory
import com.example.photoalbum.data.model.DirectoryMediaFileCrossRef
import com.example.photoalbum.data.model.LocalNetStorageDirectory
import com.example.photoalbum.data.model.LocalNetStorageInfo
import com.example.photoalbum.data.model.MediaFile
import com.example.photoalbum.data.model.PhotoInfo
import com.example.photoalbum.data.model.Settings

@Database(
    entities = [Album::class,
        PhotoInfo::class,
        MediaFile::class,
        Directory::class,
        DirectoryMediaFileCrossRef::class,
        LocalNetStorageInfo::class,
        LocalNetStorageDirectory::class,
        Settings::class], version = 1, exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {

    abstract val albumDao: AlbumDao

    abstract val photoDao: PhotoDao

    abstract val mediaFileDao: MediaFileDao

    abstract val directoryDao: DirectoryDao

    abstract val directoryMediaFileCrossRefDao: DirectoryMediaFileCrossRefDao

    abstract val localNetStorageInfoDao: LocalNetStorageInfoDao

    abstract val settingsDao: SettingsDao

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