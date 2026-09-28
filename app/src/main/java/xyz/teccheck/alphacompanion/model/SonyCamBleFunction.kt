package xyz.teccheck.alphacompanion.model

enum class SonyCamBleFunction {
    Unknown,
    RemotePowerOn,
    RemotePowerOff,
    Pairing,
    LocationInfoTransfer,
    WifiHandover,
    SmartphoneRemoteControl,
    SmartphoneImageTransfer,
    PushTransferNotification,
    Any;

    companion object {
        val RemotePowerOnOff = arrayOf(
            RemotePowerOn,
            RemotePowerOff
        )
    }
}