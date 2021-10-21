package com.ducdiep.playmusic.models.getgenres

data class DataSongInfor(
    val artists: List<Artist>,
    val genres: List<Genre>,
    val info: Info
)