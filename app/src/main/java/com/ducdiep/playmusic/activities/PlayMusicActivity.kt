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
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_play_music.*
import kotlinx.android.synthetic.main.activity_play_music.btn_back
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayMusicActivity : AppCompatActivity() {
    //set up bound service
    private lateinit var mService: MusicService
    private var mBound: Boolean = false
    var currentPos: Int = 0
    lateinit var listRecommend:List<Song>
    lateinit var songService: SongService
    lateinit var handler: Handler
    lateinit var runnable: Runnable
    lateinit var sqlHelper:SqlHelper
    var isPrevious = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            mService = binder.getService()
            setProgress()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            handler.removeCallbacks(runnable)
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    //variables
    lateinit var mSongOffline: SongOffline
    lateinit var mSongOnline: Song
    lateinit var mSongFavourite: SongFavourite
    lateinit var glide:RequestManager
    lateinit var anim: ObjectAnimator
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            var action = bundle.getInt(ACTION)
            currentPos = bundle.getInt(CURRENT_POSITION)
            handleLayoutPlay(action)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)
        supportActionBar?.hide()
        AppPreferences.init(this)
        songService = RetrofitInstance.getInstance()!!
        glide = Glide.with(this)
        sqlHelper = SqlHelper(this)
        setStatusButton()
        showDetailMusic()
        setupRecommendSong()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))
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


        //set click button
        btn_back.setOnClickListener {
            finish()
        }
        btn_handle_play_or_pause.setOnClickListener {
            if (AppPreferences.isPlaying) {
                pauseMusic()
            } else {
                resumeMusic()
            }
        }

        btn_handle_next.setOnClickListener {
            mService.playNextMusic()
        }
        btn_handle_previous.setOnClickListener {
            mService.playPreviousMusic()
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
            if (AppPreferences.isOnline){
                if (sqlHelper.checkExists(mSongOnline.id)){
                    sqlHelper.removeSong(mSongOnline.id)
                    img_favourite.setImageResource(R.drawable.unlike)
                    Toast.makeText(this, "Đã xóa bài hát khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show()
                }else{
                    sqlHelper.addSong(mSongOnline)
                    Toast.makeText(this, "Đã thêm bài hát vào danh sách yêu thích", Toast.LENGTH_SHORT).show()
                    img_favourite.setImageResource(R.drawable.like)
                }
            }else{
                if (sqlHelper.checkExistsOff(mSongOffline.resource)){
                    sqlHelper.removeSongOff(mSongOffline.resource)
                    img_favourite.setImageResource(R.drawable.unlike)
                    Toast.makeText(this, "Đã xóa bài hát khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show()
                }else{
                    sqlHelper.addSongOff(mSongOffline)
                    Toast.makeText(this, "Đã thêm bài hát off vào danh sách yêu thích", Toast.LENGTH_SHORT).show()
                    img_favourite.setImageResource(R.drawable.like)
                }
            }
        }
    }

    private fun downloadMusic() {
        if(AppPreferences.isOnline){
            var url = "http://api.mp3.zing.vn/api/streaming/${mSongOnline.type}/${mSongOnline.id}/320"
            var uri = Uri.parse(url)
            var request = DownloadManager.Request(uri)
            var title = "${mSongOnline.name}.mp3"
            request.setTitle(title)
            request.setDescription("Đang tải vui lòng đợi")
            var cookie = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie",cookie)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title)
            var downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "Bắt đầu tải xuống", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecommendSong() {
        if (AppPreferences.isOnline){
            progress_bar_play.visibility = View.VISIBLE
            songService.getRecommend(mSongOnline.type,mSongOnline.id).enqueue(object :
                Callback<ResponseRecommend> {
                override fun onResponse(
                    call: Call<ResponseRecommend>,
                    response: Response<ResponseRecommend>
                ) {
                    if (response.isSuccessful) {
                        var data = response.body()
                        listRecommend = data!!.data.items
                        if (!listSongOnline.contains(listRecommend[0])&&!isPrevious&&!AppPreferences.isPlayRequireList) {
                            listSongOnline.add(listRecommend[0])
                            isPrevious = false
                        }

                        setupRecommendSongAdapter()
                        progress_bar_play.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ResponseRecommend>, t: Throwable) {
                    Toast.makeText(this@PlayMusicActivity, "Có lỗi khi tải nhạc", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    private fun setupRecommendSongAdapter() {

        var songOnlineAdapter = SongOnlineAdapter(this,listRecommend)
        songOnlineAdapter.setOnClickItem {
            listSongOnline.removeLastOrNull()
            listSongOnline.add(it)
            AppPreferences.indexPlaying = listSongOnline.size-1
            mService.handleMusic(ACTION_START)
        }
        rcv_recommend_songs.adapter = songOnlineAdapter
    }

    private fun resumeMusic() {
        mService.mediaPlayer?.start()
        AppPreferences.isPlaying = true
        mService.sendNotificationMedia(AppPreferences.indexPlaying)
        AppPreferences.isPlaying = true
        setStatusButton()
        anim.resume()
    }

    private fun pauseMusic() {
//        seekbar_handle.progress = (currentPos/1000)
//        mService.mediaPlayer?.seekTo(currentPos)
        mService.mediaPlayer?.pause()
        AppPreferences.isPlaying = false
        mService.sendNotificationMedia(AppPreferences.indexPlaying)
        AppPreferences.isPlaying = false
        setStatusButton()
        anim.pause()
    }

    private fun handleLayoutPlay(action: Int) {
        when (action) {
            ACTION_START -> {
                showDetailMusic()
                setStatusButton()
                if (mBound) {
                    setProgress()
                }
                setupRecommendSong()
            }
            ACTION_PAUSE -> pauseMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_NEXT -> {
//                showDetailMusic()
                seekbar_handle.progress = 0
//                setupRecommendSong()
            }
            ACTION_PREVIOUS -> {
                isPrevious = true
//                showDetailMusic()
                seekbar_handle.progress = 0
//                setupRecommendSong()
            }
            ACTION_CLEAR -> {
                handler.removeCallbacks(runnable)
                reloadData()
                finish()
            }

        }
    }

    private fun setStatusButton() {
        if (AppPreferences.isPlaying) {
            btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_pause_circle_outline_50)
        } else {
            btn_handle_play_or_pause.setImageResource(R.drawable.ic_baseline_play_circle_outline_50)
        }
    }

    fun showDetailMusic() {
        if (AppPreferences.isPlayFavouriteList){
            mSongFavourite = listSongFavourite[AppPreferences.indexPlaying]
            if (mSongFavourite.url==""){//online
                var linkImage = mSongFavourite.thumbnail.replaceFirst("w94_r1x1_jpeg/","")
                glide.load(linkImage).into(img_music)
                tv_song_name.text = mSongFavourite.name
                tv_song_name.isSelected = true
                songService.getSongInfor(mSongFavourite.type,mSongFavourite.id).enqueue(object :Callback<ResponseInfor>{
                    override fun onResponse(
                        call: Call<ResponseInfor>,
                        response: Response<ResponseInfor>
                    ) {
                        if (response.isSuccessful){
                            var data = response.body()
                            if (data!!.data.genres.size>1){
                                tv_artist.text ="${mSongFavourite.artists_names}   ${data.data.genres[1].name}"
                                tv_artist.isSelected = true
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseInfor>, t: Throwable) {

                    }

                })

                img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
                if (sqlHelper.checkExists(mSongFavourite.id)){
                    img_favourite.setImageResource(R.drawable.like)
                }else{
                    img_favourite.setImageResource(R.drawable.unlike)
                }
            }else{//offline
                img_music.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.musical_default))
                tv_song_name.text = mSongFavourite.name
                tv_song_name.isSelected = true
                tv_artist.text = "${mSongFavourite.artists_names}"

                if (sqlHelper.checkExistsOff(mSongFavourite.url)){
                    img_favourite.setImageResource(R.drawable.like)
                }else{
                    img_favourite.setImageResource(R.drawable.unlike)

                }
                tv_artist.isSelected = true
                img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
            }
        }else{
            if (AppPreferences.isOnline){
                mSongOnline = listSongOnline[AppPreferences.indexPlaying]
                var linkImage = mSongOnline.thumbnail.replaceFirst("w94_r1x1_jpeg/","")
                glide.load(linkImage).into(img_music)
                tv_song_name.text = mSongOnline.name
                tv_song_name.isSelected = true
                songService.getSongInfor(mSongOnline.type,mSongOnline.id).enqueue(object :Callback<ResponseInfor>{
                    override fun onResponse(
                        call: Call<ResponseInfor>,
                        response: Response<ResponseInfor>
                    ) {
                        if (response.isSuccessful){
                            var data = response.body()
                            if (data!!.data.genres.size>1){
                                tv_artist.text ="${mSongOnline.artists_names}   ${data.data.genres[1].name}"
                                tv_artist.isSelected = true
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseInfor>, t: Throwable) {

                    }

                })

                img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
                if (sqlHelper.checkExists(mSongOnline.id)){
                    img_favourite.setImageResource(R.drawable.like)
                }else{
                    img_favourite.setImageResource(R.drawable.unlike)
                }

            }else{
                mSongOffline = listSongOffline[AppPreferences.indexPlaying]
                img_music.setImageBitmap(mSongOffline.imageBitmap)
                tv_song_name.text = mSongOffline.name
                tv_song_name.isSelected = true
                if (mSongOffline.genres!="null"){
                    tv_artist.text = "${mSongOffline.artist}   ${mSongOffline.genres}"
                }else{
                    tv_artist.text = "${mSongOffline.artist}"
                }

                if (sqlHelper.checkExistsOff(mSongOffline.resource)){
                    img_favourite.setImageResource(R.drawable.like)
                }else{
                    img_favourite.setImageResource(R.drawable.unlike)

                }
                tv_artist.isSelected = true
                img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
            }
        }


    }


    //set up progressbar
    fun setProgress() {
        var duration:Long
        if(AppPreferences.isOnline){
            mSongOnline = listSongOnline[AppPreferences.indexPlaying]
            duration = (mSongOnline.duration*1000).toLong()
        }else{
            mSongOffline = listSongOffline[AppPreferences.indexPlaying]
            duration = mSongOffline.duration.toLong()
        }
        tv_progress.text = timerConversion(currentPos.toLong())
        tv_duration.text = timerConversion(duration)
        seekbar_handle.max = duration.toInt()
        handler = Handler(mainLooper)

        runnable = object : Runnable {
            override fun run() {
                try {
                    tv_progress.text =
                        timerConversion(mService.mediaPlayer?.currentPosition!!.toLong())
                    seekbar_handle.progress = mService.mediaPlayer?.currentPosition!!
                    handler.postDelayed(this, 1000)

                } catch (ex: Exception) {

                }
            }
        }
        handler.postDelayed(runnable, 1000)
        seekbar_handle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mService.mediaPlayer!!.seekTo(seekBar.progress)
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBackPressed() {
//        val taskInfo = activityManager.appTasks
//        if (taskInfo[0].taskInfo.numActivities >2) {
//            super.onBackPressed()
//        } else {
//            var intent = Intent(this, ListOfflineActivity::class.java)
//            intent.putExtra(ACTION_RELOAD, 1)
//            startActivity(intent)
//            finish()
//        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}