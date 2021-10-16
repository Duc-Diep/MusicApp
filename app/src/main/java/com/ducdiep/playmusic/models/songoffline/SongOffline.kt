package com.ducdiep.playmusic.models.songoffline

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

data class SongOffline(var name:String, var artist:String, var duration: Long, var imageBitmap:Bitmap, var resource: String): Serializable
