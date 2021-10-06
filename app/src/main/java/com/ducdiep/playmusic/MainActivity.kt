package com.ducdiep.playmusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ducdiep.playmusic.objects.Song
import com.ducdiep.playmusic.services.MusicService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_start_service.setOnClickListener{
            playMusic()
        }
        btn_stop_service.setOnClickListener{
            stopMusic()
        }
    }

    private fun stopMusic() {
        var intent = Intent(this,MusicService::class.java)
        stopService(intent)
    }

    private fun playMusic() {
        var intent = Intent(this,MusicService::class.java)
        intent.putExtra("song",Song("Key of truth","Đức Điệp",R.drawable.mayu,R.raw.key_of_truth))
        startService(intent)
    }
}