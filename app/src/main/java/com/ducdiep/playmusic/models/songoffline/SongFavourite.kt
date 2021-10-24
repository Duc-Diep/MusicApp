package com.ducdiep.playmusic.models.songoffline

data class SongFavourite(
    val artists_names: String,
    val code: String,
    val duration: Int,
    val id: String,
    val name: String,
    val position: Int,
    val thumbnail: String,
    val type: String,
    val url:String
)
