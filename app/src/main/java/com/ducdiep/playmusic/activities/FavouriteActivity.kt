package com.ducdiep.playmusic.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SongFavouriteAdapter
import com.ducdiep.playmusic.adapters.SongOfflineAdapter
import com.ducdiep.playmusic.adapters.SongOnlineAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongFavourite
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.helpers.SqlHelper
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import com.ducdiep.playmusic.viewmodel.FavouriteViewModel
import com.ducdiep.playmusic.viewmodel.HandleViewModel
import kotlinx.android.synthetic.main.activity_favourite.*
import kotlinx.android.synthetic.main.activity_favourite.btn_close
import kotlinx.android.synthetic.main.activity_favourite.btn_next
import kotlinx.android.synthetic.main.activity_favourite.btn_play_or_pause
import kotlinx.android.synthetic.main.activity_favourite.btn_previous
import kotlinx.android.synthetic.main.activity_favourite.img_song
import kotlinx.android.synthetic.main.activity_favourite.layout_title
import kotlinx.android.synthetic.main.activity_favourite.rcv_songs
import kotlinx.android.synthetic.main.activity_favourite.tv_name
import kotlinx.android.synthetic.main.activity_favourite.tv_single

class FavouriteActivity : AppCompatActivity() {

    //    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            var bundle = intent.extras
//            if (bundle == null) return
//            var action = bundle.getInt(ACTION)
//            handleLayoutPlay(action)
//        }
//    }
//    lateinit var mSongOffline: SongOffline
//    lateinit var mSongOnline: Song
//    lateinit var mSongFavourite: SongFavourite
    lateinit var handleViewModel: HandleViewModel
    lateinit var favouriteViewModel: FavouriteViewModel
    lateinit var glide: RequestManager
    lateinit var sqlHelper: SqlHelper
    lateinit var songOnlineAdapter: SongFavouriteAdapter
    lateinit var listSong: List<SongFavourite>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourite)
        supportActionBar?.hide()
//        LocalBroadcastManager.getInstance(this)
//            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        init()
        btn_play_list.setOnClickListener {
            listSongFavourite.clear()
            listSongFavourite.addAll(listSong)
            AppPreferences.indexPlaying = 0
            AppPreferences.isOnline = false
            AppPreferences.isPlayRequireList = true
            AppPreferences.isPlayFavouriteList = true
            handleViewModel.startMusic()
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
        }
        btn_play_or_pause.setOnClickListener {
            handleViewModel.onClickPlayOrPause()
        }
        btn_close.setOnClickListener {
            handleViewModel.onClickClose()
        }
        btn_next.setOnClickListener {
            handleViewModel.onClickNext()
        }
        btn_previous.setOnClickListener {
            handleViewModel.onClickPrevious()

        }
        layout_title.setOnClickListener {
            var intent = Intent(this, PlayMusicActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    fun init() {
        favouriteViewModel = ViewModelProvider(this).get(FavouriteViewModel::class.java)
        favouriteViewModel.fetchRepoListFavourite().observe(this, { t ->
            listSong = t
            setupAdapter()
        })
        favouriteViewModel.fetchRepoListFavourite()
        handleViewModel = ViewModelProvider(this).get(HandleViewModel::class.java)
        handleViewModel.isPlaying.observe(this, { t ->
            if (t == true) {
                btn_play_or_pause.setImageResource(R.drawable.pause)
            } else {
                btn_play_or_pause.setImageResource(R.drawable.play)
            }
        })
        handleViewModel.isVisibleLayout.observe(this, { t ->
            if (t == true) {
                layout_playing.visibility = View.VISIBLE
            } else {
                layout_playing.visibility = View.GONE
            }

        })
        handleViewModel.mSongOffline.observe(this,
            { t -> showMusicOffline(t) })
        handleViewModel.mSongOnline.observe(this,
            { t -> showMusicOnline(t) })
        handleViewModel.mSongFavourite.observe(this,
            { t -> showMusicfavourite(t) })
        sqlHelper = SqlHelper(this)
        glide = Glide.with(this)
        if (AppPreferences.indexPlaying != -1) {
            handleViewModel.handleLayoutPlay(ACTION_START)
        }
    }

    private fun setupAdapter() {
        songOnlineAdapter = SongFavouriteAdapter(this, listSong)
        songOnlineAdapter.setOnClickItem {
            listSongFavourite.clear()
            listSongFavourite.addAll(listSong)
            AppPreferences.indexPlaying = listSongFavourite.indexOf(it)
            AppPreferences.isOnline = false
            AppPreferences.isPlayRequireList = true
            AppPreferences.isPlayFavouriteList = true
            handleViewModel.startMusic()
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
        }
        rcv_songs.adapter = songOnlineAdapter
    }

    private fun showMusicOffline(t: SongOffline?) {
        img_song.setImageBitmap(t?.imageBitmap)
        tv_name.text = t?.name
        tv_name.isSelected = true
        tv_single.text = t?.artist
        tv_single.isSelected = true
        tv_single.isFocusable = true
    }

    private fun showMusicOnline(t: Song?) {
        var linkImage = t?.thumbnail
        glide.load(linkImage).into(img_song)
        tv_name.text = t?.name
        tv_name.isSelected = true
        tv_single.text = t?.artists_names
        tv_single.isSelected = true
    }

    private fun showMusicfavourite(t: SongFavourite?) {
        if (t?.url == "") {
            var linkImage = t.thumbnail
            glide.load(linkImage).into(img_song)
            tv_name.text = t.name
            tv_name.isSelected = true
            tv_single.text = t.artists_names
            tv_single.isSelected = true
        } else {
            img_song.setImageBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.musical_default
                )
            )
            tv_name.text = t?.name
            tv_name.isSelected = true
            tv_single.text = t?.artists_names
            tv_single.isSelected = true
            tv_single.isFocusable = true
        }
    }

//    fun sendActionToService(action: Int) {
//        var intent = Intent(this, MusicService::class.java)
//        intent.putExtra(ACTION_TO_SERVICE, action)
//        startService(intent)
//    }

//    private fun handleLayoutPlay(action: Int) {
//        when (action) {
//            ACTION_START -> {
//                layout_playing.visibility = View.VISIBLE
//                showDetailMusic()
//                setStatusButton()
//            }
//            ACTION_PAUSE -> setStatusButton()
//            ACTION_RESUME -> setStatusButton()
//            ACTION_CLEAR -> {
//                layout_playing.visibility = View.GONE
//                reloadData()
//            }
//            ACTION_NEXT -> showDetailMusic()
//            ACTION_PREVIOUS -> showDetailMusic()
//        }
//    }

//    fun showDetailMusic() {
//        if (AppPreferences.isPlayFavouriteList) {
//            mSongFavourite = listSongFavourite[AppPreferences.indexPlaying]
//            if (mSongFavourite.url == "") {
//                var linkImage = mSongFavourite.thumbnail
//                glide.load(linkImage).into(img_song)
//                tv_name.text = mSongFavourite.name
//                tv_name.isSelected = true
//                tv_single.text = mSongFavourite.artists_names
//                tv_single.isSelected = true
//            } else {
//                img_song.setImageBitmap(
//                    BitmapFactory.decodeResource(
//                        resources,
//                        R.drawable.musical_default
//                    )
//                )
//                tv_name.text = mSongFavourite.name
//                tv_name.isSelected = true
//                tv_single.text = mSongFavourite.artists_names
//                tv_single.isSelected = true
//                tv_single.isFocusable = true
//            }
//        } else {
//            if (AppPreferences.isOnline) {
//                mSongOnline = listSongOnline[AppPreferences.indexPlaying]
//                var linkImage = mSongOnline.thumbnail
//                glide.load(linkImage).into(img_song)
//                tv_name.text = mSongOnline.name
//                tv_name.isSelected = true
//                tv_single.text = mSongOnline.artists_names
//                tv_single.isSelected = true
//            } else {
//                mSongOffline = MyApplication.listSongOffline[AppPreferences.indexPlaying]
//                img_song.setImageBitmap(mSongOffline.imageBitmap)
//                tv_name.text = mSongOffline.name
//                tv_name.isSelected = true
//                tv_single.text = mSongOffline.artist
//                tv_single.isSelected = true
//                tv_single.isFocusable = true
//            }
//        }
//
//
//    }
//
//    fun setStatusButton() {
//        if (AppPreferences.isPlaying) {
//            btn_play_or_pause.setImageResource(R.drawable.pause)
//        } else {
//            btn_play_or_pause.setImageResource(R.drawable.play)
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}