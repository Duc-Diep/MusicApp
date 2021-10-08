package com.ducdiep.playmusic.app

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

object AppPreferences {
    private const val NAME = "AppPreferences"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    private val INDEX_PLAYING = Pair("index",0)

    fun init(context: Context?) {
        preferences = context?.getSharedPreferences(NAME, MODE)!!
    }
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    var indexPlaying: Int
        get() = preferences.getInt(INDEX_PLAYING.first, INDEX_PLAYING.second)
        set(value) = preferences.edit {
            it.putInt(INDEX_PLAYING.first, value)
        }

}