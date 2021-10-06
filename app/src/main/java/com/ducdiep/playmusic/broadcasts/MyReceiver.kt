package com.ducdiep.playmusic.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ducdiep.playmusic.services.MusicService

class MyReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var action = intent.getIntExtra("action_music",0)
        var intentService = Intent(context,MusicService::class.java)
        intentService.putExtra("action_music_receiver",action)
        context.startService(intentService)
    }
}