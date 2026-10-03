package xyz.teccheck.alphacompanion.cdm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.net.MacAddress
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import xyz.teccheck.alphacompanion.ble.SonyBleCamClient
import xyz.teccheck.alphacompanion.ble.SonyBleCamClient.Companion.LOCATION_REQUEST_CODE
import xyz.teccheck.alphacompanion.location.LocationBroadcastReceiver

class CDMService : CDMServiceCompat() {

    private val notificationManager: DeviceNotificationManager by lazy {
        DeviceNotificationManager(applicationContext)
    }

    private val locationManager: LocationManager by lazy {
        applicationContext.getSystemService()!!
    }

    private val locationIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(LocationBroadcastReceiver.LOCATION_BROADCAST_ACTION),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
        )
    }

    private val locationBroadcastReceiver: LocationBroadcastReceiver by lazy {
        val receiver = LocationBroadcastReceiver(LocationListener {
            Log.d(TAG, "LocationListener $it")
            activeClients.forEach { (_, client) -> client.setLocation(it) }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                IntentFilter(LocationBroadcastReceiver.LOCATION_BROADCAST_ACTION),
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                receiver,
                IntentFilter(LocationBroadcastReceiver.LOCATION_BROADCAST_ACTION)
            )
        }

        return@lazy receiver
    }

    private val activeClients: MutableMap<MacAddress, SonyBleCamClient> = mutableMapOf()

    fun addCameraClient(device: BluetoothDevice) {
        val address = MacAddress.fromString(device.address)
        val client = SonyBleCamClient(applicationContext, device) {
            removeCameraClient(device)
        }
        activeClients[address] = client

        if (activeClients.size == 1) {
            startLocationUpdates()
        }
    }

    fun removeCameraClient(device: BluetoothDevice) {
        val address = MacAddress.fromString(device.address)
        Log.i(TAG, "Removing inactive client for $address")
        activeClients.remove(address)

        if (activeClients.isEmpty()) {
            stopLocationUpdates()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS])
    override fun deviceAppeared(device: BluetoothDevice) {
        val address = MacAddress.fromString(device.address)
        val status = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)

        notificationManager.onDeviceAppeared(
            address = address,
            status = "$status",
        )

        if (activeClients.contains(address)) {
            Log.w(TAG, "Client for $address already active")
            return
        }

        addCameraClient(device)

    }

    override fun deviceDisappeared(device: BluetoothDevice) {

    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        // This is a lazy hack to register the broadcast receiver once
        val receiver = locationBroadcastReceiver

        val provider = LocationManager.FUSED_PROVIDER
        val locationRequest = LocationRequest.Builder(1000)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()

        //locationManager.getCurrentLocation(
        //    provider, locationRequest, locationCancellationSignal, executor, this::setLocation
        //)
        locationManager.requestLocationUpdates(provider, locationRequest, locationIntent)
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationIntent)
    }

    /**
     * Check BLUETOOTH_CONNECT is granted and POST_NOTIFICATIONS is granted for devices running
     * Android 13 and above.
     */
    private fun missingPermissions(): Boolean = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) != PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED)

    /**
     * Utility class to post notification when CDM notifies that a device appears or disappears
     */
    private class DeviceNotificationManager(context: Context) {

        companion object {
            private const val CDM_CHANNEL = "cdm_channel"
        }

        private val manager = NotificationManagerCompat.from(context)

        private val notificationBuilder = NotificationCompat.Builder(context, CDM_CHANNEL)
            .setSmallIcon(IconCompat.createWithResource(context, context.applicationInfo.icon))
            .setContentTitle("Companion Device Manager Sample")

        init {
            createNotificationChannel()
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        fun onDeviceAppeared(address: MacAddress, status: String) {
            val notification =
                notificationBuilder.setContentText("Device: $address appeared.\nStatus: $status")
            manager.notify(address.hashCode(), notification.build())
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        fun onDeviceDisappeared(address: String) {
            val notification = notificationBuilder.setContentText("Device: $address disappeared")
            manager.notify(address.hashCode(), notification.build())
        }

        private fun createNotificationChannel() {
            val channel =
                NotificationChannelCompat.Builder(CDM_CHANNEL, NotificationManager.IMPORTANCE_HIGH)
                    .setName("CDM Sample").setDescription("Channel for the CDM sample").build()
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "CDMService"
    }
}