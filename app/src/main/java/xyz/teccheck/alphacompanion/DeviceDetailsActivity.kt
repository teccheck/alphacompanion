package xyz.teccheck.alphacompanion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ClipData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.db.SonyCamera
import xyz.teccheck.alphacompanion.manager.CameraManager
import xyz.teccheck.alphacompanion.ui.theme.AlphaCompanionTheme
import xyz.teccheck.alphacompanion.ui.widgets.TopBar

class DeviceDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphaCompanionTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopBar(
                            showBackButton = true,
                            onBack = { finish() }
                        )
                    }) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    ) {
                        val address = intent.getStringExtra(EXTRA_ADDRESS)?.uppercase()

                        val btMan: BluetoothManager = getSystemService() ?: return@Box
                        val device = btMan.adapter.getRemoteDevice(address) ?: return@Box

                        val cameraManager = application.alphaCompanion().cameraManager

                        ConnectDeviceScreen(device, cameraManager)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "address"
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, cameraManager: CameraManager) {
    val scope = rememberCoroutineScope()

    var state by remember(device) {
        mutableStateOf<DeviceConnectionState?>(null)
    }

    BLEConnectEffect(device = device) {
        state = it
    }

    val cameras by cameraManager.getPairedCameras().collectAsStateWithLifecycle(emptyList())
    val camera = cameras.find { it.macAddress.equals(device.address, true) }
    val clipboard = LocalClipboard.current
    val details = DeviceDetails(camera, state)

    DeviceDetailsView(
        details,
        onCopy = {
            scope.launch {
                val clipData = ClipData.newPlainText("Device Details", buildClipboardText(details))
                val clipEntry = ClipEntry(clipData)
                clipboard.setClipEntry(clipEntry)
            }
        },
        onDiscover = {
            scope.launch(Dispatchers.IO) {
                state?.gatt?.discoverServices()
            }
        },
        onGetMtu = {
            scope.launch(Dispatchers.IO) {
                if (state?.connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                    state?.gatt?.connect()
                }
                state?.gatt?.requestMtu(158)
            }
        },
    )
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
private fun BLEConnectEffect(
    device: BluetoothDevice,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStateChange: (DeviceConnectionState) -> Unit,
) {
    val context = LocalContext.current
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    // Keep the current connection state
    var state by remember {
        mutableStateOf(DeviceConnectionState.None)
    }

    DisposableEffect(lifecycleOwner, device) {
        // This callback will notify us when things change in the GATT connection so we can update
        // our state
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                super.onConnectionStateChange(gatt, status, newState)
                state = state.copy(gatt = gatt, connectionState = newState)
                currentOnStateChange(state)

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    gatt.requestMtu(158)
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                state = state.copy(gatt = gatt, mtu = mtu)
                currentOnStateChange(state)
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                state = state.copy(services = gatt.services)
                currentOnStateChange(state)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (state.gatt != null) {
                    // If we previously had a GATT connection let's reestablish it
                    state.gatt?.connect()
                } else {
                    // Otherwise create a new GATT connection
                    state = state.copy(gatt = device.connectGatt(context, false, callback))
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Unless you have a reason to keep connected while in the bg you should disconnect
                state.gatt?.disconnect()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and close the connection
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.gatt?.close()
            state = DeviceConnectionState.None
        }
    }
}

@Composable
private fun DeviceDetailsView(
    details: DeviceDetails,
    onCopy: () -> Unit,
    onDiscover: () -> Unit,
    onGetMtu: () -> Unit
) {
    val camera = details.camera
    val state = details.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Name: ${camera?.name}")
        Text(text = "Address: ${camera?.macAddress}")
        Text(text = "Association ID: ${camera?.associationId}")
        Text(text = "Model Version: ${camera?.modelVersion?.toHexString()}")
        Text(text = "Model Code: ${camera?.modeCode}")
        Text(text = "Scan Record: ${camera?.scanRecordData}")

        Text(text = "Status: ${state?.connectionState?.toConnectionStateString()}")
        Text(text = "MTU: ${state?.mtu}")

        Text(text = "Services:\n${LogUtils.buildServicesOverview(state?.services ?: listOf())}")

        Button(onClick = onCopy) { Text(text = "Copy for bugreport") }
        Button(onClick = onGetMtu) { Text(text = "Request MTU") }
        Button(enabled = state?.gatt != null, onClick = onDiscover) { Text(text = "Discover") }
    }
}

private data class DeviceDetails(val camera: SonyCamera?, val state: DeviceConnectionState?)

private fun buildClipboardText(deviceDetails: DeviceDetails): String {
    return "${deviceDetails.camera}\nState: ${deviceDetails.state?.connectionState?.toConnectionStateString()}\nServices:\n${
        LogUtils.buildServicesOverview(
            deviceDetails.state?.services ?: listOf()
        )
    }"
}

internal fun Int.toConnectionStateString() = when (this) {
    BluetoothProfile.STATE_CONNECTED -> "Connected"
    BluetoothProfile.STATE_CONNECTING -> "Connecting"
    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
    else -> "N/A"
}

private data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    val services: List<BluetoothGattService> = emptyList(),
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }
}