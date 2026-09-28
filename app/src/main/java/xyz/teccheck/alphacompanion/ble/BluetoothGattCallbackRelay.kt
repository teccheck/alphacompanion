package xyz.teccheck.alphacompanion.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import androidx.annotation.RequiresApi

class BluetoothGattCallbackRelay(private val callbacks: MutableList<BluetoothGattCallback>) :
    BluetoothGattCallback() {

    fun addCallback(callback: BluetoothGattCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: BluetoothGattCallback) {
        callbacks.remove(callback)
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
    ) = callbacks.forEach { it.onCharacteristicChanged(gatt, characteristic) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray
    ) = callbacks.forEach { it.onCharacteristicChanged(gatt, characteristic, value) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) = callbacks.forEach { it.onCharacteristicRead(gatt, characteristic, value, status) }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
    ) = callbacks.forEach { it.onCharacteristicRead(gatt, characteristic, status) }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
    ) = callbacks.forEach { it.onCharacteristicWrite(gatt, characteristic, status) }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt?, status: Int, newState: Int
    ) = callbacks.forEach { it.onConnectionStateChange(gatt, status, newState) }

    @Deprecated("Deprecated in Java")
    override fun onDescriptorRead(
        gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int
    ) = callbacks.forEach { it.onDescriptorRead(gatt, descriptor, status) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDescriptorRead(
        gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int, value: ByteArray
    ) = callbacks.forEach { it.onDescriptorRead(gatt, descriptor, status, value) }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int
    ) = callbacks.forEach { it.onDescriptorWrite(gatt, descriptor, status) }

    override fun onMtuChanged(
        gatt: BluetoothGatt?, mtu: Int, status: Int
    ) = callbacks.forEach { it.onMtuChanged(gatt, mtu, status) }

    override fun onPhyRead(
        gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int
    ) = callbacks.forEach { it.onPhyRead(gatt, txPhy, rxPhy, status) }

    override fun onPhyUpdate(
        gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int
    ) = callbacks.forEach { it.onPhyUpdate(gatt, txPhy, rxPhy, status) }

    override fun onReadRemoteRssi(
        gatt: BluetoothGatt?, rssi: Int, status: Int
    ) = callbacks.forEach { it.onReadRemoteRssi(gatt, rssi, status) }

    override fun onReliableWriteCompleted(
        gatt: BluetoothGatt?, status: Int
    ) = callbacks.forEach { it.onReliableWriteCompleted(gatt, status) }

    override fun onServiceChanged(gatt: BluetoothGatt) =
        callbacks.forEach { it.onServiceChanged(gatt) }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) =
        callbacks.forEach { it.onServicesDiscovered(gatt, status) }
}