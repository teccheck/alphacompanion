package xyz.teccheck.alphacompanion.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import java.util.Calendar

class SonyBleCamClient(
    private val context: Context,
    device: BluetoothDevice,
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

    private val locationManager: LocationManager by lazy {
        context.getSystemService()!!
    }
    private val locationCancellationSignal = CancellationSignal()
    private val locationUpdateListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = this@SonyBleCamClient.setLocation(location)
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled($provider)")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled($provider)")
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(
            provider: String?,
            status: Int,
            extras: Bundle?
        ) {
            Log.d(TAG, "onLocStatus($provider, $status, $extras)")
        }
    }

    private lateinit var serviceLocation: GattServiceLocation

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

        serviceLocation = GattServiceLocation(this.queueingGatt, this::startLocationUpdates)
        gattCallbackRelay.addCallback(serviceLocation)

        serviceLocation.init()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val provider = LocationManager.FUSED_PROVIDER
        val executor = context.mainExecutor

        val locationRequest = LocationRequest.Builder(1000)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()

        locationManager.getCurrentLocation(
            provider, locationRequest, locationCancellationSignal, executor, this::setLocation
        )
        locationManager.requestLocationUpdates(
            provider, locationRequest, executor, locationUpdateListener
        )
    }

    fun setLocation(location: Location?) {
        Log.d(TAG, "setLocation($location)")
        if (location == null) return

        this.serviceLocation.writeLocation(location, Calendar.getInstance())
    }

    private fun onDisconnected() {
        Log.i(TAG, "gatt disconnected")
        stopLocationUpdates()
        gatt.close()
        this.onClose()
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationUpdateListener)
        locationCancellationSignal.cancel()
    }

    companion object {
        const val TAG = "SonyBleCamClient"
    }
}