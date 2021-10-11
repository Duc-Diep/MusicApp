package com.ducdiep.playmusic.config

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.models.Song
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import kotlin.math.floor

const val ACTION_PAUSE = 1
const val ACTION_RESUME = 2
const val ACTION_CLEAR = 3
const val ACTION_START = 4
const val ACTION_NEXT = 6
const val ACTION_PREVIOUS = 5
const val PERMISSION_REQUEST = 7
const val ACTION_RELOAD = "reload"
const val SONG_OBJECT = "song"
const val STATUS_PLAY = "status_play"
const val ACTION = "action"
const val CURRENT_POSITION = "position"
const val IMAGE = "image"
const val INDEX = "index"
const val PROGRESS = "progress"
const val ACTION_SEND_TO_ACTIVITY = "action_send_data_to_activity"
const val ACTION_SERVICE_TO_BROADCAST = "action_service_to_broadcast"
const val ACTION_TO_SERVICE = "action_broadcast_to_service"
lateinit var bitmapDefault1: Bitmap
lateinit var bitmapDefault2: Bitmap


fun loadDefaultMusic(context: Context): ArrayList<Song> {
    bitmapDefault1 = BitmapFactory.decodeResource(context.resources, R.drawable.mayu)
    var listSong = ArrayList<Song>()
    listSong.apply {
        add(
            Song(
                "Key of truth",
                "Sweet Arms",
                "233000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.key_of_truth
            )
        )
        add(
            Song(
                "Date a live",
                "Sweet Arms",
                "107000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.date_a_live_spirit_pledge
            )
        )
        add(
            Song(
                "Dramma",
                "МиМиМи (Mimimi)",
                "224000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.dramma
            )
        )
        add(
            Song(
                "EDM",
                "Đức Điệp",
                "307000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.edm
            )
        )
        add(
            Song(
                "Ichinen Nikagetsu Hatsuka",
                "BRIGHT",
                "313000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.ichinen_nikagetsu_hatsuka
            )
        )
        add(
            Song(
                "Summertime",
                "Cinnamons, Evening Cinema",
                "251000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.summertime
            )
        )
        add(
            Song(
                "Xomu Lantern",
                "Miyuri Remix",
                "211000",
                bitmapDefault1,
                "android.resource://com.ducdiep.playmusic/" + R.raw.xomu_lantern
            )
        )
    }
    return listSong
}

fun getAudio(context: Context): ArrayList<Song> {
    var listSong = ArrayList<Song>()
    bitmapDefault2 = BitmapFactory.decodeResource(context.resources, R.drawable.tohka)
    val contentResolver = context.contentResolver
    val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val cursor: Cursor? = contentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC, null, null)
    if (cursor != null && cursor.moveToFirst()) {
        do {
            val name: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val artist: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val url: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
            var media = MediaMetadataRetriever()
            media.setDataSource(url)
            var bitmap: ByteArray? = media.embeddedPicture
            val duration: String =
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))

            if (url.contains(".mp3")) {
                var song: Song
                if (bitmap == null) {
                    song = Song(name, artist, duration, bitmapDefault2, url)
                } else {
                    song = Song(
                        name,
                        artist,
                        duration,
                        BitmapFactory.decodeByteArray(bitmap, 0, bitmap.size),
                        url
                    )
                }
                listSong.add(song)
            }
        } while (cursor.moveToNext())
    }
    return listSong
}