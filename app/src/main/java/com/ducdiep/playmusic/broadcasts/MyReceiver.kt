package com.ducdiep.playmusic.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ducdiep.playmusic.config.ACTION_SERVICE_TO_BROADCAST
import com.ducdiep.playmusic.config.ACTION_TO_SERVICE
import com.ducdiep.playmusic.services.MusicService

class MyReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var action = intent.getIntExtra(ACTION_SERVICE_TO_BROADCAST,0)
        var intentService = Intent(context,MusicService::class.java)
        intentService.putExtra(ACTION_TO_SERVICE,action)
        context.startService(intentService)
    }
}