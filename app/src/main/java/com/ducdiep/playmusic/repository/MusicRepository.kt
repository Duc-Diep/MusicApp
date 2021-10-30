package com.ducdiep.playmusic.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.config.bitmapDefault
import com.ducdiep.playmusic.helpers.SqlHelper
import com.ducdiep.playmusic.models.getgenres.ResponseInfor
import com.ducdiep.playmusic.models.search.ResponseSearch
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.ResponseRecommend
import com.ducdiep.playmusic.models.songresponse.ResponseTopSong
import kotlinx.android.synthetic.main.activity_play_music.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MusicRepository {
    //lay ds nhac offline
    @SuppressLint("Range")
    fun getListOffline(context: Context): ArrayList<SongOffline> {

        var listSong = ArrayList<SongOffline>()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        try {
            bitmapDefault =
                BitmapFactory.decodeResource(context.resources, R.drawable.musical_default)
        } catch (ex: Exception) {
            Toast.makeText(context, "Ảnh quá nặng vượt mức cho phép", Toast.LENGTH_SHORT).show()
        }
        val contentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? =
            contentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                try {
                    val name: String =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val artist: String =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val url: String =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val duration: String =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))


                    var bitmapPicture: Bitmap? = null
                    var genres = ""
                    try {
                        var media = MediaMetadataRetriever()
                        media.setDataSource(url)
                        var byteArray: ByteArray? = media.embeddedPicture
                        bitmapPicture = try {
                            BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
                        } catch (ex: Exception) {
                            bitmapDefault
                        }
                        genres = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                            .toString()

                    } catch (ex: Exception) {

                    }
                    var songOffline: SongOffline
                    if (bitmapPicture == null) {
                        songOffline =
                            SongOffline(name, artist, duration.toInt(), bitmapDefault, url, genres)
                    } else {
                        songOffline = SongOffline(
                            name,
                            artist,
                            duration.toInt(),
                            bitmapPicture,
                            url,
                            genres
                        )
                    }
                    listSong.add(songOffline)
                } catch (ex: Exception) {
//                Toast.makeText(context, "Tải bài hát thất bại", Toast.LENGTH_SHORT).show()
                }

            } while (cursor.moveToNext())
        }
        cursor!!.close()
        }

        return listSong
    }

    //lay top 100
    fun getTopSong(onResult: (isSuccess: Boolean, response: ResponseTopSong?) -> Unit) {
        RetrofitInstance.getInstance()?.getTopSong()?.enqueue(object : Callback<ResponseTopSong> {
            override fun onResponse(
                call: Call<ResponseTopSong>,
                response: Response<ResponseTopSong>
            ) {
                if (response.isSuccessful && response != null) {
                    onResult(true, response.body()!!)
                } else {
                    onResult(false, null)

                }
            }

            override fun onFailure(call: Call<ResponseTopSong>, t: Throwable) {
                onResult(false, null)
            }

        })
    }

    // lay tu search
    fun getSongSearch(
        key: String,
        onResult: (isSuccess: Boolean, response: ResponseSearch?) -> Unit
    ) {
        RetrofitInstance.getInstanceSearch()?.getSearch("artist,song,key,code", 500, key)
            ?.enqueue(object : Callback<ResponseSearch> {
                override fun onResponse(
                    call: Call<ResponseSearch>,
                    response: Response<ResponseSearch>
                ) {
                    if (response.isSuccessful && response != null) {
                        onResult(true, response.body()!!)
                    } else {
                        onResult(false, null)

                    }
                }

                override fun onFailure(call: Call<ResponseSearch>, t: Throwable) {
                    onResult(false, null)
                }

            })
    }

    //lay bai lien quan
    fun getRecommendSong(
        type: String,
        id: String,
        onResult: (isSuccess: Boolean, response: ResponseRecommend?) -> Unit
    ) {
        RetrofitInstance.getInstance()?.getRecommend(type, id)?.enqueue(object :
            Callback<ResponseRecommend> {
            override fun onResponse(
                call: Call<ResponseRecommend>,
                response: Response<ResponseRecommend>
            ) {
                if (response.isSuccessful && response != null) {
                    onResult(true, response.body()!!)
                } else {
                    onResult(false, null)

                }
            }

            override fun onFailure(call: Call<ResponseRecommend>, t: Throwable) {
                onResult(false, null)
            }

        })
    }

    //lay thong tin 1 bai
    fun getASongInfor(
        type: String,
        id: String,
        onResult: (isSuccess: Boolean, response: ResponseInfor?) -> Unit
    ) {
        RetrofitInstance.getInstance()?.getSongInfor(type, id)
            ?.enqueue(object : Callback<ResponseInfor> {
                override fun onResponse(
                    call: Call<ResponseInfor>,
                    response: Response<ResponseInfor>
                ) {
                    if (response.isSuccessful && response != null) {
                        onResult(true, response.body()!!)
                    } else {
                        onResult(false, null)

                    }
                }

                override fun onFailure(call: Call<ResponseInfor>, t: Throwable) {
                    onResult(false, null)
                }

            })
    }
    fun getFavouriteSong(context: Context):List<SongFavourite>{
        var sqlHelper = SqlHelper(context)
        return sqlHelper.getAllSong()
    }


    companion object {
        private var INSTANCE: MusicRepository? = null
        fun getInstance() = INSTANCE
            ?: MusicRepository().also {
                INSTANCE = it
            }
    }
}