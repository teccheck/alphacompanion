package xyz.teccheck.alphacompanion.ble

import android.bluetooth.le.ScanFilter
import xyz.teccheck.alphacompanion.model.SonyCamManufacturerData

fun buildSonyCameraScanFilter(): ScanFilter = ScanFilter.Builder().setManufacturerData(
    SonyCamManufacturerData.SONY_ID.toInt(),
    byteArrayOf(0x03, 0x00)
).build()
