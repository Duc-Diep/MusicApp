package com.ducdiep.playmusic.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val baseUrl = " http://mp3.zing.vn/"

class RetrofitInstance {
    companion object{
        fun getInstance(): Retrofit {
            var gson = GsonBuilder()
                .setDateFormat("YYYY-MM-dd HH:mm:ss").create()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
    }
}