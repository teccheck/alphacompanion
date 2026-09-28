package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.UUID

open class GattService(uuid: String, protected val gatt: QueueingBluetoothGatt) :
    BluetoothGattCallback() {
    protected val uuid: UUID = UUID.fromString(uuid)
    protected val service: BluetoothGattService = gatt.internal.getService(this.uuid)
    private val characteristics = mutableMapOf<UUID, Callbacks>()

    protected fun registerCharacteristic(
        uuidPrefix: String,
        onRead: ReadCallback? = null,
        onWrite: WriteCallback? = null,
        onChange: ChangeCallback? = null,
    ): BluetoothGattCharacteristic? {
        val characteristic =
            service.characteristics.first { it.uuid.toString().startsWith(uuidPrefix, true) }
                ?: return null
        characteristics[characteristic.uuid] = Callbacks(onRead, onWrite, onChange)
        return characteristic
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun readCharacteristic(characteristic: BluetoothGattCharacteristic) =
        gatt.queueCharacteristicRead(characteristic)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic, data: ByteArray
    ) = gatt.queueCharacteristicWrite(characteristic, data)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun writeNotificationDescriptor(characteristic: BluetoothGattCharacteristic) {
        gatt.internal.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString(NOTIFICATION_UUID))
        if (descriptor == null) {
            Log.d(TAG, "Notify descriptor is null")
            return
        }

        gatt.queueDescriptorWrite(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }

    private fun onRead(uuid: UUID, data: ByteArray, status: Int) {
        characteristics[uuid]?.read?.onRead(data, status)
    }

    private fun onWrite(uuid: UUID, status: Int) {
        characteristics[uuid]?.write?.onWrite(status)
    }

    private fun onChange(uuid: UUID, data: ByteArray) {
        Log.d(TAG, "onChange($uuid, ${data.toHexString()})")
        characteristics[uuid]?.change?.onChange(data)
    }

    override fun onCharacteristicRead(
        g: BluetoothGatt, c: BluetoothGattCharacteristic, v: ByteArray, s: Int
    ) = this.onRead(c.uuid, v, s)

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        g: BluetoothGatt?, c: BluetoothGattCharacteristic, s: Int
    ) = this.onRead(c.uuid, c.value, s)

    override fun onCharacteristicWrite(
        g: BluetoothGatt?, c: BluetoothGattCharacteristic, s: Int
    ) = onWrite(c.uuid, s)

    override fun onCharacteristicChanged(
        g: BluetoothGatt, c: BluetoothGattCharacteristic, v: ByteArray
    ) = onChange(c.uuid, v)

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        g: BluetoothGatt?, c: BluetoothGattCharacteristic
    ) = onChange(c.uuid, c.value)

    private data class Callbacks(
        val read: ReadCallback?, val write: WriteCallback?, val change: ChangeCallback?
    )

    fun interface ReadCallback {
        fun onRead(data: ByteArray, status: Int)
    }

    fun interface WriteCallback {
        fun onWrite(status: Int)
    }

    fun interface ChangeCallback {
        fun onChange(data: ByteArray)
    }

    companion object {
        const val TAG = "GattService"
        const val NOTIFICATION_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}