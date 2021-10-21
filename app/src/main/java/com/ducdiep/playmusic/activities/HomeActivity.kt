package com.ducdiep.playmusic.activities

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SlideAdapter
import com.ducdiep.playmusic.adapters.SongSearchAdapter
import com.ducdiep.playmusic.adapters.TopSongHoriAdapter
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.search.ResponseSearch
import com.ducdiep.playmusic.models.search.SongSearch
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.ResponseTopSong
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.btn_close
import kotlinx.android.synthetic.main.activity_home.btn_next
import kotlinx.android.synthetic.main.activity_home.btn_play_or_pause
import kotlinx.android.synthetic.main.activity_home.btn_previous
import kotlinx.android.synthetic.main.activity_home.edt_search
import kotlinx.android.synthetic.main.activity_home.img_song
import kotlinx.android.synthetic.main.activity_home.layout_playing
import kotlinx.android.synthetic.main.activity_home.layout_title
import kotlinx.android.synthetic.main.activity_home.tv_name
import kotlinx.android.synthetic.main.activity_home.tv_single
import kotlinx.android.synthetic.main.activity_list_offline.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {
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
    lateinit var songService: SongService
    lateinit var songServiceSearch: SongService
    lateinit var listTopSong: List<Song>
    lateinit var listSearch: List<SongSearch>
    private val searcByvoice =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                var hi = it.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                edt_search.setText(hi!![0])
            }
        }
    var handler = Handler()
    var runSlide = Runnable {
        if (view_pager_slide.currentItem == 4) {
            view_pager_slide.setCurrentItem(0, true)
        } else {
            view_pager_slide.setCurrentItem(view_pager_slide.currentItem + 1, true)
        }
    }

    //search
    var runSearch = Runnable {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(edt_search.windowToken, 0)
        var key = edt_search.text.toString()
        songServiceSearch.getSearch("artist,song,key,code", 500, key)
            .enqueue(object : Callback<ResponseSearch> {
                override fun onResponse(
                    call: Call<ResponseSearch>,
                    response: Response<ResponseSearch>
                ) {
                    if (response.isSuccessful) {
                        var data = response.body()
                        listSearch = data?.data!![0].song
                        setupDataSearch(listSearch)
                        progressbar_search.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ResponseSearch>, t: Throwable) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Không tìm thấy bài hát này",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressbar_search.visibility = View.GONE
                }

            })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)
        init()
        setClick()
        getTopSong()
    }

    private fun setClick() {
        edt_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                btn_back.visibility = View.VISIBLE
                rcv_search.visibility = View.VISIBLE
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == "") {
                    btn_back.visibility = View.GONE
                    handler.removeCallbacks(runSearch)
                } else {
                    progressbar_search.visibility = View.VISIBLE
                    handler.removeCallbacks(runSearch)
                    handler.postDelayed(runSearch, 1000)
                }

            }

        })
        btn_back.setOnClickListener {
            listSearch = listOf()
            setupDataSearch(listSearch)
            btn_back.visibility = View.GONE
            edt_search.clearFocus()
            edt_search.setText("")
            rcv_search.visibility = View.GONE
        }
        cv_on_phone.setOnClickListener {
            var intent = Intent(this, ListOfflineActivity::class.java)
            startActivity(intent)
        }
        img_favourite_home.setOnClickListener{
            var intent = Intent(this, FavouriteActivity::class.java)
            startActivity(intent)
        }
        btn_mic_home.setOnClickListener {
            showPopup(it)
        }

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
    }

    fun init(){
        AppPreferences.init(this)
        glide = Glide.with(this)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
        songService = RetrofitInstance.getInstance().create(SongService::class.java)
        songServiceSearch = RetrofitInstance.getInstanceSearch().create(SongService::class.java)
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
    fun setStatusButton() {
        if (AppPreferences.isPlaying) {
            btn_play_or_pause.setImageResource(R.drawable.pause)
        } else {
            btn_play_or_pause.setImageResource(R.drawable.play)
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
                        this@HomeActivity,
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
                        this@HomeActivity,
                        "Error when use this function",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun getTopSong() {
        songService.getTopSong().enqueue(object : Callback<ResponseTopSong> {
            override fun onResponse(
                call: Call<ResponseTopSong>,
                response: Response<ResponseTopSong>
            ) {
                if (response.isSuccessful) {
                    var dataRespone = response.body()
                    listTopSong = dataRespone?.data?.song!!
                    setUpSlide()
                    setupTopSongAdapter()
                }
            }

            override fun onFailure(call: Call<ResponseTopSong>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Lỗi khi tải bài hát", Toast.LENGTH_SHORT).show()
            }

        })

    }

    //slide
    fun setUpSlide() {
        var list = listTopSong.take(5)
        var slideAdapter = SlideAdapter(this, list)
        slideAdapter.setOnClickItem {
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        view_pager_slide.adapter = slideAdapter
        circle_indicator.setViewPager(view_pager_slide)
        view_pager_slide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handler.removeCallbacks(runSlide)
                handler.postDelayed(runSlide, 2000)
            }
        })
    }

    //adapter top 100 bai hat
    fun setupTopSongAdapter() {
        var topSongAdapter = TopSongHoriAdapter(this, listTopSong)
        topSongAdapter.setOnClickItem {
            listSongOnline = listTopSong as ArrayList<Song>
            AppPreferences.indexPlaying = it.position - 1
            AppPreferences.isOnline = true
            AppPreferences.isPlayRequireList = true
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
            sendActionToService(ACTION_START)
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        rcv_top_songs.adapter = topSongAdapter
    }

    private fun setupDataSearch(list: List<SongSearch>) {
        var songAdapter = SongSearchAdapter(this, list)
        songAdapter.setOnClickItem {
            listSongOnline.clear()
            listSongOnline.add(
                Song(
                    it.artist,
                    it.id,
                    it.duration.toInt(),
                    it.id,
                    it.name,
                    0,
                    "$URL_THUMB${it.thumb}",
                    "audio"
                )
            )
            AppPreferences.indexPlaying = 0
            AppPreferences.isOnline = true
            AppPreferences.isPlayRequireList = false
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
            sendActionToService(ACTION_START)
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        rcv_search.adapter = songAdapter
    }

    fun sendActionToService(action: Int) {
        var intent = Intent(this, MusicService::class.java)
        intent.putExtra(ACTION_TO_SERVICE, action)
        startService(intent)
    }


    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runSlide)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runSlide, 2000)
    }

    override fun onBackPressed() {
        if (btn_back.visibility == View.VISIBLE) {
            listSearch = listOf()
            setupDataSearch(listSearch)
            btn_back.visibility = View.GONE
            edt_search.clearFocus()
            edt_search.setText("")
            rcv_search.visibility = View.GONE
        } else {
            AlertDialog.Builder(this).setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn thoát app không?")
                .setPositiveButton("Có"
                ) { dialog, which -> finish() }
                .setNegativeButton("Không"
                ) { dialog, which -> }
                .show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}