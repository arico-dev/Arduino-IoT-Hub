package com.example.arduino_iot_hub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, DescriptionActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        binding.progressBar.visibility = View.GONE
                        if (it.isSuccessful) {
                            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DescriptionActivity::class.java))
                            finish()
                        } else {
                            binding.tvStatus.text = getString(R.string.login_error)
                        }
                    }
            } else {
                Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
