package xyz.teccheck.alphacompanion

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.cdm.buildSonyCameraScanFilter
import xyz.teccheck.alphacompanion.model.AssociatedDeviceCompat
import xyz.teccheck.alphacompanion.model.getAssociatedDevices
import xyz.teccheck.alphacompanion.ui.theme.AlphaCompanionTheme
import xyz.teccheck.alphacompanion.ui.widgets.AssociatedDevicesList
import xyz.teccheck.alphacompanion.ui.widgets.ConnectDeviceScreen
import xyz.teccheck.alphacompanion.ui.widgets.PermissionBox
import java.util.concurrent.Executor

private const val TAG = "CDMActivity"

class CDMActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UiContent()
        }
    }

    @Composable
    fun UiContent() {
        val context = LocalContext.current
        val deviceManager = context.getSystemService<CompanionDeviceManager>()
        val adapter = context.getSystemService<BluetoothManager>()?.adapter
        var selectedDevice by remember {
            mutableStateOf<BluetoothDevice?>(null)
        }

        AlphaCompanionTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { TopBar(
                    onBack = {
                        selectedDevice = null
                    }
                ) }) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    if (deviceManager == null || adapter == null) {
                        Text(text = "No Companion device manager found. The device does not support it.")
                    } else {
                        if (selectedDevice == null) {
                            DevicesScreen(deviceManager) { device ->
                                selectedDevice = (device.device ?: adapter.getRemoteDevice(device.address))
                            }
                        } else {
                            PermissionBox(permission = Manifest.permission.BLUETOOTH_CONNECT) {
                                ConnectDeviceScreen(device = selectedDevice!!) {
                                    selectedDevice = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TopBar(modifier: Modifier = Modifier, onBack: () -> Unit) {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            navigationIcon = {
                FilledIconButton(
                    modifier = Modifier.padding(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = onBack
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.content_description_back_icon),
                    )
                }
            },
            title = {
                Text(stringResource(R.string.app_name))
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun DevicesScreen(
        deviceManager: CompanionDeviceManager,
        onConnect: (AssociatedDeviceCompat) -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        var associatedDevices by remember {
            // If we already associated the device no need to do it again.
            mutableStateOf(deviceManager.getAssociatedDevices())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LaunchedEffect(associatedDevices) {
                associatedDevices.forEach {
                    deviceManager.startObservingDevicePresence(it.address)
                }
            }
        }

        AssociatedDevicesList(
            associatedDevices = associatedDevices,
            onConnect = onConnect,
            onDisassociate = {
                scope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        deviceManager.disassociate(it.id)
                    } else {
                        @Suppress("DEPRECATION") deviceManager.disassociate(it.address)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        deviceManager.stopObservingDevicePresence(it.address)
                    }
                    associatedDevices = deviceManager.getAssociatedDevices()
                }
            },
            deviceManager,
            this@CDMActivity::requestDeviceAssociation,
            onDeviceAssociated = {
                associatedDevices = associatedDevices + it
            }
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun requestDeviceAssociation(deviceManager: CompanionDeviceManager): IntentSender {
        val deviceFilter = BluetoothLeDeviceFilter.Builder()
            .setScanFilter(buildSonyCameraScanFilter())
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)
            .build()

        val result = CompletableDeferred<IntentSender>()

        val callback = object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                result.complete(intentSender)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onDeviceFound(intentSender: IntentSender) {
                result.complete(intentSender)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                // This callback was added in API 33 but the result is also send in the activity result.
                // For handling backwards compatibility we can just have all the logic there instead
            }

            override fun onFailure(errorMessage: CharSequence?) {
                result.completeExceptionally(
                    IllegalStateException(
                        errorMessage?.toString().orEmpty()
                    )
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val executor = Executor { it.run() }
            deviceManager.associate(pairingRequest, executor, callback)
        } else {
            deviceManager.associate(pairingRequest, callback, null)
        }
        return result.await()
    }
}