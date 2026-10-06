package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattConnectionSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
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
    private val gatt: BluetoothGatt = connectGatt(context, device, gattCallbackRelay)
    private lateinit var queueingGatt: QueueingBluetoothGatt

    private var serviceLocation: GattServiceLocation? = null
    private var serviceParing: GattServicePairing? = null

    @SuppressLint("MissingPermission")
    private fun onConnected() {
        Log.i(TAG, "gatt connected")
        if (missingPermissions()) return
        this.gatt.requestMtu(158)
    }

    @SuppressLint("MissingPermission")
    private fun onMtuChanged(mtu: Int) {
        Log.i(TAG, "mtu received, is now $mtu")
        if (missingPermissions()) return
        this.gatt.discoverServices()
    }

    @SuppressLint("MissingPermission")
    private fun onServicesDiscovered() {
        Log.i(TAG, "services discovered")
        if (missingPermissions()) return

        queueingGatt = QueueingBluetoothGatt(gatt)
        gattCallbackRelay.addCallback(queueingGatt)

        serviceParing = GattServicePairing(this.queueingGatt)
        gattCallbackRelay.addCallback(serviceParing!!)
        serviceParing!!.pair()

        serviceLocation = GattServiceLocation(this.queueingGatt)
        gattCallbackRelay.addCallback(serviceLocation!!)
        serviceLocation!!.init()
    }

    @SuppressLint("MissingPermission")
    private fun onDisconnected() {
        Log.i(TAG, "gatt disconnected")
        if (!missingPermissions()) gatt.close()
        this.onClose()
    }

    @SuppressLint("MissingPermission")
    fun setLocation(location: Location?) {
        if (location == null) return
        if (missingPermissions()) return

        this.serviceLocation?.writeLocation(location, Calendar.getInstance())
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(): String {
        if (missingPermissions()) return device.address
        return device.name
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectGatt(context: Context, device: BluetoothDevice, callback: BluetoothGattCallback): BluetoothGatt {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            val settings = BluetoothGattConnectionSettings.Builder().build()
            device.connectGatt(settings, context.mainExecutor, callback)!!
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context, false, callback)
        }
    }

    private fun missingPermissions(): Boolean = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) != PackageManager.PERMISSION_GRANTED

    companion object {
        const val TAG = "SonyBleCamClient"
    }
}