package xyz.teccheck.alphacompanion.ui.widgets

import android.companion.CompanionDeviceManager
import android.content.IntentSender
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.cdm.getAssociationResult
import xyz.teccheck.alphacompanion.model.AssociatedDeviceCompat

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScanForDevicesMenu(
    deviceManager: CompanionDeviceManager,
    onRequestDeviceAssociation: suspend (CompanionDeviceManager) -> IntentSender,
    onDeviceAssociated: (AssociatedDeviceCompat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var errorMessage by remember {
        mutableStateOf("")
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        when (it.resultCode) {
            CompanionDeviceManager.RESULT_OK -> {
                it.data?.getAssociationResult()?.run {
                    onDeviceAssociated(this)
                }
            }

            CompanionDeviceManager.RESULT_CANCELED -> {
                errorMessage = "The request was canceled"
            }

            CompanionDeviceManager.RESULT_INTERNAL_ERROR -> {
                errorMessage = "Internal error happened"
            }

            CompanionDeviceManager.RESULT_DISCOVERY_TIMEOUT -> {
                errorMessage = "No device matching the given filter were found"
            }

            CompanionDeviceManager.RESULT_USER_REJECTED -> {
                errorMessage = "The user explicitly declined the request"
            }

            else -> {
                errorMessage = "Unknown error"
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = "Find & associate another device running the GATTServerSample",
            )
            Button(
                modifier = Modifier.weight(0.3f),
                onClick = {
                    scope.launch {
                        val intentSender =
                            onRequestDeviceAssociation(
                                deviceManager
                            )
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                },
            ) {
                Text(text = "Start")
            }
        }
        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}