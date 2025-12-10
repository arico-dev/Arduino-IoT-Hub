package com.example.arduino_iot_hub

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.example.arduino_iot_hub.databinding.ActivityControlPanelBinding
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class ControlPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlPanelBinding
    private lateinit var firestore: FirebaseFirestore
    private var ledOn = false
    private var deviceAddress: String? = null
    private var bluetoothConnection: BluetoothConnection? = null
    private var lastLdrState: String? = null // To track LDR state changes

    private val handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BluetoothConnection.STATE_CONNECTING -> {
                    binding.tvStatus.text = getString(R.string.status_connecting)
                }
                BluetoothConnection.STATE_CONNECTED -> {
                    binding.tvStatus.text = getString(R.string.status_connected)
                    saveEventToFirestore("CONEXION_EXITOSA", mapOf("deviceAddress" to (deviceAddress ?: "N/A")))
                }
                BluetoothConnection.STATE_CONNECTION_FAILED -> {
                    binding.tvStatus.text = getString(R.string.status_connection_failed)
                }
                BluetoothConnection.STATE_MESSAGE_RECEIVED -> {
                    val message = msg.obj as? String
                    if (message != null) {
                        binding.tvConsole.append(message + "\n")
                        // Process every message received from Arduino
                        handleArduinoData(message)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        if (deviceAddress != null) {
            bluetoothConnection = BluetoothConnection(this, handler, deviceAddress!!)
            bluetoothConnection?.connect()
        } else {
            Toast.makeText(this, "Device address not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Enable scrolling in the console TextView
        binding.tvConsole.movementMethod = ScrollingMovementMethod()

        setupUIListeners()
    }

    private fun setupUIListeners() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                bluetoothConnection?.cancel()
                finish()
            }
        })

        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            // This listener is only for user actions. The UI update is handled by handleArduinoData.
            val command = if (isChecked) "LED_ON" else "LED_OFF"
            bluetoothConnection?.write(command)
            saveEventToFirestore("ACCION_LED", mapOf("estado" to if(isChecked) "ENCENDIDO" else "APAGADO"))
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothConnection?.cancel()
            finish()
        }

        binding.btnSendConsole.setOnClickListener {
            val txt = binding.etConsole.text.toString().trim()
            if (txt.isNotEmpty()) {
                bluetoothConnection?.write(txt)
                saveEventToFirestore("COMANDO_CONSOLA", mapOf("comando" to txt))
                val t = "[App] $txt"
                binding.tvConsole.append(t + "\n")
                binding.etConsole.setText("")
            }
        }

        binding.btnLoadHistory.setOnClickListener {
            loadHistoryFromFirestore()
        }
    }

    private fun handleArduinoData(message: String) {
        // Parse LDR value if present
        if (message.contains("Luz:")) {
            try {
                val ldrString = message.substringAfter("Luz: ").substringBefore(" ->").trim()
                val ldrValue = ldrString.toInt()
                binding.tvLdrValue.text = ldrValue.toString()
                binding.ldrProgressIndicator.progress = ldrValue

                // Check for LDR state change for Firestore
                val newLdrState = when {
                    message.contains("Auto: OSCURO") -> "Oscuro"
                    message.contains("Auto: HAY LUZ") -> "Claro"
                    else -> null // Not an automatic state message
                }

                if (newLdrState != null && newLdrState != lastLdrState) {
                    lastLdrState = newLdrState
                    saveEventToFirestore("CAMBIO_DE_LUZ", mapOf("nuevo_estado" to newLdrState, "valor_ldr" to ldrValue))
                }
            } catch (e: Exception) {
                Log.e("ControlPanelActivity", "Error parsing LDR value from: $message", e)
            }
        }

        // Parse LED State from any relevant message
        val newLedState = when {
            message.contains("(LED ON)") || message.contains("MANUAL: LED ENCENDIDO") -> true
            message.contains("(LED OFF)") || message.contains("MANUAL: LED APAGADO") -> false
            else -> null // This message doesn't inform the LED state
        }

        if (newLedState != null) {
            ledOn = newLedState
            updateLedUi()
        }
    }

    private fun saveEventToFirestore(eventType: String, eventData: Map<String, Any>) {
        val event = hashMapOf(
            "type" to eventType,
            "timestamp" to FieldValue.serverTimestamp(),
            "data" to eventData
        )

        firestore.collection("eventos_arduino")
            .add(event)
            .addOnSuccessListener { 
                val logMsg = "[Firestore] Evento '$eventType' guardado.\n"
                binding.tvConsole.append(logMsg)
            }
            .addOnFailureListener { e ->
                val logMsg = "[Firestore] Error al guardar evento '$eventType': ${e.message}\n"
                binding.tvConsole.append(logMsg)
                Log.w("ControlPanelActivity", "Error adding document", e)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun loadHistoryFromFirestore() {
        binding.tvConsole.text = getString(R.string.loading_history) + "\n"
        firestore.collection("eventos_arduino")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvConsole.text = ""
                if (documents.isEmpty) {
                    binding.tvConsole.text = getString(R.string.no_history_found)
                    return@addOnSuccessListener
                }
                // Reverse the list to show oldest first
                val reversedDocuments = documents.documents.asReversed()
                for (document in reversedDocuments) {
                    val timestamp = document.getTimestamp("timestamp")?.toDate()
                    val type = document.getString("type")
                    val data = document.get("data")

                    val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
                    val dateString = timestamp?.let { sdf.format(it) } ?: "No date"

                    val logEntry = "$dateString - TIPO: $type, Datos: $data\n"
                    binding.tvConsole.append(logEntry)
                }
            }
            .addOnFailureListener { exception ->
                binding.tvConsole.text = "Error al cargar el historial: ${exception.message}\n"
                Log.w("ControlPanelActivity", "Error getting documents: ", exception)
            }
    }


    private fun updateLedUi() {
        // Remove listener before updating to prevent feedback loop
        binding.switchLed.setOnCheckedChangeListener(null)
        binding.switchLed.isChecked = ledOn
        binding.viewLedIndicator.isActivated = ledOn
        // Restore listener
        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            val command = if (isChecked) "LED_ON" else "LED_OFF"
            bluetoothConnection?.write(command)
            saveEventToFirestore("ACCION_LED", mapOf("estado" to if(isChecked) "ENCENDIDO" else "APAGADO"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothConnection?.cancel()
    }
}
