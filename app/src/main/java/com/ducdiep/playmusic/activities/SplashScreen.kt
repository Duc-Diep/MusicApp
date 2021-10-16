package com.ducdiep.playmusic.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.app.MyApplication.Companion.listSongOffline
import com.ducdiep.playmusic.config.PERMISSION_REQUEST
import com.ducdiep.playmusic.config.getAudio
import com.ducdiep.playmusic.config.loadDefaultMusic

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)
        requestPermisssion()
    }
    fun requestPermisssion() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST
                )
            }
        }else{
            listSongOffline = getAudio(this)
            listSongOffline.addAll(0, loadDefaultMusic(this))
            Handler().postDelayed({
                var intent = Intent(this,HomeActivity::class.java)
                startActivity(intent)
                finish()
            },1000)

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Access permission read external success",
                        Toast.LENGTH_SHORT
                    ).show()
                    listSongOffline = getAudio(this)
                    listSongOffline.addAll(0, loadDefaultMusic(this))
                } else {
                    Toast.makeText(
                        this,
                        "Access permission read external denied",
                        Toast.LENGTH_SHORT
                    ).show()
                    listSongOffline.addAll(0, loadDefaultMusic(this))
                }
                Handler().postDelayed({
                    var intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 500)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}