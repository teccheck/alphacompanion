package xyz.teccheck.alphacompanion.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.net.MacAddress
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import xyz.teccheck.alphacompanion.R
import xyz.teccheck.alphacompanion.ble.SonyBleCamClient

class LocationTransferService : Service() {
    private val activeClients: MutableMap<MacAddress, SonyBleCamClient> = mutableMapOf()
    private val scanningDevices = mutableListOf<String>()
    private val scanTimeoutHandler = Handler(Looper.getMainLooper())

    private val notificationManager: NotificationManager by lazy {
        applicationContext.getSystemService()!!
    }

    private val bluetoothManager: BluetoothManager by lazy {
        applicationContext.getSystemService()!!
    }

    private val locationManager: LocationManager by lazy {
        applicationContext.getSystemService()!!
    }

    private val locationCancellationSignal = CancellationSignal()
    private val locationsListener = LocationListener { setLocation(it) }

    private fun setLocation(location: Location?) {
        activeClients.forEach { (_, client) -> client.setLocation(location) }
    }

    private fun addCameraClient(device: BluetoothDevice) {
        Log.d(TAG, "addCameraClient $device")

        val address = MacAddress.fromString(device.address)
        val client = SonyBleCamClient(applicationContext, device) {
            removeCameraClient(device)
        }
        activeClients[address] = client

        // If we register a second device, location updates are already running
        if (activeClients.size == 1) {
            startLocationUpdates()
        }

        updateNotification()
    }

    private fun removeCameraClient(device: BluetoothDevice) {
        val address = MacAddress.fromString(device.address)
        Log.i(TAG, "Removing inactive client for $address")
        activeClients.remove(address)

        if (activeClients.isEmpty()) {
            stopLocationUpdates()
        }

        startDeviceScan(device)

        updateNotification()
    }

    private fun startLocationUpdates() {
        val executor = applicationContext.mainExecutor
        val provider = LocationManager.FUSED_PROVIDER
        val locationRequest = LocationRequest.Builder(1_000)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        locationManager.getCurrentLocation(
            provider, locationRequest, locationCancellationSignal, executor, this::setLocation
        )

        locationManager.requestLocationUpdates(
            provider,
            locationRequest,
            executor,
            locationsListener
        )
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationsListener)
    }

    private fun updateNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification()
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_title),
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val content = when (activeClients.size) {
            0 -> getString(R.string.notification_text_link_none)
            1 -> {
                val name = activeClients.entries.first().value.getDeviceName()
                getString(R.string.notification_text_link_one, name)
            }

            else -> {
                resources.getQuantityString(
                    R.plurals.notification_text_link_multiple,
                    activeClients.size,
                    activeClients.size
                )
            }
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_gps_fixed_24)
            .setOngoing(true)
            .build()
    }

    private fun startDeviceScan(device: BluetoothDevice) {
        //scanTimeoutHandler.postDelayed()
    }

    private fun maybeStopSelf() {
        if (activeClients.isEmpty()) stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        createNotificationChannel()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG, "onStartCommand")
        if (intent == null) return START_STICKY

        val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return START_STICKY
        val device =
            bluetoothManager.adapter.getRemoteDevice(address.uppercase()) ?: return START_STICKY

        when (intent.action) {
            ACTION_DEVICE_APPEARED -> addCameraClient(device)
            ACTION_DEVICE_DISAPPEARED -> removeCameraClient(device)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        stopLocationUpdates()
        locationCancellationSignal.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val TAG = "LocationTransferService"
        private const val NOTIFICATION_ID = 0x1337
        private const val NOTIFICATION_CHANNEL_ID = "location_transfer"

        const val ACTION_DEVICE_APPEARED = "device_appeared"
        const val ACTION_DEVICE_DISAPPEARED = "device_disappeared"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}