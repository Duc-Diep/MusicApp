package com.ducdiep.playmusic.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

data class Song(var name:String, var artist:String, var duration: String, var imageBitmap:Bitmap, var resource: String,): Serializable
