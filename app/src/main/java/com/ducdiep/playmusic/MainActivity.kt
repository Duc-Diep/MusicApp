package com.ducdiep.playmusic

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.adapters.SongAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    lateinit var mSong: Song
    var isPlaying: Boolean = false
    lateinit var listSong: ArrayList<Song>
    lateinit var songAdapter:SongAdapter
    lateinit var bitmapDefault1: Bitmap
    lateinit var bitmapDefault2: Bitmap
    lateinit var imageCurrentSong:Bitmap


    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            mSong = bundle.get(SONG_OBJECT) as Song
            imageCurrentSong = BitmapFactory.decodeResource(resources,mSong.image)
            isPlaying = bundle.getBoolean(STATUS_PLAY)
            var action = bundle.getInt(ACTION)
            handleLayoutPlay(action)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppPreferences.init(this)
        bitmapDefault1 = BitmapFactory.decodeResource(resources, R.drawable.mayu)
        bitmapDefault2 = BitmapFactory.decodeResource(resources, R.drawable.tohka)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
//        btn_start_service.setOnClickListener {
//            playMusic()
//        }
//        btn_stop_service.setOnClickListener {
//            stopMusic()
//        }
        btn_play_or_pause.setOnClickListener {
            if (isPlaying) {
                sendActionToService(ACTION_PAUSE)
            } else {
                sendActionToService(ACTION_RESUME)
            }
        }
        btn_close.setOnClickListener {
            sendActionToService(ACTION_CLEAR)
        }
        btn_next.setOnClickListener {
            if (AppPreferences.indexPlaying==listSong.size-1){
                playMusic(listSong[0])
                AppPreferences.indexPlaying = 0
            }else{
                playMusic(listSong[AppPreferences.indexPlaying+1])
                AppPreferences.indexPlaying = AppPreferences.indexPlaying+1
            }

        }
        btn_previous.setOnClickListener {
            if (AppPreferences.indexPlaying==0){
                playMusic(listSong[listSong.size-1])
                AppPreferences.indexPlaying = listSong.size-1
            }else{
                playMusic(listSong[AppPreferences.indexPlaying-1])
                AppPreferences.indexPlaying = AppPreferences.indexPlaying-1
            }

        }
        loadDefaultMusic()
        requestPermisssion()
        songAdapter = SongAdapter(this,listSong)
        songAdapter.setOnClickItem {
            playMusic(it)
            AppPreferences.indexPlaying = listSong.indexOf(it)
        }
        rcv_songs.adapter = songAdapter
    }

    private fun loadDefaultMusic() {
        listSong = ArrayList()
        listSong.apply {
            add(Song("Key of truth", "Sweet Arms", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.key_of_truth))
            add(Song("Date a live", "Sweet Arms", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.date_a_live_spirit_pledge))
            add(Song("Dramma", "МиМиМи (Mimimi)", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.dramma))
            add(Song("EDM", "Đức Điệp", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.edm))
            add(Song("Ichinen Nikagetsu Hatsuka", "BRIGHT", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.ichinen_nikagetsu_hatsuka))
            add(Song("On My Own", "Ashed Remain", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.on_my_own))
            add(Song("Sold out", "Official Lyric Video", R.drawable.mayu, bitmapDefault1,"android.resource://com.ducdiep.playmusic/" + R.raw.sold_out))
            add(Song("Summertime", "Cinnamons, Evening Cinema", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.summertime))
            add(Song("Where We Started", "Jex", R.drawable.mayu, bitmapDefault1,"android.resource://com.ducdiep.playmusic/" + R.raw.where_we_started))
            add(Song("Xomu Lantern", "Miyuri Remix", R.drawable.mayu,bitmapDefault1, "android.resource://com.ducdiep.playmusic/" + R.raw.xomu_lantern))
        }
    }

    private fun stopMusic() {
        var intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }

    private fun playMusic(song:Song) {
        var intent = Intent(this, MusicService::class.java)
        var bundle = Bundle()
        bundle.putSerializable(
            SONG_OBJECT,
            song
        )
//        bundle.putParcelable(IMAGE,song.image)
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
        img_song.setImageBitmap(imageCurrentSong)
        tv_name.text = mSong.name
        tv_single.text = mSong.artist
    }

    fun setStatusButton() {
        if (isPlaying) {
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
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST
                )
            }
        } else {
            getAudio()
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
                    getAudio()
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

    private fun getAudio() {
        val contentResolver = contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val name: String =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val artist: String =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val url: String =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                val bitmap = BitmapFactory.decodeFile(url)

                if (!url.contains(".ogg")){
                    var song: Song
                    if (bitmap == null) {
                        song = Song(name, artist, R.drawable.tohka,bitmapDefault2, url)
                    } else {

                        song = Song(name, artist, R.drawable.tohka,bitmapDefault2, url)
                    }
                    listSong.add(song)
                }
            } while (cursor.moveToNext())
        }
        rcv_songs.adapter?.notifyDataSetChanged()
    }
}

