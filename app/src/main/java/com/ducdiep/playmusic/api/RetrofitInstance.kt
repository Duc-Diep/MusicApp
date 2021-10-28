package com.ducdiep.playmusic.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val baseUrl = " http://mp3.zing.vn/"
const val baseUrlSearch = "http://ac.mp3.zing.vn/"

class RetrofitInstance {
    companion object{
        fun getInstance(): SongService? {
            var gson = GsonBuilder()
                .setDateFormat("YYYY-MM-dd HH:mm:ss").create()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SongService::class.java)
        }
        fun getInstanceSearch(): SongService? {
            var gson = GsonBuilder()
                .setDateFormat("YYYY-MM-dd HH:mm:ss").create()

            return Retrofit.Builder()
                .baseUrl(baseUrlSearch)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SongService::class.java)
        }
    }
}