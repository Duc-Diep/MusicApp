package com.ducdiep.playmusic.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.MainActivity
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.CHANNEL_ID
import com.ducdiep.playmusic.broadcasts.MyReceiver
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.objects.Song


class MusicService : Service() {
    var isPlaying = false
    var mediaPlayer: MediaPlayer? = null
    lateinit var mSong: Song
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //nhận dữ liệu từ activity khi gọi start service
        var bundle = intent.extras
        if (bundle != null) {
            var song = bundle.get(SONG_OBJECT) as Song?
            if (song != null) {
                mSong = song
                startMusic(song)
                sendNotification(song)
            }
        }
        //xử lí action nhận từ broadcast
        var actionMusic = intent.getIntExtra(ACTION_TO_SERVICE, 0)
        handleMusic(actionMusic)
        return START_NOT_STICKY
    }

    private fun startMusic(song: Song) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(applicationContext, song.resource)
        }
        mediaPlayer?.start()
        isPlaying = true
        sendActionToActivity(ACTION_START)
    }

    fun handleMusic(action: Int) {
        when (action) {
            ACTION_RESUME -> resumeMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_CLEAR -> {
                stopSelf()
                sendActionToActivity(ACTION_CLEAR)
            }

        }
    }


    private fun pauseMusic() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            isPlaying = false
            sendNotification(mSong)
            sendActionToActivity(ACTION_PAUSE)
        }
    }

    private fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer!!.start()
            isPlaying = true
            sendNotification(mSong)
            sendActionToActivity(ACTION_RESUME)
        }
    }

    private fun sendNotification(song: Song) {
        var intent = Intent(this, MainActivity::class.java)
        var pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        //custome thông báo
        var remoteView = RemoteViews(packageName, R.layout.custom_notification)
        remoteView.setTextViewText(R.id.tv_name, song.name)
        remoteView.setTextViewText(R.id.tv_single, song.single)
        var bitmap = BitmapFactory.decodeResource(resources, song.image)
        remoteView.setImageViewBitmap(R.id.img_song, bitmap)
        remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)

        //xử lí click cho các nút trên thông báo
        if (isPlaying) {
            remoteView.setOnClickPendingIntent(
                R.id.btn_play_or_pause, getPendingIntent(
                    this,
                    ACTION_PAUSE
                )
            )
            remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)
        } else {
            remoteView.setOnClickPendingIntent(
                R.id.btn_play_or_pause, getPendingIntent(
                    this,
                    ACTION_RESUME
                )
            )
            remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.play)
        }
        remoteView.setOnClickPendingIntent(
            R.id.btn_close, getPendingIntent(
                this,
                ACTION_CLEAR
            )
        )
        //Khởi tạo thông báo
        var notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setCustomContentView(remoteView)
            .setSmallIcon(R.drawable.music_logo)
            .setSound(null)
            .build()
        startForeground(1, notification)

    }





    //gửi action sang broadcast khi bấm nút
    fun getPendingIntent(context: Context, action: Int): PendingIntent {
        var intent = Intent(this, MyReceiver::class.java)
        intent.putExtra(ACTION_SERVICE_TO_BROADCAST, action)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            action,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
    //gửi data qua activity để hiện thị trên UI
    fun sendActionToActivity(action: Int) {
        var intent = Intent(ACTION_SEND_TO_ACTIVITY)
        var bundle = Bundle()
        bundle.putSerializable(SONG_OBJECT, mSong)
        bundle.putBoolean(STATUS_PLAY, isPlaying)
        bundle.putInt(ACTION, action)

        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}