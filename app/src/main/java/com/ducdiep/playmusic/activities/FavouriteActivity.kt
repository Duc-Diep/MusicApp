package com.ducdiep.playmusic.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SongOfflineAdapter
import com.ducdiep.playmusic.adapters.SongOnlineAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.helpers.SqlHelper
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_favourite.*

class FavouriteActivity : AppCompatActivity() {

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            var action = bundle.getInt(ACTION)
            handleLayoutPlay(action)
        }
    }
    lateinit var mSongOffline: SongOffline
    lateinit var mSongOnline: Song
    lateinit var glide: RequestManager
    lateinit var sqlHelper:SqlHelper
    lateinit var songOnlineAdapter:SongOnlineAdapter
    lateinit var listSong:List<Song>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourite)
        supportActionBar?.hide()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        sqlHelper = SqlHelper(this)
        glide = Glide.with(this)
        setupAdapter()
        if (AppPreferences.indexPlaying != -1) {
            handleLayoutPlay(ACTION_START)
        }
        btn_play_list.setOnClickListener {
            listSongOnline.clear()
            listSongOnline.addAll(listSong)
            AppPreferences.indexPlaying = 0
            AppPreferences.isOnline = true
            AppPreferences.isPlayRequireList = true
            sendActionToService(ACTION_START)
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
        }
        btn_play_or_pause.setOnClickListener {
            if (AppPreferences.isPlaying) {
                sendActionToService(ACTION_PAUSE)
            } else {
                sendActionToService(ACTION_RESUME)
            }
        }
        btn_close.setOnClickListener {
            sendActionToService(ACTION_CLEAR)
        }
        btn_next.setOnClickListener {
            sendActionToService(ACTION_NEXT)

        }
        btn_previous.setOnClickListener {
            sendActionToService(ACTION_PREVIOUS)

        }
        layout_title.setOnClickListener {
            var intent = Intent(this, PlayMusicActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }
    private fun setupAdapter() {
        listSong = sqlHelper.getAllSong()
        songOnlineAdapter = SongOnlineAdapter(this, listSong)
        songOnlineAdapter.setOnClickItem {
            listSongOnline.clear()
            listSongOnline.addAll(listSong)
            AppPreferences.indexPlaying = listSong.indexOf(it)
            AppPreferences.isOnline = true
            AppPreferences.isPlayRequireList = true
            sendActionToService(ACTION_START)
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
        }
        rcv_songs.adapter = songOnlineAdapter
    }
    fun sendActionToService(action: Int) {
        var intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, action)
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
            ACTION_CLEAR -> {
                layout_playing.visibility = View.GONE
                reloadData()
            }
            ACTION_NEXT -> showDetailMusic()
            ACTION_PREVIOUS -> showDetailMusic()
        }
    }

    fun showDetailMusic() {
        if (AppPreferences.isOnline){
            mSongOnline = listSongOnline[AppPreferences.indexPlaying]
            var linkImage = mSongOnline.thumbnail
            glide.load(linkImage).into(img_song)
            tv_name.text = mSongOnline.name
            tv_name.isSelected = true
            tv_single.text = mSongOnline.artists_names
            tv_single.isSelected = true
        }else{
            mSongOffline = MyApplication.listSongOffline[AppPreferences.indexPlaying]
            img_song.setImageBitmap(mSongOffline.imageBitmap)
            tv_name.text = mSongOffline.name
            tv_name.isSelected = true
            tv_single.text = mSongOffline.artist
            tv_single.isSelected = true
            tv_single.isFocusable = true
        }

    }
    fun setStatusButton() {
        if (AppPreferences.isPlaying) {
            btn_play_or_pause.setImageResource(R.drawable.pause)
        } else {
            btn_play_or_pause.setImageResource(R.drawable.play)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}