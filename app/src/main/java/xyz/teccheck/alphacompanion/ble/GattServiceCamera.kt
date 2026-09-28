package xyz.teccheck.alphacompanion.ble

import android.bluetooth.BluetoothGattCharacteristic

class GattServiceCamera(gatt: QueueingBluetoothGatt) :
    GattService("8000CC00-CC00-FFFF-FFFF-FFFFFFFFFFFF", gatt) {

    private val unseenCharacteristic01: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC01")!!

    private val powerOffCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC02")!!

    private val pushTransferNotificationCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC03")!!

    private val unseenCharacteristic04: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC04")!!

    private val wifiLaunchStatusCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC05")!!

    private val wifiSsidCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC06")!!

    private val wifiPasswordCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC07")!!

    private val wifiOnCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC08")!!

    private val cameraStatusCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC09")!!

    private val firmwareVersionCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC0A")!!

    private val modelCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC0B")!!

    private val wifiBssidCharacteristic: BluetoothGattCharacteristic =
        this.registerCharacteristic("0000CC0C")!!
}