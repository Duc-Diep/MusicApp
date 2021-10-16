package com.ducdiep.playmusic.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SlideAdapter
import com.ducdiep.playmusic.adapters.SongOnlineAdapter
import com.ducdiep.playmusic.adapters.TopSongHoriAdapter
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.models.search.SongSearch
import com.ducdiep.playmusic.models.topsong.Song
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.edt_search
import kotlinx.android.synthetic.main.activity_list_offline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class HomeActivity : AppCompatActivity() {
    lateinit var songService:SongService
    lateinit var songServiceSearch:SongService
    lateinit var listTopSong:List<Song>
    lateinit var listSearch:List<SongSearch>
    var handler = Handler()
    var runSlide = Runnable {
        if (view_pager_slide.currentItem==4){
            view_pager_slide.setCurrentItem(0, true)
        }else{
            view_pager_slide.setCurrentItem(view_pager_slide.currentItem + 1, true)
        }
    }
    var runSearch = Runnable {
        var key = edt_search.text.toString()
            GlobalScope.launch(Dispatchers.IO) {
                var response = songServiceSearch.getSearch("artist,song,key,code", 500, key).awaitResponse()
                if (response.isSuccessful){
                    var data = response.body()
                    withContext(Dispatchers.Main){
                        try {
                            listSearch = data?.data!![0].song
                            setupDataSearch(listSearch)
                            progressbar_search.visibility = View.GONE
                        }catch (ex:Exception){
                            Toast.makeText(
                                this@HomeActivity,
                                "Không tìm thấy bài hát này",
                                Toast.LENGTH_SHORT
                            ).show()
                            progress_bar.visibility = View.GONE
                        }
                    }
                }
            }


    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)
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
            var intent = Intent(this,ListOfflineActivity::class.java)
            startActivity(intent)
        }

        getTopSong()
    }

    private fun getTopSong() {
        GlobalScope.launch(Dispatchers.IO) {
            var response = songService.getTopSong().awaitResponse()
            if (response.isSuccessful){
                var dataRespone = response.body()
                withContext(Dispatchers.Main){
                    listTopSong = dataRespone?.data?.song!!
                    setUpSlide()
                    setupTopSongAdapter()
                }
//                Log.d("dataRes", "getTopSong: $dataRespone")
            }
        }
    }
    fun setUpSlide(){
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

    fun setupTopSongAdapter(){
        var topSongAdapter = TopSongHoriAdapter(this, listTopSong)
        topSongAdapter.setOnClickItem {
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        rcv_top_songs.adapter = topSongAdapter
    }

    private fun setupDataSearch(list: List<SongSearch>) {
        var songAdapter = SongOnlineAdapter(this, list)
        rcv_search.adapter = songAdapter
    }


    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runSlide)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runSlide, 2000)
    }
}