package com.ducdiep.playmusic.activities

import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.topsong.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_play_music.*

class PlayMusicActivity : AppCompatActivity() {
    //set up bound service
    private lateinit var mService: MusicService
    private var mBound: Boolean = false
    var currentPos: Int = 0
    lateinit var handler: Handler
    lateinit var runnable: Runnable

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
        glide = Glide.with(this)
        setStatusButton()
        showDetailMusic()

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
            }
            ACTION_PAUSE -> pauseMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_NEXT -> {
                showDetailMusic()
                seekbar_handle.progress = 0
            }
            ACTION_PREVIOUS -> {
                showDetailMusic()
                seekbar_handle.progress = 0
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
        if (AppPreferences.isOnline){
            mSongOnline = listSongOnline[AppPreferences.indexPlaying]
            glide.load(mSongOnline.thumbnail).into(img_music)
            tv_song_name.text = mSongOnline.name
            tv_song_name.isSelected = true
            tv_artist.text = mSongOnline.artists_names
            tv_artist.isSelected = true
            img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
        }else{
            mSongOffline = listSongOffline[AppPreferences.indexPlaying]
            img_music.setImageBitmap(mSongOffline.imageBitmap)
            tv_song_name.text = mSongOffline.name
            tv_song_name.isSelected = true
            tv_artist.text = mSongOffline.artist
            tv_artist.isSelected = true
            img_music.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_image))
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
            duration = mSongOffline.duration
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