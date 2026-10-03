package xyz.teccheck.alphacompanion.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log

class LocationBroadcastReceiver(private val listener: LocationListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive")

        if (intent?.action != LOCATION_BROADCAST_ACTION) return

        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED, Location::class.java)
        } else {
            intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED)
        }

        location?.let { listener.onLocationChanged(it) }

        if (!intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) return

        val enabled = intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false)
        if (enabled) {
            listener.onProviderEnabled("")
        } else {
            listener.onProviderDisabled("")
        }
    }

    companion object {
        const val TAG = "LocationBroadcastReceiver"
        const val LOCATION_BROADCAST_ACTION = "xyz.teccheck.alphacompanion.LOCATION_UDPATE"
    }
}