package xyz.teccheck.alphacompanion.cdm

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.companion.ObservingDevicePresenceRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import xyz.teccheck.alphacompanion.LogUtils.typeToString

abstract class CDMServiceCompat : CompanionDeviceService() {

    protected val companionDeviceManager: CompanionDeviceManager by lazy {
        applicationContext.getSystemService()!!
    }

    abstract fun deviceAppeared(address: String)

    abstract fun deviceDisappeared(address: String)

    // Used on API 31 and 32
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(address: String) {
        super.onDeviceAppeared(address)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        Log.d(TAG, "Appeared [V1]: $address")
        deviceAppeared(address)
    }

    // Used on API 33, 34 and 35
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) return

        Log.d(TAG, "Appeared [V2]: $associationInfo")
        deviceAppeared(associationInfo.deviceMacAddress.toString())
    }

    // Used on API 31 and 32
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(address: String) {
        super.onDeviceDisappeared(address)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        Log.d(TAG, "Disappeared [V1]: $address")

        // Nasty hack, see comment on onDevicePresenceEvent
        companionDeviceManager.stopObservingDevicePresence(address)
        companionDeviceManager.startObservingDevicePresence(address)

        deviceDisappeared(address)
    }

    // Used on API 33, 34 and 35
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        super.onDeviceDisappeared(associationInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) return

        Log.d(TAG, "Disappeared [V2]: $associationInfo")

        val address = associationInfo.deviceMacAddress.toString()

        // Nasty hack, see comment on onDevicePresenceEvent
        companionDeviceManager.stopObservingDevicePresence(address)
        companionDeviceManager.startObservingDevicePresence(address)

        deviceDisappeared(address)
    }

    // Used since API 36
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        super.onDevicePresenceEvent(event)
        Log.d(TAG, "Event: ${event.typeToString()}, Id, ${event.associationId}")
        val info = companionDeviceManager.getAssociatedDevice(event.associationId) ?: return
        val address = info.deviceMacAddress.toString()

        when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> deviceAppeared(address)
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> {
                // Nasty hack. This is needed to get future events for this device.
                // If there are no other devices associated on the system, the CDM will no scan.
                // When a BT device disconnects, the CDM tries to restart the ble scanner.
                // If there are no disconnected devices, it will not scan.
                // However, the just disconnected device is not removed from the connected devices
                // list yet, so no scan is started. Therefore, no events will be issued.
                // Have a look at:
                // https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/services/companion/java/com/android/server/companion/devicepresence/DevicePresenceProcessor.java;drc=cc76884010e63fd37f1bccb4f0131dd21a9bf27e;l=714
                // https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/services/companion/java/com/android/server/companion/devicepresence/BleDeviceProcessor.java;drc=ff2cd5abe1c99e6495710dd83892b9145ebb7f8a;l=171
                // This hack just forces the CDM to recheck if there are disconnected devices.
                // Only calling startObservingDevicePresence will just cause a noop.
                // Also, only the deprecated methods seem to work.
                // This took me way to long to figure out!!!
                companionDeviceManager.stopObservingDevicePresence(address)
                companionDeviceManager.startObservingDevicePresence(address)

                deviceDisappeared(address)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun CompanionDeviceManager.getAssociatedDevice(id: Int) =
        this.myAssociations.find { it.id == id }

    companion object {
        private const val TAG = "CDMServiceCompat"
    }
}