package xyz.teccheck.alphacompanion

import android.bluetooth.BluetoothGatt
import android.companion.DevicePresenceEvent
import android.os.Build
import androidx.annotation.RequiresApi

object LogUtils {

    fun Int.gattStatusToString() = when(this) {
        BluetoothGatt.GATT_SUCCESS-> "SUCCESS"
        BluetoothGatt.GATT_READ_NOT_PERMITTED-> "READ_NOT_PERMITTED"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED-> "WRITE_NOT_PERMITTED"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION-> "INSUFFICIENT_AUTHENTICATION"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED-> "REQUEST_NOT_SUPPORTED"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION-> "INSUFFICIENT_ENCRYPTION"
        BluetoothGatt.GATT_INVALID_OFFSET-> "INVALID_OFFSET"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION-> "INSUFFICIENT_AUTHORIZATION"
        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH-> "INVALID_ATTRIBUTE_LENGTH"
        BluetoothGatt.GATT_CONNECTION_CONGESTED-> "CONNECTION_CONGESTED"
        BluetoothGatt.GATT_CONNECTION_TIMEOUT-> "CONNECTION_TIMEOUT"
        BluetoothGatt.GATT_FAILURE-> "FAILURE"
        else -> "UNKNOWN[$this]"
    }

    fun Int.gattStateToString() = when(this) {
        BluetoothGatt.STATE_CONNECTING -> "CONNECTING"
        BluetoothGatt.STATE_CONNECTED -> "CONNECTED"
        BluetoothGatt.STATE_DISCONNECTING -> "DISCONNECTING"
        BluetoothGatt.STATE_DISCONNECTED -> "DISCONNECTED"
        else -> "UNKNOWN[$this]"
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun DevicePresenceEvent.typeToString() = when (this.event) {
        DevicePresenceEvent.EVENT_BLE_APPEARED -> "EVENT_BLE_APPEARED"
        DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> "EVENT_BLE_DISAPPEARED"
        DevicePresenceEvent.EVENT_BT_CONNECTED -> "EVENT_BT_CONNECTED"
        DevicePresenceEvent.EVENT_BT_DISCONNECTED -> "EVENT_BT_DISCONNECTED"
        DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED -> "EVENT_SELF_MANAGED_APPEARED"
        DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED -> "EVENT_SELF_MANAGED_DISAPPEARED"
        DevicePresenceEvent.EVENT_ASSOCIATION_REMOVED -> "EVENT_ASSOCIATION_REMOVED"
        else -> "UNKNOWN (${this.event})"
    }
}