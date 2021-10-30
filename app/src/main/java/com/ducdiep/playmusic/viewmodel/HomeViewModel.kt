package com.ducdiep.playmusic.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ducdiep.playmusic.config.isNetworkAvailable
import com.ducdiep.playmusic.models.search.SongSearch
import com.ducdiep.playmusic.models.songresponse.Song
import com.ducdiep.playmusic.repository.MusicRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var context: Context = getApplication<Application>().applicationContext
    val isNetworkAvailble = MutableLiveData<Boolean>()
    var repoListSearch = MutableLiveData<List<SongSearch>>()
    var repoListTopSong = MutableLiveData<List<Song>>()
    var dataLoading = MutableLiveData<Boolean>()
    var networkBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent?.action) {
                isNetworkAvailble.value = isNetworkAvailable(context!!)
            }
        }
    }
    init {
        var intentfilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(networkBroadcast, intentfilter)
    }

    override fun onCleared() {
        context.unregisterReceiver(networkBroadcast)
        super.onCleared()
    }

    fun fetchListSearch(key:String){
        dataLoading.value = true
        MusicRepository.getInstance().getSongSearch(key) { isSuccess, response ->
            dataLoading.value = false
            if (isSuccess){
                if (response!!.data.size > 0) {
                    repoListSearch.value = response?.data!![0].song
                }
            }else{
                Toast.makeText(
                    context,
                    "Không tìm thấy bài hát này",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun fetchListTopSong(){
        MusicRepository.getInstance().getTopSong{ isSuccess, response ->  
            if (isSuccess){
                repoListTopSong.value = response!!.data.song
            }else{
                Toast.makeText(context, "Lỗi khi tải bài hát", Toast.LENGTH_SHORT).show()
            }
        }
    }

}