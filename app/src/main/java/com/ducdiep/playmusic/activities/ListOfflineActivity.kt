package com.ducdiep.playmusic.activities

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SongOfflineAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_list_offline.*
import kotlinx.android.synthetic.main.activity_play_music.*
import java.util.*
import kotlin.collections.ArrayList

class ListOfflineActivity : AppCompatActivity() {
    lateinit var mSongOffline: SongOffline
    lateinit var mSongOnline: Song
    lateinit var glide: RequestManager
    lateinit var songOfflineAdapter: SongOfflineAdapter
    private val searcByvoice =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                var hi = it.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                edt_search.setText(hi!![0])
            }
        }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            var action = bundle.getInt(ACTION)
            handleLayoutPlay(action)
        }
    }


    var handleSeach = Handler()
    val runSearch = Runnable {
        var s = edt_search.text.toString()
        var listTemp = ArrayList<SongOffline>()
        for (element in listSongOffline) {
            if (element.name.toLowerCase()
                    .contains(s.toLowerCase()) || element.artist.toLowerCase()
                    .contains(
                        s.toLowerCase()
                    )
            ) {
                listTemp.add(element)
            }
        }
        songOfflineAdapter = SongOfflineAdapter(this@ListOfflineActivity, listTemp)
        songOfflineAdapter.setOnClickItem {
            AppPreferences.indexPlaying = listSongOffline.indexOf(it)
            AppPreferences.isOnline = false
            AppPreferences.isPlayRequireList = true
            playMusic()
            var intent = Intent(this@ListOfflineActivity, PlayMusicActivity::class.java)
            startActivity(intent)
        }
        rcv_songs.adapter = songOfflineAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_offline)
        AppPreferences.init(this)
        supportActionBar?.hide()
        glide = Glide.with(this)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))

        setupAdapter()

        if (AppPreferences.indexPlaying != -1) {
            handleLayoutPlay(ACTION_START)
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

        edt_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                handleSeach.removeCallbacks(runSearch)
                handleSeach.postDelayed(runSearch, 800)
            }

        })

        btn_mic.setOnClickListener {
            showPopup(it)
        }

    }

    fun sendActionToService(action: Int) {
        var intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, action)
        startService(intent)
    }


    private fun setupAdapter() {
        songOfflineAdapter = SongOfflineAdapter(this, listSongOffline)
        songOfflineAdapter.setOnClickItem {
            AppPreferences.indexPlaying = listSongOffline.indexOf(it)
            playMusic()
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)

        }
        rcv_songs.adapter = songOfflineAdapter
    }

    private fun playMusic() {
        sendActionToService(ACTION_START)
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
            mSongOffline = listSongOffline[AppPreferences.indexPlaying]
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

    //create popup
    private fun showPopup(view: View) {
        var popupMenu = PopupMenu(this, view)
        var inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_language, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.lan_vn -> {
                    searchByVoice("vi")
                    true
                }
                R.id.lan_en -> {
                    searchByVoice("en")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun searchByVoice(s: String) {
        when (s) {
            "vi" -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, s)
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó đi: ")
                try {
                    searcByvoice.launch(intent)
                } catch (ex: Exception) {
                    Toast.makeText(
                        this@ListOfflineActivity,
                        "Gặp lỗi khi sử dụng chức năng này",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            "en" -> {
//                textToSpeech = TextToSpeech(this@MainActivity) { status: Int ->
//                    if (status != TextToSpeech.ERROR) {
//                        textToSpeech.language = Locale(s)
//                    } else {
//                        Toast.makeText(this@MainActivity, "Error when use this function", Toast.LENGTH_SHORT).show()
//                    }
//                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, s)
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something: ")
                try {
                    searcByvoice.launch(intent)
                } catch (ex: Exception) {
                    Toast.makeText(
                        this@ListOfflineActivity,
                        "Error when use this function",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        setStatusButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

}

