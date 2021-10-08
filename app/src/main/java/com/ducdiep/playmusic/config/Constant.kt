package com.ducdiep.playmusic.config

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream
import kotlin.math.floor

const val ACTION_PAUSE = 1
const val ACTION_RESUME = 2
const val ACTION_CLEAR = 3
const val ACTION_START = 4
const val PERMISSION_REQUEST = 4
const val SONG_OBJECT = "song"
const val STATUS_PLAY = "status_play"
const val ACTION = "action"
const val IMAGE = "image"
const val ACTION_SEND_TO_ACTIVITY = "action_send_data_to_activity"
const val ACTION_SERVICE_TO_BROADCAST = "action_service_to_broadcast"
const val ACTION_TO_SERVICE = "action_broadcast_to_service"
const val THUMBNAIL_SIZE = 50

fun getThumbnail(uri: Uri, context: Context): Bitmap? {
    var input: InputStream = context.contentResolver.openInputStream(uri)!!
    val onlyBoundsOptions = BitmapFactory.Options()
    onlyBoundsOptions.inJustDecodeBounds = true
    onlyBoundsOptions.inDither = true //optional
    onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
    BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
    input.close()
    if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1) {
        return null
    }
    val originalSize =
        if (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) onlyBoundsOptions.outHeight else onlyBoundsOptions.outWidth
    val ratio = if (originalSize > THUMBNAIL_SIZE) originalSize / THUMBNAIL_SIZE else 1.0
    val bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio as Double)
    bitmapOptions.inDither = true //optional
    bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //
    input = context.getContentResolver().openInputStream(uri)!!
    val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
    input.close()
    return bitmap
}
private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
    val k = Integer.highestOneBit(floor(ratio).toInt())
    return if (k == 0) 1 else k
}