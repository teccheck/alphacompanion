package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import androidx.annotation.RequiresPermission

class QueueingBluetoothGatt(val internal: BluetoothGatt) : BluetoothGattCallback() {

    // TODO: timeout?
    private val queue = ArrayDeque<GattOperation>(10)
    private var operationRunning = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queueCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        queue.addLast(CharacteristicRead(characteristic))
        nextOperation()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queueCharacteristicWrite(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        queue.addLast(CharacteristicWrite(characteristic, data))
        nextOperation()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queueDescriptorRead(descriptor: BluetoothGattDescriptor) {
        queue.addLast(DescriptorRead(descriptor))
        nextOperation()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queueDescriptorWrite(descriptor: BluetoothGattDescriptor, data: ByteArray) {
        queue.addLast(DescriptorWrite(descriptor, data))
        nextOperation()
    }

    private fun nextOperation() {
        if (operationRunning) return
        val operation = queue.removeFirstOrNull() ?: return

        operationRunning = true
        operationRunning = operation.execute(this.internal)
    }

    private fun operationDone() {
        operationRunning = false
        nextOperation()
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) = operationDone()

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
    ) = operationDone()

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
    ) = operationDone()

    @Deprecated("Deprecated in Java")
    override fun onDescriptorRead(
        gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int
    ) = operationDone()

    override fun onDescriptorRead(
        gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int, value: ByteArray
    ) = operationDone()

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int
    ) = operationDone()

    override fun onReliableWriteCompleted(
        gatt: BluetoothGatt?, status: Int
    ) = operationDone()

    abstract class GattOperation {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        abstract fun execute(gatt: BluetoothGatt): Boolean
    }

    class CharacteristicWrite(
        val characteristic: BluetoothGattCharacteristic, val data: ByteArray
    ) : GattOperation() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun execute(gatt: BluetoothGatt) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.value = data
                gatt.writeCharacteristic(characteristic)
            }
    }

    class CharacteristicRead(val characteristic: BluetoothGattCharacteristic) : GattOperation() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun execute(gatt: BluetoothGatt) = gatt.readCharacteristic(characteristic)
    }

    class DescriptorWrite(val descriptor: BluetoothGattDescriptor, val data: ByteArray) :
        GattOperation() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun execute(gatt: BluetoothGatt) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor, data
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = data
                gatt.writeDescriptor(descriptor)
            }
    }

    class DescriptorRead(val descriptor: BluetoothGattDescriptor) : GattOperation() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun execute(gatt: BluetoothGatt) = gatt.readDescriptor(descriptor)
    }
}