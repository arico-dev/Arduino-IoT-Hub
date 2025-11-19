package com.example.arduino_iot_hub

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission") // Permissions are checked in BluetoothActivity
class BluetoothConnection(
    context: Context,
    private val handler: Handler,
    private val deviceAddress: String
) {
    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var connectThread: ConnectThread? = null
    private var communicationThread: CommunicationThread? = null

    companion object {
        private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_CONNECTION_FAILED = 3
        const val STATE_MESSAGE_RECEIVED = 4
    }

    // Thread to handle the connection
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(myUUID)
        }

        override fun run() {
            handler.obtainMessage(STATE_CONNECTING).sendToTarget()
            bluetoothAdapter?.cancelDiscovery() // Always cancel discovery

            try {
                socket?.connect()
                handler.obtainMessage(STATE_CONNECTED).sendToTarget()
                // Start the communication thread
                startCommunication(socket!!)
            } catch (e: IOException) {
                Log.e("BluetoothConnection", "Connection failed", e)
                handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget()
                cancel()
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e("BluetoothConnection", "Could not close the client socket", e)
            }
        }
    }

    // Thread to handle all communication (read and write)
    private inner class CommunicationThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream))

        override fun run() {
            while (true) {
                try {
                    val line = reader.readLine() ?: break
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, line).sendToTarget()
                } catch (e: IOException) {
                    Log.e("BluetoothConnection", "Input stream was disconnected", e)
                    handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget()
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BluetoothConnection", "Error writing to output stream", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e("BluetoothConnection", "Could not close the com socket", e)
            }
        }
    }

    @Synchronized
    fun connect() {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget()
            return
        }

        // Cancel any thread currently running a connection
        connectThread?.cancel()
        connectThread = null
        // Cancel any thread currently running communication
        communicationThread?.cancel()
        communicationThread = null

        // Start the thread to connect with the given device
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    @Synchronized
    private fun startCommunication(socket: BluetoothSocket) {
        // Start the thread to manage the connection and perform transmissions
        communicationThread = CommunicationThread(socket)
        communicationThread?.start()
    }

    fun write(message: String) {
        val thread: CommunicationThread?
        synchronized(this) {
            thread = communicationThread
        }
        if (thread != null) {
            val fullMessage = message + "\n"
            thread.write(fullMessage.toByteArray())
        }
    }
	
    @Synchronized
    fun cancel() {
        connectThread?.cancel()
        communicationThread?.cancel()
        connectThread = null
        communicationThread = null
    }
}
