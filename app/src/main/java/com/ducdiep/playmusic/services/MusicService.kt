package com.ducdiep.playmusic.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.activities.PlayMusicActivity
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.CHANNEL_ID
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.broadcasts.MyReceiver
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MusicService : Service() {
    var mediaPlayer: MediaPlayer? = null
    lateinit var mSongOffline: SongOffline
    lateinit var mSongOnline: Song
    var currentPos: Int = 0
    lateinit var mediaSession: MediaSessionCompat
    lateinit var playbackStateCompat: PlaybackStateCompat

    lateinit var listRandomed: ArrayList<Int>
    var listPreviousRandom: ArrayList<Int> = ArrayList()

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        AppPreferences.init(this)
        reloadListRandom()
        AppPreferences.isServiceRunning = true
        super.onCreate()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var actionMusic = intent.getIntExtra(ACTION_TO_SERVICE, 0)
        handleMusic(actionMusic)
        return START_NOT_STICKY
    }

    fun reloadListRandom() {
        listRandomed = ArrayList()
        for (i in 0 until listSongOffline!!.size) {
            listRandomed.add(i)
        }
    }

    private fun startMusicOffline(index: Int) {
        listRandomed.remove(index)
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        mediaPlayer!!.reset()
        mediaPlayer!!.setDataSource(this, Uri.parse(listSongOffline[index].resource))
        mediaPlayer!!.prepare()
        mediaPlayer?.start()
        AppPreferences.isPlaying = true
        sendActionToActivity(ACTION_START)
        mediaPlayer!!.setOnCompletionListener {
            if (AppPreferences.isRepeatOne) {
                startMusicOffline(AppPreferences.indexPlaying)
            } else {
                playNextMusic()
            }
        }
    }

    private fun startMusicOnline(index: Int) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        mediaPlayer!!.reset()
        mediaPlayer!!.setDataSource(
            this,
            Uri.parse("http://api.mp3.zing.vn/api/streaming/${listSongOnline[index].type}/${listSongOnline[index].id}/128")
        )
        mediaPlayer!!.prepare()
        mediaPlayer?.start()
        AppPreferences.isPlaying = true
        sendActionToActivity(ACTION_START)
        mediaPlayer!!.setOnCompletionListener {
            if (AppPreferences.isRepeatOne) {
                startMusicOnline(AppPreferences.indexPlaying)
            } else {
                playNextMusic()
            }
        }
    }

    fun handleMusic(action: Int) {
        when (action) {
            ACTION_START -> startNewMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_CLEAR -> {
                sendActionToActivity(ACTION_CLEAR)
                mediaPlayer?.stop()
                stopSelf()
            }
            ACTION_NEXT -> {
                playNextMusic()
            }
            ACTION_PREVIOUS -> {
                playPreviousMusic()
            }
        }
    }

    private fun startNewMusic() {
        if (AppPreferences.isOnline) {
            mSongOnline = listSongOnline[AppPreferences.indexPlaying]
            startMusicOnline(AppPreferences.indexPlaying)
            sendNotificationMedia(AppPreferences.indexPlaying)
        } else {
            mSongOffline = listSongOffline!![AppPreferences.indexPlaying]
            startMusicOffline(AppPreferences.indexPlaying)
            sendNotificationMedia(AppPreferences.indexPlaying)
        }

    }

    fun playPreviousMusic() {
        if (AppPreferences.isOnline) {
            if (AppPreferences.indexPlaying == 0) {
                startMusicOnline(listSongOnline.size - 1)
                AppPreferences.indexPlaying = listSongOnline.size - 1
            } else {
                startMusicOnline(AppPreferences.indexPlaying - 1)
                AppPreferences.indexPlaying = AppPreferences.indexPlaying - 1
            }
        } else {
            if (AppPreferences.isShuffle) {
                if (listPreviousRandom.size > 0) {
                    if (listPreviousRandom.size == 1) {
                        var index = listPreviousRandom.size - 1
                        startMusicOffline(listPreviousRandom[index])
                        AppPreferences.indexPlaying = listPreviousRandom[index]
                        listPreviousRandom.removeLastOrNull()
                    } else {
                        var index = listPreviousRandom.size - 2
                        startMusicOffline(listPreviousRandom[index])
                        AppPreferences.indexPlaying = listPreviousRandom[index]
                        listPreviousRandom.removeLastOrNull()
                    }

                } else {
                    if (listRandomed.size == 0) {
                        reloadListRandom()
                    }
                    var rd = (0 until listRandomed!!.size).random()
                    AppPreferences.indexPlaying = listRandomed[rd]
                    startMusicOffline(listRandomed[rd])
                    if (listRandomed.size == 0) {
                        reloadListRandom()
                    }
                }
            } else {
                if (AppPreferences.indexPlaying == 0) {
                    startMusicOffline(listSongOffline!!.size - 1)
                    AppPreferences.indexPlaying = listSongOffline!!.size - 1
                } else {
                    startMusicOffline(AppPreferences.indexPlaying - 1)
                    AppPreferences.indexPlaying = AppPreferences.indexPlaying - 1
                }
            }
        }

        sendNotificationMedia(AppPreferences.indexPlaying)
        sendActionToActivity(ACTION_PREVIOUS)
    }

    fun playNextMusic() {
        if (AppPreferences.isOnline) {
            if (AppPreferences.indexPlaying == listSongOnline.size - 1) {
                startMusicOnline(0)
                AppPreferences.indexPlaying = 0
            } else {
                startMusicOnline(AppPreferences.indexPlaying + 1)
                AppPreferences.indexPlaying = AppPreferences.indexPlaying + 1
            }
        } else {
            if (AppPreferences.isShuffle) {
                if (listRandomed.size == 0) {
                    reloadListRandom()
                }
                var rd = (0 until listRandomed.size).random()
                AppPreferences.indexPlaying = listRandomed[rd]
                listPreviousRandom.add(listRandomed[rd])
                startMusicOffline(listRandomed[rd])


            } else {
                if (AppPreferences.indexPlaying == listSongOffline.size - 1) {
                    startMusicOffline(0)
                    AppPreferences.indexPlaying = 0
                } else {
                    startMusicOffline(AppPreferences.indexPlaying + 1)
                    AppPreferences.indexPlaying = AppPreferences.indexPlaying + 1
                }
            }
        }

        sendNotificationMedia(AppPreferences.indexPlaying)
        sendActionToActivity(ACTION_NEXT)
    }


    fun pauseMusic() {
        currentPos = mediaPlayer!!.currentPosition
        mediaPlayer!!.pause()
        AppPreferences.isPlaying = false
        sendNotificationMedia(AppPreferences.indexPlaying)
        sendActionToActivity(ACTION_PAUSE)
    }

    fun resumeMusic() {
        mediaPlayer!!.start()
        AppPreferences.isPlaying = true
        sendNotificationMedia(AppPreferences.indexPlaying)
        sendActionToActivity(ACTION_RESUME)
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

    //add
    suspend fun getBitmapFromURL(src: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                null
            }
        }
    }

    fun sendNotificationMedia(index: Int) {
        mediaSession = MediaSessionCompat(this, "tag")

        if (AppPreferences.isOnline) {
            var songOn = listSongOnline[index]
            var name = songOn.name
            var artist = songOn.artists_names
            var duration = songOn.duration * 1000
            GlobalScope.launch(Dispatchers.Main) {
                var bitmap = getBitmapFromURL(songOn.thumbnail)
                showNotification(bitmap, name, artist, duration)
            }
        } else {
            var songOff = listSongOffline[index]
            var bitmap = songOff.imageBitmap
            var name = songOff.name
            var artist = songOff.artist
            var duration = songOff.duration
            showNotification(bitmap, name, artist, duration)
        }

    }

    private fun showNotification(bitmap: Bitmap?, name: String, artist: String, duration: Int) {
        //pending intent mở app khi bấm vào notification
        var intent = Intent(this, PlayMusicActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        var pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        //set up hiển thị tiến trình
        val mediaMetaData = MediaMetadataCompat.Builder()
        mediaMetaData.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            duration.toLong()
        )
        mediaSession.setMetadata(mediaMetaData.build())


        updateState(0)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                mediaPlayer!!.seekTo(pos.toInt())
                updateState(pos)
            }
        })
        mediaSession.isActive = true

        var notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setSmallIcon(R.drawable.ic_baseline_music_note_24)
            .setLargeIcon(bitmap)
            .setContentText(name)
            .setContentTitle(artist)
            .setContentIntent(pendingIntent)

        if (AppPreferences.isPlaying) {
            notificationBuilder
                .addAction(R.drawable.previous, "Previous", getPendingIntent(this, ACTION_PREVIOUS))
                .addAction(R.drawable.pause, "PlayOrPause", getPendingIntent(this, ACTION_PAUSE))
                .addAction(R.drawable.next, "Next", getPendingIntent(this, ACTION_NEXT))
                .addAction(R.drawable.close, "Close", getPendingIntent(this, ACTION_CLEAR))
        } else {
            notificationBuilder
                .addAction(R.drawable.previous, "Previous", getPendingIntent(this, ACTION_PREVIOUS))
                .addAction(
                    R.drawable.play24dp,
                    "PlayOrPause",
                    getPendingIntent(this, ACTION_RESUME)
                )
                .addAction(R.drawable.next, "Next", getPendingIntent(this, ACTION_NEXT))
                .addAction(R.drawable.close, "Close", getPendingIntent(this, ACTION_CLEAR))
        }

        var notification = notificationBuilder.build()
        startForeground(1, notification)
    }

    fun updateState(curentPos: Long) {
        if (AppPreferences.isPlaying) {
            playbackStateCompat = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.ACTION_PLAY.toInt(),
                    curentPos,
                    1.0f,
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        } else {
            playbackStateCompat = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.ACTION_PAUSE.toInt(),
                    curentPos,
                    1.0f, SystemClock.elapsedRealtime()
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        }
        mediaSession.setPlaybackState(playbackStateCompat)
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


    //gửi data qua activity để hiện thị trên UI
    fun sendActionToActivity(action: Int) {
        var intent = Intent(ACTION_SEND_TO_ACTIVITY)
        var bundle = Bundle()
        bundle.putInt(ACTION, action)
        bundle.putInt(CURRENT_POSITION, currentPos)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        reloadData()
        super.onDestroy()
    }
}