package com.ducdiep.playmusic.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_play_music.*

class PlayMusicActivity : AppCompatActivity() {
    //set up bound service
    private lateinit var mService: MusicService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            mService = binder.getService()
            setProgress()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }
    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    //variables
    lateinit var listSong: ArrayList<Song>
    var isPlaying: Boolean = false
    lateinit var mSong:Song
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            isPlaying = bundle.getBoolean(STATUS_PLAY)
            var action = bundle.getInt(ACTION)
            handleLayoutPlay(action)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)
        AppPreferences.init(this)
        supportActionBar?.hide()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        loadAllMusic()
        showDetailMusic()
    }

    fun loadAllMusic() {
        listSong = loadDefaultMusic(this)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            listSong!!.addAll(getAudio(this))
        }
    }

    private fun handleLayoutPlay(action: Int) {
        when (action) {
            ACTION_START -> {
                showDetailMusic()
                setStatusButton()
                if (mBound){
                    setProgress()
                }
            }
            ACTION_PAUSE -> setStatusButton()
            ACTION_RESUME -> setStatusButton()
            ACTION_NEXT -> showDetailMusic()
            ACTION_PREVIOUS -> showDetailMusic()
        }
    }

    private fun setStatusButton() {
        if (isPlaying) {
            btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        } else {
            btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        }
    }

    fun showDetailMusic() {
        mSong = listSong[AppPreferences.indexPlaying]
        img_music.setImageBitmap(mSong.imageBitmap)
        tv_song_name.text = mSong.name
        tv_artist.text = mSong.artist
    }

    fun setProgress(){
        mSong = listSong[AppPreferences.indexPlaying]
        var duration = mSong.duration.toLong()
        tv_progress.text = timerConversion(mService.getCurrentPos().toLong())
        tv_duration.text = timerConversion(duration)
        seekbar_handle.max = duration.toInt()
        val handler = Handler(mainLooper)

        val runnable: Runnable = object : Runnable {
            override fun run() {
                try {
                    tv_progress.text = timerConversion(mService.getCurrentPos().toLong())
                    seekbar_handle.progress = mService.getCurrentPos()
                    handler.postDelayed(this,1000)
                } catch (ed: IllegalStateException) {
                    ed.printStackTrace()
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    fun timerConversion(value: Long): String {
        val audioTime: String
        val dur = value.toInt()
        val hrs = dur / 3600000
        val mns = dur / 60000 % 60000
        val scs = dur % 60000 / 1000
        audioTime = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mns, scs)
        } else {
            String.format("%02d:%02d", mns, scs)
        }
        return audioTime
    }
}