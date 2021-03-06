package com.ducdiep.playmusic.api

import com.ducdiep.playmusic.models.getgenres.ResponseInfor
import com.ducdiep.playmusic.models.search.ResponseSearch
import com.ducdiep.playmusic.models.songresponse.ResponseRecommend
import com.ducdiep.playmusic.models.songresponse.ResponseTopSong
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SongService {
    @GET("xhr/chart-realtime")
    fun getTopSong(): Call<ResponseTopSong>
    @GET("complete")
    fun getSearch(@Query("type") type:String,@Query("num") num:Int,@Query("query") query:String ):Call<ResponseSearch>
    @GET("xhr/recommend")
    fun getRecommend(@Query("type") type:String,@Query("id") id:String ):Call<ResponseRecommend>
    @GET("xhr/media/get-info")
    fun getSongInfor(@Query("type") type:String,@Query("id") id:String ):Call<ResponseInfor>
}