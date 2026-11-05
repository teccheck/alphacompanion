package xyz.teccheck.alphacompanion.manager

import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import xyz.teccheck.alphacompanion.ble.buildSonyCameraScanFilter
import xyz.teccheck.alphacompanion.db.AppDatabase
import xyz.teccheck.alphacompanion.db.SonyCamera
import xyz.teccheck.alphacompanion.model.SonyCamManufacturerData
import java.util.concurrent.Executor

class CameraManager(
    private val database: AppDatabase,
    private val cdm: CompanionDeviceManager,
) {
    suspend fun cleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            database.sonyCameraDao().retainById(cdm.myAssociations.map { it.id })
        } else {
            database.sonyCameraDao().retainByAddress(cdm.associations)
        }
    }

    fun getPairedCameras(): Flow<List<SonyCamera>> {
        return database.sonyCameraDao().getAll()
    }

    suspend fun addCamera(camera: SonyCamera) {
        Log.d(TAG, "Add Camera $camera")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val req =
                ObservingDevicePresenceRequest.Builder().setAssociationId(camera.associationId)
                    .build()
            cdm.startObservingDevicePresence(req)
        } else {
            cdm.startObservingDevicePresence(camera.macAddress)
        }

        database.sonyCameraDao().insertAll(camera)
    }

    suspend fun removeCamera(camera: SonyCamera) {
        Log.d(TAG, "Remove Camera $camera")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cdm.disassociate(camera.associationId)
        } else {
            cdm.disassociate(camera.macAddress)
        }

        cleanup()
    }

    suspend fun requestDeviceAssociation(disableDeviceFilter: Boolean): IntentSender {
        val deviceFilter = BluetoothLeDeviceFilter.Builder()
            .setScanFilter(buildSonyCameraScanFilter())
            .build()

        val builder = AssociationRequest.Builder()

        if (!disableDeviceFilter) builder.addDeviceFilter(deviceFilter)

        val pairingRequest: AssociationRequest = builder
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
            cdm.associate(pairingRequest, executor, callback)
        } else {
            cdm.associate(pairingRequest, callback, null)
        }
        return result.await()
    }

    suspend fun cameraAssociated(result: ActivityResult) {
        if (result.resultCode != CompanionDeviceManager.RESULT_OK) return
        if (result.data == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val association = result.data!!.getParcelableExtra(
                CompanionDeviceManager.EXTRA_ASSOCIATION,
                AssociationInfo::class.java,
            ) ?: return

            val scanRecord = association.associatedDevice?.bleDevice?.scanRecord ?: return
            val manufacturerData = SonyCamManufacturerData.fromScanRecord(scanRecord)
                ?: SonyCamManufacturerData.INVALID

            val camera = SonyCamera(
                association.deviceMacAddress.toString(),
                association.id,
                association.displayName.toString(),
                manufacturerData.version,
                manufacturerData.modelCode,
                scanRecord.bytes.toHexString()
            )

            addCamera(camera)
        } else {
            @Suppress("DEPRECATION")
            val scanResult =
                result.data!!.getParcelableExtra<ScanResult>(CompanionDeviceManager.EXTRA_DEVICE)
                    ?: return

            val scanRecord = scanResult.scanRecord ?: return
            val manufacturerData =
                SonyCamManufacturerData.fromScanRecord(scanRecord)
                    ?: return

            val camera = SonyCamera(
                scanResult.device.address,
                0,
                scanResult.device.name,
                manufacturerData.version,
                manufacturerData.modelCode,
                scanRecord.bytes.toHexString()
            )

            addCamera(camera)
        }
    }

    companion object {
        const val TAG = "CameraManager"
    }
}
