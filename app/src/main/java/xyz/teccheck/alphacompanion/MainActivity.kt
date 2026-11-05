package xyz.teccheck.alphacompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.manager.CameraManager
import xyz.teccheck.alphacompanion.ui.theme.AlphaCompanionTheme
import xyz.teccheck.alphacompanion.ui.widgets.PairedCamerasList
import xyz.teccheck.alphacompanion.ui.widgets.TopBar

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UiContent()
        }
    }

    @Composable
    fun UiContent() {
        AlphaCompanionTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopBar(
                        showBackButton = false,
                        onBack = {}
                    )
                }) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    DevicesScreen(application.alphaCompanion().cameraManager)
                }
            }
        }
    }



    @Composable
    private fun DevicesScreen(
        cameraManager: CameraManager,
    ) {
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
            onResult = {
                scope.launch {
                    cameraManager.cameraAssociated(it)
                }
            }
        )

        val cameras by cameraManager.getPairedCameras().collectAsStateWithLifecycle(emptyList())

        PairedCamerasList(
            cameras,
            onPairNew = {
                scope.launch {
                    val intentSender = cameraManager.requestDeviceAssociation(it)
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            },
            onClick = {
                val intent = Intent(this, DeviceDetailsActivity::class.java)
                intent.putExtra(DeviceDetailsActivity.EXTRA_ADDRESS, it.macAddress)
                startActivity(intent)
            },
            onRemove = { scope.launch { cameraManager.removeCamera(it) } },
        )
    }
}