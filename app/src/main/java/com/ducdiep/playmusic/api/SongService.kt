package com.ducdiep.playmusic.api

import com.ducdiep.playmusic.models.topsong.ResponseTopSong
import retrofit2.Call
import retrofit2.http.GET

interface SongService {
    @GET("xhr/chart-realtime?songId=0&videoId=0&albumId=0&chart=song&time=-1")
    fun getTopSong(): Call<ResponseTopSong>
}