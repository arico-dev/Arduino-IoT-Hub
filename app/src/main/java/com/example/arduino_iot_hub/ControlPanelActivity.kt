package com.example.arduino_iot_hub

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityControlPanelBinding
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback

class ControlPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlPanelBinding
    private var ledOn = false
    private var deviceAddress: String? = null
    private var bluetoothConnection: BluetoothConnection? = null

    private val handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BluetoothConnection.STATE_CONNECTING -> {
                    binding.tvStatus.text = getString(R.string.status_connecting)
                }
                BluetoothConnection.STATE_CONNECTED -> {
                    binding.tvStatus.text = getString(R.string.status_connected)
                }
                BluetoothConnection.STATE_CONNECTION_FAILED -> {
                    binding.tvStatus.text = getString(R.string.status_connection_failed)
                }
                BluetoothConnection.STATE_MESSAGE_RECEIVED -> {
                    val message = msg.obj as? String
                    if (message != null) {
                        binding.tvConsole.append(message + "\n")
                        // Parse the message for LDR and LED status
                        if (message.contains("Luz:")) {
                            try {
                                val ldrValue = message.substringAfter("Luz: ").substringBefore(" |").trim().toInt()
                                binding.tvLdrValue.text = ldrValue.toString()
                                binding.ldrProgressIndicator.progress = ldrValue

                                ledOn = message.contains("LED ENCENDIDO")
                                updateLedUi()
                            } catch (e: Exception) {
                                Log.e("ControlPanelActivity", "Error parsing LDR value from: $message", e)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        if (deviceAddress != null) {
            bluetoothConnection = BluetoothConnection(this, handler, deviceAddress!!)
            bluetoothConnection?.connect()
        } else {
            Toast.makeText(this, "Device address not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                bluetoothConnection?.cancel()
                finish()
            }
        })

        updateLedUi()

        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bluetoothConnection?.write("LED_ON")
            } else {
                bluetoothConnection?.write("LED_OFF")
            }
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothConnection?.cancel()
            finish()
        }

        binding.btnSendConsole.setOnClickListener {
            val txt = binding.etConsole.text.toString().trim()
            if (txt.isNotEmpty()) {
                bluetoothConnection?.write(txt)
                val t = "[App] $txt"
                binding.tvConsole.append(t + "\n")
                binding.etConsole.setText("")
            }
        }
    }

    private fun updateLedUi() {
        binding.switchLed.isChecked = ledOn
        binding.viewLedIndicator.isActivated = ledOn
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothConnection?.cancel()
    }
}
