package com.ducdiep.playmusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.objects.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var mSong: Song
    var isPlaying: Boolean = false

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            mSong = bundle.get(SONG_OBJECT) as Song
            isPlaying = bundle.getBoolean(STATUS_PLAY)
            var action = bundle.getInt(ACTION)

            handleLayoutPlay(action)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        btn_start_service.setOnClickListener {
            playMusic()
        }
        btn_stop_service.setOnClickListener {
            stopMusic()
        }
        btn_play_or_pause.setOnClickListener {
            if (isPlaying){
                sendActionToService(ACTION_PAUSE)
            }else{
                sendActionToService(ACTION_RESUME)
            }
        }
        btn_close.setOnClickListener {
            sendActionToService(ACTION_CLEAR)
        }
    }

    private fun stopMusic() {
        var intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }

    private fun playMusic() {
        var intent = Intent(this, MusicService::class.java)
        var bundle = Bundle()
        bundle.putSerializable(
            SONG_OBJECT,
            Song("Key of truth", "Đức Điệp", R.drawable.mayu, R.raw.key_of_truth)
        )
        intent.putExtras(bundle)
        startService(intent)
    }

    private fun handleLayoutPlay(action: Int) {
        when (action) {
            ACTION_START -> {
                layout_playing.visibility = View.VISIBLE
                showDetailMusic()
                setStatusButton()
            }
            ACTION_PAUSE -> setStatusButton()
            ACTION_RESUME -> setStatusButton()
            ACTION_CLEAR -> layout_playing.visibility = View.GONE
        }
    }

    fun showDetailMusic() {
        if (mSong == null) {
            return
        }
        img_song.setImageResource(mSong.image)
        tv_name.text = mSong.name
        tv_single.text = mSong.single
    }

    fun setStatusButton() {
        if (isPlaying) {
            btn_play_or_pause.setImageResource(R.drawable.pause)
        } else {
            btn_play_or_pause.setImageResource(R.drawable.play)

        }
    }
    fun sendActionToService(action:Int){
        var intent = Intent(this,MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE,action)
        startService(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}