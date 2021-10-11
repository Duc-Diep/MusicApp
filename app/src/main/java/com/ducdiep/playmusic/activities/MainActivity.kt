package com.ducdiep.playmusic.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SongAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var mSong: Song
    lateinit var listSong: ArrayList<Song>
    lateinit var songAdapter:SongAdapter
    private val searcByvoice =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == RESULT_OK&&it.data!=null){
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppPreferences.init(this)
        supportActionBar?.hide()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        listSong = loadDefaultMusic(this)
        setupAdapter()
        requestPermisssion()
        var actionReload = intent.getIntExtra(ACTION_RELOAD, 0)
        if (actionReload==1){
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
            var intentService = Intent(this, MusicService::class.java)
            intentService.putExtra(ACTION_TO_SERVICE, ACTION_NEXT)
            startService(intentService)

        }
        btn_previous.setOnClickListener {
            var intentService = Intent(this, MusicService::class.java)
            intentService.putExtra(ACTION_TO_SERVICE, ACTION_PREVIOUS)
            startService(intentService)
        }
        layout_title.setOnClickListener{
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
                var listTemp = ArrayList<Song>()
                for (element in listSong) {
                    if (element.name.toLowerCase()
                            .contains(s.toString().toLowerCase()) || element.artist.toLowerCase()
                            .contains(
                                s.toString().toLowerCase()
                            )
                    ) {
                        listTemp.add(element)
                    }
                }
                songAdapter = SongAdapter(this@MainActivity, listTemp)
                songAdapter.setOnClickItem {
                    AppPreferences.indexPlaying = listSong.indexOf(it)
                    playMusic()
                    var intent = Intent(this@MainActivity, PlayMusicActivity::class.java)
                    startActivity(intent)
                }
                rcv_songs.adapter = songAdapter
            }

        })

        btn_mic.setOnClickListener {
            showPopup(it)
        }

    }


    private fun setupAdapter() {
        songAdapter = SongAdapter(this, listSong)
        songAdapter.setOnClickItem {
            AppPreferences.indexPlaying = listSong.indexOf(it)
            playMusic()
            var intent = Intent(this, PlayMusicActivity::class.java)

            startActivity(intent)

        }
        rcv_songs.adapter = songAdapter
    }

    private fun playMusic() {
        var intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, ACTION_START)
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
            ACTION_NEXT -> showDetailMusic()
            ACTION_PREVIOUS -> showDetailMusic()
        }
    }

    fun showDetailMusic() {
        mSong = listSong[AppPreferences.indexPlaying]
        img_song.setImageBitmap(mSong.imageBitmap)
        tv_name.text = mSong.name
        tv_single.text = mSong.artist
    }

    fun setStatusButton() {
        if (AppPreferences.isPlaying) {
            btn_play_or_pause.setImageResource(R.drawable.pause)
        } else {
            btn_play_or_pause.setImageResource(R.drawable.play)

        }
    }

    fun sendActionToService(action: Int) {
        var intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, action)
        startService(intent)
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
        when(s){
            "vi"->{
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, s)
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó đi: ")
                try {
                    searcByvoice.launch(intent)
                }catch (ex:Exception){
                    Toast.makeText(this@MainActivity, "Gặp lỗi khi sử dụng chức năng này", Toast.LENGTH_SHORT).show()
                }
            }
            "en"->{
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
                }catch (ex:Exception){
                    Toast.makeText(this@MainActivity, "Error when use this function", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }



    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }


    fun requestPermisssion() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST
                )
            }
        } else {
            listSong.addAll(getAudio(this))
            rcv_songs.adapter?.notifyDataSetChanged()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Access permission read external success",
                        Toast.LENGTH_SHORT
                    ).show()
                    listSong.addAll(getAudio(this))
                    rcv_songs.adapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        this,
                        "Access permission read external denied",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        setStatusButton()
    }


}

