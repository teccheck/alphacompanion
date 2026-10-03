package xyz.teccheck.alphacompanion.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.location.Location
import android.util.Log
import java.util.Calendar

class SonyBleCamClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val onClose: () -> Unit
) {
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) =
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> onConnected()
                BluetoothGatt.STATE_DISCONNECTED -> onDisconnected()
                else -> {}
            }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) = onMtuChanged(mtu)

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) =
            onServicesDiscovered()
    }

    private val gattCallbackRelay = BluetoothGattCallbackRelay(mutableListOf(gattCallback))
    private val gatt: BluetoothGatt = device.connectGatt(context, false, gattCallbackRelay)
    private lateinit var queueingGatt: QueueingBluetoothGatt

    private var serviceLocation: GattServiceLocation? = null

    private fun onConnected() {
        Log.i(TAG, "gatt connected")
        this.gatt.requestMtu(158)
    }

    private fun onMtuChanged(mtu: Int) {
        Log.i(TAG, "mtu received, is now $mtu")
        this.gatt.discoverServices()
    }

    private fun onServicesDiscovered() {
        Log.i(TAG, "services discovered")
        queueingGatt = QueueingBluetoothGatt(gatt)
        gattCallbackRelay.addCallback(queueingGatt)

        serviceLocation = GattServiceLocation(this.queueingGatt, {})
        gattCallbackRelay.addCallback(serviceLocation!!)

        serviceLocation?.init()
    }

    fun setLocation(location: Location?) {
        if (location == null) return
        this.serviceLocation?.writeLocation(location, Calendar.getInstance())
    }

    fun getDevice(): BluetoothDevice = device

    private fun onDisconnected() {
        Log.i(TAG, "gatt disconnected")
        gatt.close()
        this.onClose()
    }

    companion object {
        const val TAG = "SonyBleCamClient"
        const val LOCATION_REQUEST_CODE = 0xCAFE
    }
}