package com.ducdiep.playmusic.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song

public const val CHANNEL_ID = "channel_id_music_app"
public const val CHANNEL_NAME = "channel_name_music_app"

class MyApplication : Application() {
    companion object{
        var listSongOffline:ArrayList<SongOffline> = ArrayList()
        var listSongOnline:ArrayList<Song> = ArrayList()
        var listSongFavourite:ArrayList<SongFavourite> = ArrayList()
    }

    override fun onCreate() {
        super.onCreate()
        createChannelNotifycation()

    }

    private fun createChannelNotifycation() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            var channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null,null)
            var manager = getSystemService(NotificationManager::class.java)
            if (manager!=null){
                manager.createNotificationChannel(channel)
            }
        }
    }
}