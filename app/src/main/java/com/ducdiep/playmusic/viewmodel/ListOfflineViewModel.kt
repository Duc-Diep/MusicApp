package com.ducdiep.playmusic.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ducdiep.playmusic.activities.PlayMusicActivity
import com.ducdiep.playmusic.adapters.SongOfflineAdapter
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.app.MyApplication
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.repository.MusicRepository
import kotlinx.android.synthetic.main.activity_list_offline.*

class ListOfflineViewModel(application: Application) : AndroidViewModel(application) {
    var context: Context = getApplication<Application>().applicationContext
    val listSongOff = MutableLiveData<List<SongOffline>>()
    fun fetchRepoList():MutableLiveData<List<SongOffline>>{
        listSongOff.value = MusicRepository.getInstance().getListOffline(context)
        return listSongOff
    }

    fun getListSearch(s:String):ArrayList<SongOffline>{
        var listTemp = ArrayList<SongOffline>()
            for (element in MyApplication.listSongOffline) {
                if (element.name.toLowerCase()
                        .contains(s.toLowerCase()) || element.artist.toLowerCase()
                        .contains(
                            s.toLowerCase()
                        )
                ) {
                    listTemp.add(element)
                }
            }
        return listTemp
    }


}