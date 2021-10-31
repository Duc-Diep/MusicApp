package com.ducdiep.playmusic.activities

import android.animation.ObjectAnimator
import android.app.DownloadManager
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.webkit.CookieManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.adapters.SongOnlineAdapter
import com.ducdiep.playmusic.api.RetrofitInstance
import com.ducdiep.playmusic.api.SongService
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongFavourite
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.helpers.SqlHelper
import com.ducdiep.playmusic.models.getgenres.ResponseInfor
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.ResponseRecommend
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.services.MusicService
import com.ducdiep.playmusic.viewmodel.PlayViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_play_music.*
import kotlinx.android.synthetic.main.activity_play_music.btn_back
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayMusicActivity : AppCompatActivity() {
    lateinit var playViewModel: PlayViewModel
    var duration: Long = 0
    lateinit var sqlHelper: SqlHelper
    lateinit var glide: RequestManager
    lateinit var anim: ObjectAnimator


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)
        supportActionBar?.hide()
        init()
        setClick()
    }

    private fun setClick() {
        btn_back.setOnClickListener {
            finish()
        }
        btn_handle_play_or_pause.setOnClickListener {
            playViewModel.onClickPlayOrPause()
        }

        btn_handle_next.setOnClickListener {
            playViewModel.onClickNext()
        }
        btn_handle_previous.setOnClickListener {
            playViewModel.onClickPrevious()
        }
        btn_handle_shuffle.setOnClickListener {
            if (!AppPreferences.isShuffle) {
                btn_handle_shuffle.setColorFilter(Color.rgb(0, 255, 255))
                AppPreferences.isShuffle = true
                Toast.makeText(this, "Chế độ phát ngẫu nhiên được bật", Toast.LENGTH_SHORT).show()
            } else {
                btn_handle_shuffle.setColorFilter(Color.WHITE)
                AppPreferences.isShuffle = false
                Toast.makeText(this, "Chế độ phát ngẫu nhiên đã tắt", Toast.LENGTH_SHORT).show()
            }
        }

        btn_handle_repeat.setColorFilter(Color.rgb(0, 255, 255))
        btn_handle_repeat.setOnClickListener {
            if (!AppPreferences.isRepeatOne) {
                btn_handle_repeat.setImageResource(R.drawable.ic_baseline_repeat_one_50)
                AppPreferences.isRepeatOne = true
                Toast.makeText(this, "Chế độ phát 1 bài được bật", Toast.LENGTH_SHORT).show()
            } else {
                btn_handle_repeat.setImageResource(R.drawable.ic_baseline_repeat_50)
                AppPreferences.isRepeatOne = false
                Toast.makeText(this, "Chế độ phát 1 bài đã tắt", Toast.LENGTH_SHORT).show()
            }
        }
        btn_download.setOnClickListener {
            downloadMusic()
        }
        img_favourite.setOnClickListener {
            playViewModel.addOrRemoveFavourite()
            if (playViewModel.isFavourite.value==true){
                Toast.makeText(this, "Bạn đã thêm bài hát vào danh sách yêu thích", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Bạn đã xóa bài hát khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show()

            }
        }
    }

    fun init() {
        playViewModel = ViewModelProvider(this).get(PlayViewModel::class.java)
        playViewModel.isPlaying.observe(this, {
            if (it == true) {
                btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_pause_circle_outline_50)
            } else {
                btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_play_circle_outline_50)
            }
        })
        playViewModel.mSongOffline.observe(this,
            { showMusicOffline(it) })
        playViewModel.mSongOnline.observe(this,
            { showMusicOnline(it) })
        playViewModel.mSongFavourite.observe(this,
            { showMusicFavourite(it) })
        playViewModel.currentPosition.observe(this, {
            setupProgress(it)
        })
        playViewModel.dataLoading.observe(this, {
            if (it == true) {
                progress_bar_play.visibility = View.VISIBLE
            } else {
                progress_bar_play.visibility = View.GONE
            }
        })

        playViewModel.repoListRecommend.observe(this, {
            setupRecommendSongAdapter(it)
        })

        playViewModel.genres.observe(this, {
            tv_artist.text = "${tv_artist.text}   $it"
            tv_artist.isSelected = true
        })
        playViewModel.isFavourite.observe(this){
            if (it==true){
                img_favourite.setImageResource(R.drawable.like)
            }else{
                img_favourite.setImageResource(R.drawable.unlike)
            }
        }
        playViewModel.isDestroy.observe(this,{
            if (it==true){
                finish()
            }
        })

        playViewModel.handleLayoutPlay(ACTION_START)
        setupProgress(0)

        AppPreferences.init(this)
        glide = Glide.with(this)
        sqlHelper = SqlHelper(this)
        if (AppPreferences.isShuffle) {
            btn_handle_shuffle.setColorFilter(Color.rgb(0, 255, 255))
        }
        if (AppPreferences.isRepeatOne) {
            btn_handle_repeat.setImageResource(R.drawable.ic_baseline_repeat_one_50)
        }

        anim = ObjectAnimator.ofFloat(img_music, "rotation", 0f, 360f)
        anim.interpolator = LinearInterpolator()
        anim.duration = 20000
        anim.repeatCount = 1000
        anim.repeatMode = ObjectAnimator.RESTART
        anim.start()
    }

    private fun downloadMusic() {
        if (AppPreferences.isOnline) {
            playViewModel.downloadMusic()
            Toast.makeText(this, "Bắt đầu tải xuống", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecommendSongAdapter(listRecommend: List<Song>) {

        var songOnlineAdapter = SongOnlineAdapter(this, listRecommend)
        songOnlineAdapter.setOnClickItem {
            listSongOnline.removeLastOrNull()
            listSongOnline.add(it)
            AppPreferences.indexPlaying = listSongOnline.size - 1
            playViewModel.startMusic()
        }
        rcv_recommend_songs.adapter = songOnlineAdapter
    }

    private fun resumeMusic() {
        playViewModel.resumeMusic()
        AppPreferences.isPlaying = true
        anim.resume()
    }

    private fun pauseMusic() {
        playViewModel.pauseMusic()
        AppPreferences.isPlaying = false
        anim.pause()
    }

    fun showMusicOnline(s: Song) {
        duration = (s.duration * 1000).toLong()
        var linkImage = s.thumbnail.replaceFirst("w94_r1x1_jpeg/", "")
        glide.load(linkImage).into(img_music)
        tv_song_name.text = s.name
        tv_song_name.isSelected = true
        tv_artist.text = "${s.artists_names}"
        tv_artist.isSelected = true
        setupProgress(0)
        img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
    }

    fun showMusicOffline(s: SongOffline) {
        duration = s.duration.toLong()
        img_music.setImageBitmap(s.imageBitmap)
        tv_song_name.text = s.name
        tv_song_name.isSelected = true
        if (s.genres != "null") {
            tv_artist.text = "${s.artist}   ${s.genres}"
        } else {
            tv_artist.text = "${s.artist}"
        }
        setupProgress(0)
        tv_artist.isSelected = true
        img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
    }

    fun showMusicFavourite(s: SongFavourite) {

        if (s.url == "") {//online
            duration = (s.duration * 1000).toLong()
            var linkImage = s.thumbnail.replaceFirst("w94_r1x1_jpeg/", "")
            glide.load(linkImage).into(img_music)
            tv_song_name.text = s.name
            tv_song_name.isSelected = true
            tv_artist.text = "${s.artists_names}"
            tv_artist.isSelected = true
            img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
        } else {//offline
            duration = s.duration.toLong()
            img_music.setImageBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.musical_default
                )
            )
            tv_song_name.text = s.name
            tv_song_name.isSelected = true
            tv_artist.text = "${s.artists_names}"
            tv_artist.isSelected = true
            img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
        }
        setupProgress(0)
    }


    //    set up progressbar
    fun setupProgress(t: Int) {
        tv_duration.text = timerConversion(duration)
        tv_progress.text = timerConversion(t.toLong())
        seekbar_handle.progress = t
        seekbar_handle.max = duration.toInt()
        seekbar_handle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                playViewModel.onSeekbarChange(seekBar.progress)
            }

        })
    }

    fun timerConversion(value: Long): String {
        val audioTime: String
        val dur = value.toInt()
        val hrs = dur / 3600000
        val mns = dur / 60000 % 60000
        val scs = dur % 60000 / 1000
        audioTime = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mns, scs)
        } else {
            String.format("%02d:%02d", mns, scs)
        }
        return audioTime
    }



}