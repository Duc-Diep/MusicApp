package com.ducdiep.playmusic.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongFavourite
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_list_offline.*

open class HandleViewModel(application: Application) : AndroidViewModel(application) {
    var context: Context = getApplication<Application>().applicationContext
    var mSongFavourite = MutableLiveData<SongFavourite>()
    var mSongOnline = MutableLiveData<Song>()
    var mSongOffline = MutableLiveData<SongOffline>()
    var isVisibleLayout = MutableLiveData<Boolean>()
    var isPlaying = MutableLiveData<Boolean>()

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            var action = bundle.getInt(ACTION)
            handleLayoutPlay(action)
        }
    }

    //dang ki broadcast
    init {
        AppPreferences.init(context)
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        super.onCleared()
    }

    fun handleLayoutPlay(action: Int) {
        when (action) {
            ACTION_START -> {
                isVisibleLayout.value = true
//                layout_playing.visibility = View.VISIBLE
                getMusic()
                setStatusButton()
            }
            ACTION_PAUSE -> setStatusButton()
            ACTION_RESUME -> setStatusButton()
            ACTION_CLEAR -> {
                isVisibleLayout.value = false
//                layout_playing.visibility = View.GONE
                reloadData()
            }
            ACTION_NEXT -> getMusic()
            ACTION_PREVIOUS -> getMusic()
        }
    }

    fun setStatusButton() {
        isPlaying.value = AppPreferences.isPlaying
    }

    fun getMusic() {
        if (AppPreferences.isPlayFavouriteList) {
            mSongFavourite.value = listSongFavourite[AppPreferences.indexPlaying]
        } else {
            if (AppPreferences.isOnline) {
                mSongOnline.value = listSongOnline[AppPreferences.indexPlaying]
            } else {
                mSongOffline.value = listSongOffline[AppPreferences.indexPlaying]
            }
        }
    }

    fun sendActionToService(action: Int) {
        var intent = Intent(context, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, action)
        context.startService(intent)
    }

    fun onClickPlayOrPause(){
        if (isPlaying.value==true) {
            sendActionToService(ACTION_PAUSE)
        } else {
            sendActionToService(ACTION_RESUME)
        }
    }

    fun onClickClose(){
        sendActionToService(ACTION_CLEAR)
    }

    fun onClickNext(){
        sendActionToService(ACTION_NEXT)
    }

    fun onClickPrevious(){
        sendActionToService(ACTION_NEXT)
    }
    fun startMusic(){
        sendActionToService(ACTION_START)
    }

}