package com.ducdiep.playmusic.activities

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SlideAdapter
import com.ducdiep.playmusic.adapters.SongSearchAdapter
import com.ducdiep.playmusic.adapters.TopSongHoriAdapter
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.ACTION_START
import com.ducdiep.playmusic.config.ACTION_TO_SERVICE
import com.ducdiep.playmusic.config.URL_THUMB
import com.ducdiep.playmusic.models.search.ResponseSearch
import com.ducdiep.playmusic.models.search.SongSearch
import com.ducdiep.playmusic.models.topsong.ResponseTopSong
import com.ducdiep.playmusic.models.topsong.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.edt_search
import kotlinx.android.synthetic.main.activity_list_offline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

class HomeActivity : AppCompatActivity() {
    lateinit var songService: SongService
    lateinit var songServiceSearch: SongService
    lateinit var listTopSong: List<Song>
    lateinit var listSearch: List<SongSearch>
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
                    progress_bar.visibility = View.GONE
                }

            })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)
        AppPreferences.init(this)
        songService = RetrofitInstance.getInstance().create(SongService::class.java)
        songServiceSearch = RetrofitInstance.getInstanceSearch().create(SongService::class.java)
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

        getTopSong()
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
                    it.name,
                    "audio"
                )
            )
            AppPreferences.indexPlaying = 0
            AppPreferences.isOnline = true
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
}