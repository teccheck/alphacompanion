package xyz.teccheck.alphacompanion.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log

class GattServiceMystery(gatt: QueueingBluetoothGatt) :
    GattService("8000FF00-FF00-FFFF-FFFF-FFFFFFFFFFFF", gatt) {

    // Writable
    private val unknownCharacteristic01: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000FF01")!!

    // Notification
    private val unknownCharacteristic02: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000FF02", onChange = this::onChange)!!

    fun enableNotification() {
        this.writeNotificationDescriptor(unknownCharacteristic02)
    }

    fun onChange(data: ByteArray) {
        Log.d(TAG, "Change: ${data.toHexString()}")
    }

    companion object {
        const val TAG = "GattServiceMysteryFF"
    }
}