package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import xyz.teccheck.alphacompanion.LogUtils.gattStatusToString
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.TimeZone
import kotlin.experimental.and

// For this service wrapper to send location data, a call to init is required
class GattServiceLocation(gatt: QueueingBluetoothGatt) :
    GattService("8000DD00-DD00-FFFF-FFFF-FFFFFFFFFFFF", gatt) {

    private val statusCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000DD01", onChange = this::onChangeStatus)!!

    private val locationCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000DD11", onWrite = this::onWriteLocation)!!

    private val configCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000DD21", this::onReadConfig)!!

    private var config: LocationServiceConfig? = null
    private var sendLocation = true

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun init() {
        Log.d(TAG, "init")
        this.writeNotificationDescriptor(statusCharacteristic)
        this.readCharacteristic(configCharacteristic)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeLocation(location: Location, calendar: Calendar) {
        val config = this.config
        if (config == null) {
            Log.w(TAG, "tried to write location, but config is null")
            return
        }

        if (!sendLocation) {
            Log.w(TAG, "tried to write location, but is disabled")
            return
        }

        this.writeCharacteristic(
            locationCharacteristic,
            buildLocationPayload(location, calendar, config.shouldSendTimeZone)
        )
    }

    private fun buildLocationPayload(
        location: Location, calendar: Calendar, sendTimezone: Boolean
    ): ByteArray {
        // We need to buffer these values, as we use UTC for time and date later on
        val offset = calendar.get(Calendar.ZONE_OFFSET)
        val dst = calendar.get(Calendar.DST_OFFSET)
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        val bufferSize = if (sendTimezone) 95 else 91
        val data = ByteArray(bufferSize)

        val buffer = ByteBuffer.wrap(data)
        buffer.put(byteArrayOf(0, (bufferSize - 2).toByte(), 8, 2, -4, 3, 0, 0, 16, 16, 16))

        buffer.putInt((location.latitude * 1.0E7).toInt())
        buffer.putInt((location.longitude * 1.0E7).toInt())

        buffer.putShort(calendar.get(Calendar.YEAR).toShort())
        buffer.put((calendar.get(Calendar.MONTH) + 1).toByte())
        buffer.put(calendar.get(Calendar.DAY_OF_MONTH).toByte())
        buffer.put(calendar.get(Calendar.HOUR_OF_DAY).toByte())
        buffer.put(calendar.get(Calendar.MINUTE).toByte())
        buffer.put(calendar.get(Calendar.SECOND).toByte())

        if (!sendTimezone) return data

        buffer.position(91)
        buffer.putShort((offset / 60000).toShort())
        buffer.putShort((dst / 60000).toShort())

        return data
    }

    private fun onChangeStatus(data: ByteArray) {
        val text = if (data.contentEquals(LOCATION_TRANSFER_AVAILABLE)) {
            "LOCATION_TRANSFER_AVAILABLE"
        } else if (data.contentEquals(LOCATION_TRANSFER_ENABLE)) {
            sendLocation = true
            "LOCATION_TRANSFER_ENABLE"
        } else if (data.contentEquals(LOCATION_TRANSFER_DISABLE)) {
            sendLocation = false
            "LOCATION_TRANSFER_DISABLE"
        } else {
            "UNKNOWN [${data.toHexString()}]"
        }

        Log.d(TAG, "Status: $text")
    }

    private fun onWriteLocation(status: Int) {
        Log.d(TAG, "onWriteLocation(${status.gattStatusToString()})")
        // TDOD: Watch for repeated failures
    }

    private fun onReadConfig(data: ByteArray, status: Int) {
        Log.d(TAG, "onReadConfig(${data.toHexString()}, ${status.gattStatusToString()})")

        val sendTz = data.getOrNull(4)?.let { (it and 2) == 2.toByte() } ?: false
        this.config = LocationServiceConfig(sendTz)
    }

    private data class LocationServiceConfig(val shouldSendTimeZone: Boolean)

    companion object {
        const val TAG = "GattServiceLocation"
        val LOCATION_TRANSFER_DISABLE: ByteArray = byteArrayOf(3, 1, 2, 0)
        val LOCATION_TRANSFER_ENABLE: ByteArray = byteArrayOf(3, 1, 2, 1) // Not defined on original code?
        val LOCATION_TRANSFER_AVAILABLE: ByteArray = byteArrayOf(3, 1, 3, 1)
    }
}