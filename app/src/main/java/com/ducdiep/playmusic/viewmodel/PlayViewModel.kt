package com.ducdiep.playmusic.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongFavourite
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOnline
import com.ducdiep.playmusic.config.*
import com.ducdiep.playmusic.helpers.SqlHelper
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.repository.MusicRepository
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_play_music.*

class PlayViewModel(application: Application) : AndroidViewModel(application) {
    var context: Context = getApplication<Application>().applicationContext
    private lateinit var mService: MusicService
    var sqlHelper: SqlHelper = SqlHelper(context)
    var mBound = MutableLiveData<Boolean>()
    var isPlaying = MutableLiveData<Boolean>()
    var repoListRecommend = MutableLiveData<List<Song>>()
    var mSongFavourite = MutableLiveData<SongFavourite>()
    var mSongOnline = MutableLiveData<Song>()
    var mSongOffline = MutableLiveData<SongOffline>()
    var genres = MutableLiveData<String>()
    var currentPosition = MutableLiveData<Int>()
    var dataLoading = MutableLiveData<Boolean>()
    var isFavourite = MutableLiveData<Boolean>()
    var isPrevious = false

    //bound service
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            mService = binder.getService()
//            setProgress()
            mBound.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
//            handler.removeCallbacks(runnable)
            mBound.value = false
        }
    }

    // dang ki broadcast
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var bundle = intent.extras
            if (bundle == null) return
            var action = bundle.getInt(ACTION)
//            currentPos = bundle.getInt(CURRENT_POSITION)
            handleLayoutPlay(action)
        }
    }


    init {
        Intent(context, MusicService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_SEND_TO_ACTIVITY))

    }

    override fun onCleared() {
        context.unbindService(connection)
        mBound.value = false
        handler?.removeCallbacks(runnable!!)
        super.onCleared()
    }

    fun getListRecommend(s: Song) {
        dataLoading.value = true
        MusicRepository.getInstance().getRecommendSong(s.type, s.id) { isSuccess, response ->
            dataLoading.value = false
            if (isSuccess) {
                repoListRecommend.value = response?.data?.items
                if (!listSongOnline.contains(repoListRecommend.value!![0]) && !isPrevious && !AppPreferences.isPlayRequireList) {
                    listSongOnline.add(repoListRecommend.value!![0])
                    isPrevious = false
                }
            } else {
                Toast.makeText(
                    context,
                    "Có lỗi xảy ra khi tải bài hát liên quan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getGenres(s: Song) {
        MusicRepository.getInstance().getASongInfor(s.type, s.id) { isSuccess, response ->
            if (isSuccess) {
                var dataGenres = response!!.data.genres
                if (dataGenres.size > 1) {
                    genres.value = dataGenres[1].name
                } else {
                    genres.value = dataGenres[0].name
                }
            }
        }
    }

    fun handleLayoutPlay(action: Int) {
        when (action) {
            ACTION_START -> {
                getMusic()
                setStatusButton()
//                if (mBound.value==true) {
                getProgress()
            }
            ACTION_PAUSE -> {
                setStatusButton()
                handler?.removeCallbacks(runnable!!)
            }
            ACTION_RESUME -> {
                handler?.postDelayed(runnable!!, 0)
                setStatusButton()
            }
            ACTION_NEXT -> {
                currentPosition.value = 0
//                setupRecommendSong()
            }
            ACTION_PREVIOUS -> {
                isPrevious = true
                currentPosition.value = 0
//                setupRecommendSong()
            }
            ACTION_CLEAR -> {
                clearProgress()
                reloadData()
            }

        }
    }


    fun getMusic() {
        if (AppPreferences.isPlayFavouriteList) {
            mSongFavourite.value = listSongFavourite[AppPreferences.indexPlaying]
            if (mSongFavourite.value!!.url == "") {
                isFavourite.value = sqlHelper.checkExists(mSongFavourite.value!!.id)
            } else {
                isFavourite.value = sqlHelper.checkExistsOff(mSongFavourite.value!!.url)
            }
        } else {
            if (AppPreferences.isOnline) {
                mSongOnline.value = listSongOnline[AppPreferences.indexPlaying]
                getListRecommend(mSongOnline.value!!)
                getGenres(mSongOnline.value!!)
                isFavourite.value = sqlHelper.checkExists(mSongOnline.value!!.id)
            } else {
                mSongOffline.value = listSongOffline[AppPreferences.indexPlaying]
                isFavourite.value = sqlHelper.checkExistsOff(mSongOffline.value!!.resource)
            }
        }
    }

    fun addOrRemoveFavourite() {
        if (AppPreferences.isPlayFavouriteList) {
            if (isFavourite.value == true) {
                sqlHelper.removeSong(mSongFavourite.value!!.id)
                isFavourite.value = false
            } else {
                sqlHelper.addSongFavourite(mSongFavourite.value!!)
                isFavourite.value = true
            }
        }else{
            if (AppPreferences.isOnline){
                if (isFavourite.value==true){
                    sqlHelper.removeSong(mSongOnline.value!!.id)
                    isFavourite.value=false
                }else{
                    sqlHelper.addSong(mSongOnline.value!!)
                    isFavourite.value = true
                }
            }else{
                if (isFavourite.value==true){
                    sqlHelper.removeSongOff(mSongOffline.value!!.resource)
                    isFavourite.value=false
                }else{
                    sqlHelper.addSongOff(mSongOffline.value!!)
                    isFavourite.value=true
                }
            }
        }
    }

    fun setStatusButton() {
        isPlaying.value = AppPreferences.isPlaying
    }

    fun onClickNext() {
        mService.playNextMusic()
    }

    fun onClickPrevious() {
        mService.playPreviousMusic()
    }

    fun startMusic() {
        mService.handleMusic(ACTION_START)
    }

    fun resumeMusic() {
        mService.handleMusic(ACTION_RESUME)
    }

    fun pauseMusic() {
        mService.handleMusic(ACTION_PAUSE)
    }

    fun onClickPlayOrPause() {
        if (isPlaying.value == true) {
            pauseMusic()
        } else {
            resumeMusic()
        }
    }

    fun onSeekbarChange(position: Int) {
        mService.mediaPlayer!!.seekTo(position)
    }

    var handler: Handler? = Handler(Looper.getMainLooper())

    var runnable: Runnable? = object : Runnable {
        override fun run() {
            try {
                currentPosition.value = mService.mediaPlayer?.currentPosition!!
                Log.d("hihi", "run: ${currentPosition.value}")
                handler!!.postDelayed(this, 1000)
            } catch (ex: Exception) {
//                Log.d("hihi", "run: ${ex.message}")
            }
        }
    }

    fun getProgress() {
        handler!!.removeCallbacks(runnable!!)
        handler!!.postDelayed(runnable!!, 1000)
    }

    fun downloadMusic() {
        if (mSongOnline.value != null) {
            var url =
                "http://api.mp3.zing.vn/api/streaming/${mSongOnline.value!!.type}/${mSongOnline.value!!.id}/320"
            var uri = Uri.parse(url)
            var request = DownloadManager.Request(uri)
            var title = "${mSongOnline.value!!.name}.mp3"
            request.setTitle(title)
            request.setDescription("Đang tải vui lòng đợi")
            var cookie = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookie)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
            var downloadManager =
                context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

        }

    }

    private fun clearProgress() {
        if (handler != null) {
            handler!!.removeCallbacks(runnable!!)
        }
    }


}