package com.surveyor.data.device.communication

/**
 * Интерфейс для коммуникации с устройствами (тахеометры, GPS приёмники)
 */
interface DeviceCommunication {
    
    /**
     * Подключиться к устройству
     */
    suspend fun connect(deviceAddress: String): Result<Unit>
    
    /**
     * Отключиться от устройства
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Отправить команду на устройство
     */
    suspend fun sendCommand(command: ByteArray): Result<ByteArray>
    
    /**
     * Получить статус подключения
     */
    fun isConnected(): Boolean
    
    /**
     * Получить имя устройства
     */
    fun getDeviceName(): String?
}
