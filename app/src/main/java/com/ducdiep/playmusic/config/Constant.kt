package com.ducdiep.playmusic.config

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.models.songoffline.SongOffline

const val ACTION_PAUSE = 1
const val ACTION_RESUME = 2
const val ACTION_CLEAR = 3
const val ACTION_START = 4
const val ACTION_NEXT = 6
const val ACTION_PREVIOUS = 5
const val PERMISSION_REQUEST = 7
const val ACTION_RELOAD = "reload"
const val ACTION = "action"
const val IS_ONLINE = "isOnline"
const val CURRENT_POSITION = "position"
const val IMAGE = "image"
const val INDEX = "index"
const val PROGRESS = "progress"
const val ACTION_SEND_TO_ACTIVITY = "action_send_data_to_activity"
const val ACTION_SERVICE_TO_BROADCAST = "action_service_to_broadcast"
const val ACTION_TO_SERVICE = "action_broadcast_to_service"
const val URL_THUMB = "https://photo-zmp3.zadn.vn/"
const val URL_MUSIC = "http://api.mp3.zing.vn/api/streaming/"

lateinit var bitmapDefault: Bitmap

fun getAudio(context: Context): ArrayList<SongOffline> {
    var listSong = ArrayList<SongOffline>()
    try {
        bitmapDefault = BitmapFactory.decodeResource(context.resources, R.drawable.musical_default)
    }catch (ex:Exception){
        Toast.makeText(context,"Ảnh quá nặng vượt mức cho phép",Toast.LENGTH_SHORT).show()
    }
    val contentResolver = context.contentResolver
    val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val cursor: Cursor? = contentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC+"!=0", null, null)
    if (cursor != null && cursor.moveToFirst()) {
        do {
            val name: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val artist: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val url: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
            val duration: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
            var bitmapPicture :Bitmap?= null
            try{
                var media = MediaMetadataRetriever()
                media.setDataSource(url)
                var byteArray: ByteArray? = media.embeddedPicture
                bitmapPicture = try {
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
                }catch (ex:Exception){
                    bitmapDefault
                }
            }catch (ex : Exception){

            }
                var songOffline: SongOffline
                if (bitmapPicture == null) {
                    songOffline = SongOffline(name, artist, duration.toLong(), bitmapDefault, url)
                } else {
                    songOffline = SongOffline(
                        name,
                        artist,
                        duration.toLong(),
                        bitmapPicture,
                        url
                    )
                }
                listSong.add(songOffline)
        } while (cursor.moveToNext())
    }
    cursor!!.close()
    return listSong
}
fun reloadData(){
    AppPreferences.isRepeatOne = false
    AppPreferences.isShuffle = false
    AppPreferences.indexPlaying=-1
    AppPreferences.isPlaying = false
    AppPreferences.isServiceRunning = false
    AppPreferences.isOnline = false
}