package xyz.teccheck.alphacompanion.model

import android.bluetooth.le.ScanRecord
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.BitSet

data class SonyCamManufacturerData(
    val isCameraOn: Boolean = false,
    val isLocationFunctionEnabled: Boolean = false,
    val isLocationFunctionSupported: Boolean = false,
    val isPairingEnabled: Boolean = false,
    val isPairingSupported: Boolean = false,
    val isPushTransferNotificationEnabled: Boolean = false,
    val isPushTransferNotificationSupported: Boolean = false,
    val isSmartphoneImageTransferEnabled: Boolean = false,
    val isSmartphoneImageTransferSupported: Boolean = false,
    val isSmartphoneRemoteControlEnabled: Boolean = false,
    val isSmartphoneRemoteControlSupported: Boolean = false,
    val isWifiHandoverEnabled: Boolean = false,
    val isWifiHandoverSupported: Boolean = false,
    val isWirelessPowerOnEnabled: Boolean = false,
    val modelCode: String = "",
    val version: Short = 0,
) {
    fun isUxpSupported(): Boolean {
        return this.modelCode.startsWith("U")
    }

    fun isFunctionEnabled(function: SonyCamBleFunction): Boolean = when (function) {
        SonyCamBleFunction.RemotePowerOn -> isWirelessPowerOnEnabled
        SonyCamBleFunction.RemotePowerOff -> isWirelessPowerOnEnabled
        SonyCamBleFunction.Pairing -> isPairingEnabled
        SonyCamBleFunction.LocationInfoTransfer -> isLocationFunctionEnabled
        SonyCamBleFunction.WifiHandover -> isWifiHandoverEnabled
        SonyCamBleFunction.SmartphoneRemoteControl -> isSmartphoneRemoteControlEnabled
        SonyCamBleFunction.SmartphoneImageTransfer -> isSmartphoneImageTransferEnabled
        SonyCamBleFunction.PushTransferNotification -> isPushTransferNotificationEnabled
        SonyCamBleFunction.Unknown -> false
        SonyCamBleFunction.Any -> false
    }

    fun isFunctionSupported(function: SonyCamBleFunction): Boolean = when (function) {
        SonyCamBleFunction.RemotePowerOn -> isWirelessPowerOnEnabled
        SonyCamBleFunction.RemotePowerOff -> isWirelessPowerOnEnabled
        SonyCamBleFunction.Pairing -> isPairingSupported
        SonyCamBleFunction.LocationInfoTransfer -> isLocationFunctionSupported
        SonyCamBleFunction.WifiHandover -> isWifiHandoverSupported
        SonyCamBleFunction.SmartphoneRemoteControl -> isSmartphoneRemoteControlSupported
        SonyCamBleFunction.SmartphoneImageTransfer -> isSmartphoneImageTransferSupported
        SonyCamBleFunction.PushTransferNotification -> isPushTransferNotificationSupported
        SonyCamBleFunction.Unknown -> false
        SonyCamBleFunction.Any -> false
    }

    fun getCameraModel() = when(version) {
        VERSION_ILCE_6400 -> SonyCameraModel.ILCE_6400
        else -> SonyCameraModel.UNKNOWN
    }

    companion object {
        const val TAG = "SonyCamManufacturerData"
        const val SONY_ID: Short = 0x012D
        const val SONY_CAM_ID: Short = 0x0003
        const val TAG_GENERAL: Byte = 33
        const val TAG_BLE: Byte = 34
        const val TAG_PHONE: Byte = 35

        const val VERSION_ILCE_6400: Short = 0x6400

        val INVALID = SonyCamManufacturerData(modelCode = "⚡⚡")

        fun fromScanRecord(scanRecord: ScanRecord): SonyCamManufacturerData? {
            val sonyData = scanRecord.getManufacturerSpecificData(SONY_ID.toInt()) ?: return null
            return parse(sonyData)
        }

        fun parse(rawData: ByteArray): SonyCamManufacturerData? {
            val buffer = ByteBuffer.wrap(rawData)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val isSonyCamera = buffer.getShort() == SONY_CAM_ID
            if (!isSonyCamera) {
                return null
            }

            buffer.order(ByteOrder.BIG_ENDIAN)
            val version = buffer.getShort()
            val modelCode = String(byteArrayOf(buffer.get(), buffer.get()), StandardCharsets.UTF_8)

            var isCameraOn = false
            var isLocationFunctionEnabled = false
            var isLocationFunctionSupported = false
            var isPairingEnabled = false
            var isPairingSupported = false
            var isPushTransferNotificationEnabled = false
            var isPushTransferNotificationSupported = false
            var isSmartphoneImageTransferEnabled = false
            var isSmartphoneImageTransferSupported = false
            var isSmartphoneRemoteControlEnabled = false
            var isSmartphoneRemoteControlSupported = false
            var isWifiHandoverEnabled = false
            var isWifiHandoverSupported = false
            var isWirelessPowerOnEnabled = false

            while (buffer.hasRemaining()) {
                val tag = buffer.get()
                if (tag == 0.toByte()) {
                    break
                }

                when (tag) {
                    TAG_GENERAL -> {
                        val flags = BitSet.valueOf(byteArrayOf(buffer.get()))
                        isWirelessPowerOnEnabled = flags.get(7)
                        isCameraOn = flags.get(6)
                        isWifiHandoverSupported = flags.get(5)
                        isWifiHandoverEnabled = flags.get(4)
                    }

                    TAG_BLE -> {
                        val flags = BitSet.valueOf(byteArrayOf(buffer.get()))
                        isPairingSupported = flags.get(7)
                        isPairingEnabled = flags.get(6)
                        isLocationFunctionSupported = flags.get(5)
                        isLocationFunctionEnabled = flags.get(4)
                    }

                    TAG_PHONE -> {
                        val flags = BitSet.valueOf(byteArrayOf(buffer.get()))
                        isSmartphoneRemoteControlSupported = flags.get(7)
                        isSmartphoneRemoteControlEnabled = flags.get(5)
                        isSmartphoneImageTransferSupported = flags.get(4)
                        isSmartphoneImageTransferEnabled = flags.get(2)
                        isPushTransferNotificationSupported = flags.get(1)
                        isPushTransferNotificationEnabled = flags.get(0)
                    }

                    else -> Log.w(TAG, "unknown length/tag")
                }
            }

            return SonyCamManufacturerData(
                isCameraOn,
                isLocationFunctionEnabled,
                isLocationFunctionSupported,
                isPairingEnabled,
                isPairingSupported,
                isPushTransferNotificationEnabled,
                isPushTransferNotificationSupported,
                isSmartphoneImageTransferEnabled,
                isSmartphoneImageTransferSupported,
                isSmartphoneRemoteControlEnabled,
                isSmartphoneRemoteControlSupported,
                isWifiHandoverEnabled,
                isWifiHandoverSupported,
                isWirelessPowerOnEnabled,
                modelCode,
                version
            )
        }
    }
}