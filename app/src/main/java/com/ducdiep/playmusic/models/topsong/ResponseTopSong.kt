package com.ducdiep.playmusic.models.topsong

data class ResponseTopSong(
    val `data`: Data,
    val err: Int,
    val msg: String,
    val timestamp: Long
)