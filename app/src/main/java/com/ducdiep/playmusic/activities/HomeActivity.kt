package com.ducdiep.playmusic.activities

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
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
import androidx.lifecycle.ViewModelProvider
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
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongFavourite
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.search.ResponseSearch
import com.ducdiep.playmusic.models.search.SongSearch
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.ResponseTopSong
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import com.ducdiep.playmusic.viewmodel.HandleViewModel
import com.ducdiep.playmusic.viewmodel.HomeViewModel
import kotlinx.android.synthetic.main.activity_favourite.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.btn_close
import kotlinx.android.synthetic.main.activity_home.btn_next
import kotlinx.android.synthetic.main.activity_home.btn_play_or_pause
import kotlinx.android.synthetic.main.activity_home.btn_previous
import kotlinx.android.synthetic.main.activity_home.edt_search
import kotlinx.android.synthetic.main.activity_home.img_song
import kotlinx.android.synthetic.main.activity_home.layout_playing
import kotlinx.android.synthetic.main.activity_home.layout_search
import kotlinx.android.synthetic.main.activity_home.layout_title
import kotlinx.android.synthetic.main.activity_home.tv_name
import kotlinx.android.synthetic.main.activity_home.tv_single
import kotlinx.android.synthetic.main.activity_list_offline.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    lateinit var handleViewModel: HandleViewModel
    lateinit var homeViewModel: HomeViewModel
    lateinit var glide: RequestManager
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
        var key = edt_search.text.toString()
        homeViewModel.fetchListSearch(key)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)
        init()
        setClick()
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
                    rcv_search.visibility = View.GONE
                    progressbar_search.visibility = View.GONE
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
        img_favourite_home.setOnClickListener {
            var intent = Intent(this, FavouriteActivity::class.java)
            startActivity(intent)
        }
        btn_mic_home.setOnClickListener {
            showPopup(it)
        }

        if (AppPreferences.indexPlaying != -1) {
            handleViewModel.handleLayoutPlay(ACTION_START)
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

    fun loadData(check: Boolean?) {
        if (check == true) {
            layout_search.visibility = View.VISIBLE
            layout_slide.visibility = View.VISIBLE
            layout_title_top_song.visibility = View.VISIBLE
            rcv_top_songs.visibility = View.VISIBLE
            getTopSong()
        } else {
            layout_search.visibility = View.GONE
            layout_slide.visibility = View.GONE
            layout_title_top_song.visibility = View.GONE
            rcv_top_songs.visibility = View.GONE
        }
    }

    fun init() {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.isNetworkAvailble.observe(this, { t ->
            loadData(t)
        })

        homeViewModel.repoListSearch.observe(this, { t ->
            setupDataSearch(t)
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(edt_search.windowToken, 0)
        })
        homeViewModel.repoListTopSong.observe(this, { t ->
            listTopSong = t
            setUpSlide()
            setupTopSongAdapter()
        })

        homeViewModel.dataLoading.observe(this, { t ->
            if (t == true) {
                progressbar_search.visibility = View.VISIBLE
            } else {
                progressbar_search.visibility = View.GONE
            }
        })


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
            { t -> showMusicFavourite(t) })
        AppPreferences.init(this)
        glide = Glide.with(this)

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

    private fun showMusicFavourite(t: SongFavourite?) {
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
        homeViewModel.fetchListTopSong()
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
            AppPreferences.isPlayFavouriteList = false
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
            handleViewModel.startMusic()
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
            AppPreferences.isPlayFavouriteList = false
            var intent = Intent(this, PlayMusicActivity::class.java)
            startActivity(intent)
            handleViewModel.startMusic()
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        rcv_search.adapter = songAdapter
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runSlide)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runSlide, 2000)
        if (AppPreferences.indexPlaying != -1) {
            handleViewModel.handleLayoutPlay(ACTION_START)
        }

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
                .setPositiveButton(
                    "Có"
                ) { dialog, which -> finish() }
                .setNegativeButton(
                    "Không"
                ) { dialog, which -> }
                .show()
        }
    }
}