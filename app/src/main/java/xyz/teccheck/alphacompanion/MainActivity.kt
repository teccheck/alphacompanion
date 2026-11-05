package xyz.teccheck.alphacompanion

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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.manager.CameraManager
import xyz.teccheck.alphacompanion.ui.theme.AlphaCompanionTheme
import xyz.teccheck.alphacompanion.ui.widgets.PairedCamerasList

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
    fun TopBar(modifier: Modifier = Modifier, showBackButton: Boolean, onBack: () -> Unit) {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            navigationIcon = {
                if (showBackButton) {
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
                }
            },
            title = {
                Text(stringResource(R.string.app_name))
            },
        )
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
            onClick = { },
            onRemove = { scope.launch { cameraManager.removeCamera(it) } },
        )
    }
}