package com.example.arduino_iot_hub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityLoginBinding
import android.widget.Toast

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

    Log.i("ArduinoIoT", "LoginActivity onCreate")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i("ArduinoIoT", "LoginActivity onBackPressed (dispatcher)")
                finish()
            }
        })

        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString()
            val pass = binding.etPassword.text.toString()
            if (user == "user" && pass == "1234") {
                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                // Navegar a DescriptionActivity después del login
                startActivity(Intent(this, DescriptionActivity::class.java))
            } else {
                binding.tvStatus.text = getString(R.string.login_error)
            }
        }
    }
}
