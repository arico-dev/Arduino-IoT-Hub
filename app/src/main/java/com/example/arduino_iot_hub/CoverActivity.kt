package com.example.arduino_iot_hub

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityCoverBinding

class CoverActivity : AppCompatActivity() {

    // Declaramos el objeto de View Binding
    private lateinit var binding: ActivityCoverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conectamos el archivo XML (activity_cover.xml) con esta clase
        binding = ActivityCoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

    Log.i("ArduinoIoT", "CoverActivity onCreate")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i("ArduinoIoT", "CoverActivity onBackPressed (dispatcher)")
                finish()
            }
        })

        // Evento: cuando el usuario toca la pantalla
        binding.coverLayout.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        Log.i("ArduinoIoT", "CoverActivity onResume")
    }
}
