package xyz.teccheck.alphacompanion.cdm

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.core.content.ContextCompat
import xyz.teccheck.alphacompanion.location.LocationTransferService

class CDMService : CDMServiceCompat() {

    override fun deviceAppeared(device: BluetoothDevice) {
        val intent = Intent(this, LocationTransferService::class.java)
        intent.action = LocationTransferService.ACTION_DEVICE_APPEARED
        intent.putExtra(LocationTransferService.EXTRA_DEVICE, device)

        ContextCompat.startForegroundService(this, intent)
    }

    override fun deviceDisappeared(device: BluetoothDevice) {
        val intent = Intent(this, LocationTransferService::class.java)
        intent.action = LocationTransferService.ACTION_DEVICE_DISAPPEARED
        intent.putExtra(LocationTransferService.EXTRA_DEVICE, device)

        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        const val TAG = "CDMService"
    }
}