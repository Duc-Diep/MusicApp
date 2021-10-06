package com.ducdiep.playmusic.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ducdiep.playmusic.MainActivity
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.CHANNEL_ID
import com.ducdiep.playmusic.broadcasts.MyReceiver
import com.ducdiep.playmusic.objects.Song

const val ACTION_PAUSE = 1
const val ACTION_RESUME = 2
const val ACTION_CLEAR = 3

class MusicService : Service() {
    var isPlaying = false
    var mediaPlayer: MediaPlayer? = null
    lateinit var mSong:Song
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var song = intent.extras?.getSerializable("song") as Song
        if (song != null) {
            startMusic(song)
            mSong = song
            sendNotifiCation(song)
        }
        var actionMusic = intent.getIntExtra("action_music_receiver",0)
        handleMusic(actionMusic)
        return START_NOT_STICKY
    }

    private fun startMusic(song: Song) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(applicationContext, song.resource)
        }
        mediaPlayer?.start()
        isPlaying = true
    }

    fun handleMusic(action: Int) {
        when (action) {
            ACTION_RESUME -> resumeMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_CLEAR -> stopSelf()
        }
    }


    private fun pauseMusic() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            isPlaying = false
            sendNotifiCation(mSong)
        }
    }

    private fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer!!.start()
            isPlaying = true
            sendNotifiCation(mSong)
        }
    }

    private fun sendNotifiCation(song: Song) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            var intent = Intent(this, MainActivity::class.java)
            var pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            //set up remote view(custom notification)
            var remoteView = RemoteViews(packageName, R.layout.custom_notification)
            remoteView.setTextViewText(R.id.tv_name, song.name)
            remoteView.setTextViewText(R.id.tv_single, song.single)
            var bitmap = BitmapFactory.decodeResource(resources, song.image)
            remoteView.setImageViewBitmap(R.id.img_song, bitmap)
            remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)

            //set on click action
            if (isPlaying) {
                remoteView.setOnClickPendingIntent(
                    R.id.btn_play_or_pause, getPendingIntent(
                        this,
                        ACTION_PAUSE
                    )
                )
                remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.play)
            } else {
                remoteView.setOnClickPendingIntent(
                    R.id.btn_play_or_pause, getPendingIntent(
                        this,
                        ACTION_RESUME
                    )
                )
                remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)
            }
            remoteView.setOnClickPendingIntent(
                R.id.btn_close, getPendingIntent(
                    this,
                    ACTION_CLEAR
                )
            )

            var notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setCustomContentView(remoteView)
                .setSmallIcon(R.drawable.music_logo)
                .setSound(null)
                .build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    fun getPendingIntent(context: Context, action: Int): PendingIntent {
        var intent = Intent(this,MyReceiver::class.java)
        intent.putExtra("action",action)
        return PendingIntent.getBroadcast(context.applicationContext,action,intent,PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
}