package xyz.teccheck.alphacompanion.cdm

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import xyz.teccheck.alphacompanion.model.AssociatedDeviceCompat
import xyz.teccheck.alphacompanion.model.SonyCamManufacturerData
import xyz.teccheck.alphacompanion.model.toAssociatedDevice

@RequiresApi(Build.VERSION_CODES.O)
fun Intent.getAssociationResult(): AssociatedDeviceCompat? {
    var result: AssociatedDeviceCompat? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        result = getParcelableExtra(
            CompanionDeviceManager.EXTRA_ASSOCIATION,
            AssociationInfo::class.java,
        )?.toAssociatedDevice()
    } else {
        // Below Android 33 the result returns either a BLE ScanResult, a
        // Classic BluetoothDevice or a Wifi ScanResult
        // In our case we are looking for our BLE GATT server so we can cast directly
        // to the BLE ScanResult
        @Suppress("DEPRECATION")
        val scanResult = getParcelableExtra<ScanResult>(CompanionDeviceManager.EXTRA_DEVICE)
        if (scanResult != null) {
            result = AssociatedDeviceCompat(
                id = scanResult.advertisingSid,
                address = scanResult.device.address ?: "N/A",
                name = scanResult.scanRecord?.deviceName ?: "N/A",
                device = scanResult.device,
            )
        }
    }
    return result
}

fun buildSonyCameraScanFilter(): ScanFilter = ScanFilter.Builder().setManufacturerData(
    SonyCamManufacturerData.SONY_ID.toInt(),
    byteArrayOf(0x03, 0x00)
).build()
