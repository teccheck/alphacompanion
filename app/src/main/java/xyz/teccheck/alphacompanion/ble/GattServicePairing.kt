package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.annotation.RequiresPermission
import xyz.teccheck.alphacompanion.LogUtils.gattStatusToString

class GattServicePairing(gatt: QueueingBluetoothGatt) :
    GattService("8000EE00-EE00-FFFF-FFFF-FFFFFFFFFFFF", gatt) {
    private val pairingCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000EE01", onWrite = this::onWritePairing)!!

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun pair() {
        this.writeCharacteristic(pairingCharacteristic, PAIRING_COMMAND_CHARACTERISTIC)
    }

    private fun onWritePairing(status: Int) {
        Log.d(TAG, "onWritePairing(${status.gattStatusToString()})")
    }

    companion object {
        const val TAG = "GattServiceControl"
        val PAIRING_COMMAND_CHARACTERISTIC: ByteArray = byteArrayOf(6, 8, 1, 0, 0, 0, 0)
    }
}