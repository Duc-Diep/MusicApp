package com.ducdiep.playmusic.config

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.AppPreferences
import com.ducdiep.playmusic.models.songoffline.SongOffline


const val ACTION_PAUSE = 1
const val ACTION_RESUME = 2
const val ACTION_CLEAR = 3
const val ACTION_START = 4
const val ACTION_NEXT = 6
const val ACTION_PREVIOUS = 5
const val PERMISSION_REQUEST = 7
const val ACTION_RELOAD = "reload"
const val ACTION = "action"
const val IS_ONLINE = "isOnline"
const val CURRENT_POSITION = "position"
const val IMAGE = "image"
const val INDEX = "index"
const val PROGRESS = "progress"
const val ACTION_SEND_TO_ACTIVITY = "action_send_data_to_activity"
const val ACTION_SERVICE_TO_BROADCAST = "action_service_to_broadcast"
const val ACTION_TO_SERVICE = "action_broadcast_to_service"
const val URL_THUMB = "https://photo-zmp3.zadn.vn/"
const val URL_MUSIC = "http://api.mp3.zing.vn/api/streaming/"

lateinit var bitmapDefault: Bitmap

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (cm==null){
        return false
    }
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
        var network = cm.activeNetwork
        if (network==null){
            return false
        }
        var capabilities = cm.getNetworkCapabilities(network)
        return capabilities!=null&&capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }else{
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }

}

fun reloadData() {
    AppPreferences.isRepeatOne = false
    AppPreferences.isShuffle = false
    AppPreferences.indexPlaying = -1
    AppPreferences.isPlaying = false
    AppPreferences.isServiceRunning = false
    AppPreferences.isOnline = false
    AppPreferences.isPlayRequireList = false
    AppPreferences.isPlayFavouriteList = false
}