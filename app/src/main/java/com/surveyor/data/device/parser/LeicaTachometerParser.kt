package com.surveyor.data.device.parser

import com.surveyor.domain.model.Measurement
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI

/**
 * Парсер данных для Leica тахеометров
 * Поддерживает различные форматы Leica протокола
 */
class LeicaTachometerParser : TachometerDataParser {
    
    companion object {
        private const val LEICA_HEADER = 0x06 // ACK
        private const val START_BYTE = 0x02   // STX
        private const val END_BYTE = 0x03     // ETX
    }
    
    override fun parseMeasurement(data: ByteArray): Result<Measurement> {
        return try {
            if (!validateData(data)) {
                return Result.failure(Exception("Невалидный формат данных Leica"))
            }
            
            // Пропустить заголовок и конец
            val payload = data.sliceArray(1 until data.size - 1)
            
            // Парсить различные команды
            val result = when {
                payload.startsWith("13".toByteArray()) -> parseMeasurementData(payload)
                payload.startsWith("11".toByteArray()) -> parseReducedData(payload)
                payload.startsWith("14".toByteArray()) -> parseCompleteData(payload)
                else -> Result.failure(Exception("Неизвестный тип команды"))
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun validateData(data: ByteArray): Boolean {
        return data.isNotEmpty() &&
                data.first() == START_BYTE.toByte() &&
                data.last() == END_BYTE.toByte() &&
                data.size > 4
    }
    
    private fun parseMeasurementData(payload: ByteArray): Result<Measurement> {
        return try {
            // Типичный формат: 13 + PID (2) + Distance (8) + Horizontal Angle (8) + Vertical Angle (8)
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
            
            buffer.short // пропустить команду
            val pointId = buffer.short
            val distance = buffer.double / 1000.0 // в метры
            val horizontalAngle = buffer.double.degreesToRadians()
            val verticalAngle = buffer.double.degreesToRadians()
            
            val measurement = Measurement(
                id = 0,
                projectId = 0,
                pointId = pointId.toInt(),
                distance = distance,
                horizontalAngle = horizontalAngle,
                verticalAngle = verticalAngle,
                horizontalDistance = distance * Math.cos(verticalAngle),
                verticalDistance = distance * Math.sin(verticalAngle),
                timestamp = System.currentTimeMillis(),
                rawData = payload.joinToString("") { "%02x".format(it) }
            )
            
            Result.success(measurement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseReducedData(payload: ByteArray): Result<Measurement> {
        // Сокращённый формат данных
        return parseMeasurementData(payload)
    }
    
    private fun parseCompleteData(payload: ByteArray): Result<Measurement> {
        // Полный формат данных с дополнительной информацией
        return parseMeasurementData(payload)
    }
    
    private fun Double.degreesToRadians(): Double = this * PI / 180.0
}
