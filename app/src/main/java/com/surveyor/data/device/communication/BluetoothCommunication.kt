package com.surveyor.data.device.communication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Реализация Bluetooth коммуникации с тахеометрами
 */
class BluetoothCommunication(
    private val bluetoothAdapter: BluetoothAdapter?
) : DeviceCommunication {
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null
    
    companion object {
        // UUID для Serial Port Profile (SPP)
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val READ_BUFFER_SIZE = 1024
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter == null) {
                return@withContext Result.failure(Exception("Bluetooth не доступен"))
            }
            
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            connectedDevice = device
            
            // Закрыть предыдущее соединение если существует
            disconnect()
            
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            Result.success(Unit)
        } catch (e: IOException) {
            bluetoothSocket = null
            inputStream = null
            outputStream = null
            connectedDevice = null
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            connectedDevice = null
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null || inputStream == null) {
                return@withContext Result.failure(Exception("Соединение не установлено"))
            }
            
            // Отправить команду
            outputStream?.write(command)
            outputStream?.flush()
            
            // Получить ответ
            val buffer = ByteArray(READ_BUFFER_SIZE)
            val bytesRead = inputStream?.read(buffer) ?: 0
            
            if (bytesRead <= 0) {
                return@withContext Result.failure(Exception("Не получен ответ от устройства"))
            }
            
            val response = buffer.sliceArray(0 until bytesRead)
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    override fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected ?: false
    }
    
    override fun getDeviceName(): String? {
        return connectedDevice?.name
    }
}
