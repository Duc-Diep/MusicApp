package com.ducdiep.playmusic.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ducdiep.playmusic.models.songoffline.SongFavourite
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.repository.MusicRepository

class FavouriteViewModel(application: Application) : AndroidViewModel(application) {
    var context: Context = getApplication<Application>().applicationContext
    val listSongFavourite = MutableLiveData<List<SongFavourite>>()
    fun fetchRepoListFavourite():MutableLiveData<List<SongFavourite>>{
        listSongFavourite.value = MusicRepository.getInstance().getFavouriteSong(context)
        return listSongFavourite
    }
}