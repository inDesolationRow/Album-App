package com.example.photoalbum

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.photoalbum.database.AlbumDao
import com.example.photoalbum.database.MediaDatabase
import com.example.photoalbum.database.MediaFileDao
import com.example.photoalbum.database.PhotoDao
import com.example.photoalbum.enum.AlbumType
import com.example.photoalbum.database.model.Album
import com.example.photoalbum.database.model.PhotoInfo
import com.example.photoalbum.repository.ImageStoreImageRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import android.Manifest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SQLTest {

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_IMAGES)

    private lateinit var db: MediaDatabase

    private lateinit var albumDao: AlbumDao

    private lateinit var photoDao: PhotoDao

    private lateinit var mediaDao: MediaFileDao

    private lateinit var imageStoreImageRepositoryImpl: ImageStoreImageRepositoryImpl

    @Before
    @Throws(IOException::class)
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        imageStoreImageRepositoryImpl = ImageStoreImageRepositoryImpl(context)

        val db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()

        albumDao = db.albumDao
        photoDao = db.photoDao
        mediaDao = db.mediaFileDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb(){
        try {
            db.close()
        }catch (e: Exception){
            print(e.message)
        }
    }

    @Test
    fun testSQL() = runBlocking {
        albumDao.insert(Album(type = AlbumType.MAIN_ALBUM.name))
        val albumList = albumDao.queryMainAlbum()
        assertEquals(albumList?.size, 1)

        photoDao.insert(PhotoInfo(albumId = 1))
        val photoList = photoDao.queryByAlbumId(1)
        assertEquals(photoList?.size, 1)

        photoDao.delete(id = 1)
        val photoList2 = photoDao.queryByAlbumId(1)
        val empty = photoList2.isNullOrEmpty()
        assertEquals(empty, true)

        val query = albumDao.queryById(id = 1, false)
        val isNull: Boolean = query?.equals(null) ?: true
        assertEquals(isNull, false)

        albumDao.delete(query?.id ?: 0)
        val albumList2 = albumDao.queryMainAlbum()
        assertEquals(albumList2?.size, 0)

    }

    @Test
    fun testMediaFile() = runBlocking {
       
    }

}