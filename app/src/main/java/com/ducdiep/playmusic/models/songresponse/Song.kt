package com.ducdiep.playmusic.models.songresponse

data class Song(
    val artists_names: String,//
    val code: String,
    val duration: Int,
    val id: String,
    val name: String,
    val position: Int,
    val thumbnail: String,
//    val title: String,
    val type: String
)