package com.ducdiep.playmusic.app

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

object AppPreferences {
    private const val NAME = "AppPreferences"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    private val INDEX_PLAYING = Pair("index",0)
    private val IS_SHUFFLE = Pair("shuffle",false)
    private val IS_PLAYING = Pair("playing",false)
    private val IS_REPEAT_ONE = Pair("repeatone",false)

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
    var isShuffle: Boolean
        get() = preferences.getBoolean(IS_SHUFFLE.first, IS_SHUFFLE.second)
        set(value) = preferences.edit {
            it.putBoolean(IS_SHUFFLE.first, value)
        }
    var isRepeatOne: Boolean
        get() = preferences.getBoolean(IS_REPEAT_ONE.first, IS_REPEAT_ONE.second)
        set(value) = preferences.edit {
            it.putBoolean(IS_REPEAT_ONE.first, value)
        }
    var isPlaying: Boolean
        get() = preferences.getBoolean(IS_PLAYING.first, IS_PLAYING.second)
        set(value) = preferences.edit {
            it.putBoolean(IS_PLAYING.first, value)
        }
}