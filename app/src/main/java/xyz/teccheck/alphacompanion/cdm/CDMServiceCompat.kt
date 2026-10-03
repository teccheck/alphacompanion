package xyz.teccheck.alphacompanion.cdm

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import xyz.teccheck.alphacompanion.LogUtils.typeToString

abstract class CDMServiceCompat : CompanionDeviceService() {

    protected val companionDeviceManager: CompanionDeviceManager by lazy {
        applicationContext.getSystemService()!!
    }

    protected val bluetoothManager: BluetoothManager by lazy {
        applicationContext.getSystemService()!!
    }

    abstract fun deviceAppeared(device: BluetoothDevice)

    abstract fun deviceDisappeared(device: BluetoothDevice)

    // Used on API 31 and 32
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(address: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        Log.d(TAG, "Appeared [V1]: $address")
        val device = bluetoothManager.adapter.getRemoteDevice(address) ?: return
        deviceAppeared(device)
    }

    // Used on API 33, 34 and 35
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) return

        Log.d(TAG, "Appeared [V2]: $associationInfo")
        val device = bluetoothManager.getAssociatedBtDevice(associationInfo) ?: return
        deviceAppeared(device)
    }

    // Used on API 31 and 32
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(address: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        Log.d(TAG, "Disappeared [V1]: $address")
        val device = bluetoothManager.adapter.getRemoteDevice(address) ?: return
        deviceDisappeared(device)
    }

    // Used on API 33, 34 and 35
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) return

        Log.d(TAG, "Disappeared [V2]: $associationInfo")
        val device = bluetoothManager.getAssociatedBtDevice(associationInfo) ?: return
        deviceDisappeared(device)
    }

    // Used since API 36
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        Log.d(TAG, "Event: ${event.typeToString()}, Id, ${event.associationId}")
        val info = companionDeviceManager.getAssociatedDevice(event.associationId) ?: return
        val device = bluetoothManager.getAssociatedBtDevice(info) ?: return

        when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> deviceAppeared(device)
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> deviceDisappeared(device)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun CompanionDeviceManager.getAssociatedDevice(id: Int) =
        this.myAssociations.find { it.id == id }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun BluetoothManager.getAssociatedBtDevice(info: AssociationInfo): BluetoothDevice? =
        this.adapter.getRemoteDevice(info.deviceMacAddress.toString().uppercase())

    companion object {
        private const val TAG = "CDMServiceCompat"
    }
}