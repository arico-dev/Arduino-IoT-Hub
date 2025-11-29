package com.example.arduino_iot_hub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                registerUser()
            }
        }

        binding.btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun validateInput(): Boolean {
        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.all_fields_required)
            return false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.all_fields_required)
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.all_fields_required)
            return false
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_do_not_match)
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            return false
        }

        return true
    }

    private fun registerUser() {
        setInProgress(true)
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                setInProgress(false)
                if (it.isSuccessful) {
                    firebaseAuth.signOut() // Force user to log in after registration
                    Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.registration_error), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setInProgress(inProgress: Boolean) {
        binding.progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !inProgress
        binding.btnGoToLogin.isEnabled = !inProgress
        binding.tilEmail.isEnabled = !inProgress
        binding.tilPassword.isEnabled = !inProgress
        binding.tilConfirmPassword.isEnabled = !inProgress
    }
}
