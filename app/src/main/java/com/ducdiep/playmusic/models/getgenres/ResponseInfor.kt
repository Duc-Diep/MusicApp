package com.ducdiep.playmusic.models.getgenres

data class ResponseInfor(
    val `data`: DataSongInfor,
    val err: Int,
    val msg: String,
    val timestamp: Long
)