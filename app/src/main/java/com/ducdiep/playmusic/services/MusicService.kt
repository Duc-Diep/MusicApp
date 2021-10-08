package com.ducdiep.playmusic.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.MainActivity
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.CHANNEL_ID
import com.ducdiep.playmusic.broadcasts.MyReceiver
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.Song


class MusicService : Service() {
    var isPlaying = false
    var mediaPlayer: MediaPlayer? = null
    lateinit var mSong: Song
    lateinit var mPlaybackState: PlaybackStateCompat
    lateinit var bitmap: Bitmap

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
                bitmap = BitmapFactory.decodeResource(resources,song.image)
                startMusic(song)
                sendNotificationMedia(song)
            }
        }
        //xử lí action nhận từ broadcast
        var actionMusic = intent.getIntExtra(ACTION_TO_SERVICE, 0)
        handleMusic(actionMusic)
        return START_NOT_STICKY
    }

    private fun startMusic(song: Song) {
        mediaPlayer = MediaPlayer()
        if (mediaPlayer == null|| mediaPlayer!!.isPlaying) {
            if (mediaPlayer!=null){
                mediaPlayer!!.stop()
                mediaPlayer!!.reset()
            }
        }
        mediaPlayer!!.setDataSource(this,Uri.parse(song.resource))
        mediaPlayer!!.prepare()
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
            sendNotificationMedia(mSong)
            sendActionToActivity(ACTION_PAUSE)
        }
    }

    private fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer!!.start()
            isPlaying = true
            sendNotificationMedia(mSong)
            sendActionToActivity(ACTION_RESUME)
        }
    }

//    private fun sendNotification(song: Song) {
//        var intent = Intent(this, MainActivity::class.java)
//        var pendingIntent =
//            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//        //custome thông báo
//        var remoteView = RemoteViews(packageName, R.layout.custom_notification)
//        remoteView.setTextViewText(R.id.tv_name, song.name)
//        remoteView.setTextViewText(R.id.tv_single, song.single)
//        var bitmap = BitmapFactory.decodeResource(resources, song.image)
//        remoteView.setImageViewBitmap(R.id.img_song, bitmap)
//        remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)
//
//        //xử lí click cho các nút trên thông báo
//        if (isPlaying) {
//            remoteView.setOnClickPendingIntent(
//                R.id.btn_play_or_pause, getPendingIntent(
//                    this,
//                    ACTION_PAUSE
//                )
//            )
//            remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.pause)
//        } else {
//            remoteView.setOnClickPendingIntent(
//                R.id.btn_play_or_pause, getPendingIntent(
//                    this,
//                    ACTION_RESUME
//                )
//            )
//            remoteView.setImageViewResource(R.id.btn_play_or_pause, R.drawable.play)
//        }
//        remoteView.setOnClickPendingIntent(
//            R.id.btn_close, getPendingIntent(
//                this,
//                ACTION_CLEAR
//            )
//        )
//        //Khởi tạo thông báo
//        var notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentIntent(pendingIntent)
//            .setCustomContentView(remoteView)
//            .setSmallIcon(R.drawable.music_logo)
//            .setSound(null)
//            .build()
//        startForeground(1, notification)
//
//    }

    fun sendNotificationMedia(song: Song) {
        //pending intent mở app khi bấm vào notification
        var intent = Intent(this, MainActivity::class.java)
        var pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        //set up hiển thị tiến trình
        val mediaMetaData = MediaMetadataCompat.Builder()
        mediaMetaData.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            mediaPlayer!!.duration.toLong()
        )

        if (isPlaying) {
            mPlaybackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.ACTION_PLAY.toInt(),
                    mediaPlayer!!.currentPosition.toLong(),
                    1.0f,
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        } else {
            mPlaybackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.ACTION_PAUSE.toInt(),
                    mediaPlayer!!.currentPosition.toLong(),
                    1.0f
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        }


        var mediaSession = MediaSessionCompat(this, "tag")
        mediaSession.setMetadata(mediaMetaData.build())
        mediaSession.setPlaybackState(mPlaybackState)
        mediaSession.isActive = true

        var notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setProgress(mediaPlayer!!.duration, mPlaybackState.position.toInt(), false)
            .setSmallIcon(R.drawable.music_logo)
            .setLargeIcon(bitmap)
            .setContentText(song.artist)
            .setContentTitle(song.name)
            .setContentIntent(pendingIntent)

        if (isPlaying) {
            notificationBuilder
                .addAction(R.drawable.previous, "Previous", null)
                .addAction(R.drawable.pause, "PlayOrPause", getPendingIntent(this, ACTION_PAUSE))
                .addAction(R.drawable.next, "Next", null)
                .addAction(R.drawable.close, "Close", getPendingIntent(this, ACTION_CLEAR))
        } else {
            notificationBuilder
                .addAction(R.drawable.previous, "Previous", null)
                .addAction(R.drawable.play, "PlayOrPause", getPendingIntent(this, ACTION_RESUME))
                .addAction(R.drawable.next, "Next", null)
                .addAction(R.drawable.close, "Close", getPendingIntent(this, ACTION_CLEAR))
        }

        var notification = notificationBuilder.build()
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