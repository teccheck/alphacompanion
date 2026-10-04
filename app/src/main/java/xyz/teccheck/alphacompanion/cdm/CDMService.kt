package xyz.teccheck.alphacompanion.cdm

import android.content.Intent
import androidx.core.content.ContextCompat
import xyz.teccheck.alphacompanion.location.LocationTransferService

class CDMService : CDMServiceCompat() {

    override fun deviceAppeared(address: String) {
        val intent = Intent(this, LocationTransferService::class.java)
        intent.action = LocationTransferService.ACTION_DEVICE_APPEARED
        intent.putExtra(LocationTransferService.EXTRA_DEVICE_ADDRESS, address)

        ContextCompat.startForegroundService(this, intent)
    }

    override fun deviceDisappeared(address: String) {}

    companion object {
        const val TAG = "CDMService"
    }
}