package com.ducdiep.playmusic.activities

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SlideAdapter
import com.ducdiep.playmusic.adapters.TopSongHoriAdapter
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.models.topsong.Song
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class HomeActivity : AppCompatActivity() {
    lateinit var songService:SongService
    lateinit var listTopSong:List<Song>
    var handler = Handler()
    var runSlide = Runnable {
        if (view_pager_slide.currentItem==4){
            view_pager_slide.setCurrentItem(0)
        }else{
            view_pager_slide.setCurrentItem(view_pager_slide.currentItem+1)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)
        songService = RetrofitInstance.getInstance().create(SongService::class.java)
        edt_search.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                btn_back.visibility = View.VISIBLE
                rcv_search.visibility = View.VISIBLE
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
        btn_back.setOnClickListener {
            btn_back.visibility = View.GONE
            edt_search.clearFocus()
            rcv_search.visibility = View.GONE
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
                Log.d("dataRes", "getTopSong: $dataRespone")
            }
        }
    }
    fun setUpSlide(){
        var list = listTopSong.take(5)
        var slideAdapter = SlideAdapter(this,list)
        slideAdapter.setOnClickItem {
            Toast.makeText(this, "${it.name}", Toast.LENGTH_SHORT).show()
        }
        view_pager_slide.adapter = slideAdapter
        circle_indicator.setViewPager(view_pager_slide)
        view_pager_slide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handler.removeCallbacks(runSlide)
                handler.postDelayed(runSlide,2000)

            }
        })
    }

    fun setupTopSongAdapter(){
        var topSongAdapter = TopSongHoriAdapter(this,listTopSong)
        rcv_top_songs.adapter = topSongAdapter
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runSlide)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runSlide,2000)
    }
}