package xyz.teccheck.alphacompanion.ui.widgets

import android.annotation.SuppressLint
import android.companion.CompanionDeviceManager
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.teccheck.alphacompanion.R
import xyz.teccheck.alphacompanion.cdm.getAssociationResult
import xyz.teccheck.alphacompanion.model.AssociatedDeviceCompat
import xyz.teccheck.alphacompanion.ui.theme.AlphaCompanionTheme
import kotlin.math.abs

@Composable
fun AssociatedDevicesList(
    associatedDevices: List<AssociatedDeviceCompat>,
    onConnect: (AssociatedDeviceCompat) -> Unit,
    onDisassociate: (AssociatedDeviceCompat) -> Unit,
    deviceManager: CompanionDeviceManager?,
    onRequestDeviceAssociation: suspend (CompanionDeviceManager) -> IntentSender,
    onDeviceAssociated: (AssociatedDeviceCompat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        when (it.resultCode) {
            CompanionDeviceManager.RESULT_OK -> {
                it.data?.getAssociationResult()?.run {
                    onDeviceAssociated(this)
                }
            }
        }
    }

    val count = associatedDevices.size
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            SegmentedListItem(
                modifier = Modifier.height(72.dp),
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                colors = colors,
                content = { Text(text = stringResource(R.string.pair_new_camera)) },
                leadingContent = {
                    SettingsRowIcon(painterResource(R.drawable.baseline_add_24))
                },
                onClick = {
                    scope.launch {
                        val intentSender = onRequestDeviceAssociation(deviceManager!!)
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                modifier = Modifier.padding(8.dp, 0.dp),
                text = "Saved Devices",
                color = MaterialTheme.colorScheme.primary
            )
        }

        item { Spacer(Modifier.height(2.dp)) }

        itemsIndexed(associatedDevices) { index, device ->
            SegmentedListItem(
                modifier = Modifier.height(72.dp),
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                colors = colors,
                content = { Text(text = device.name) },
                supportingContent = { Text(text = device.address.uppercase()) },
                leadingContent = {
                    DeviceRainbowIcon(
                        painterResource(R.drawable.baseline_linked_camera_24),
                        device.address
                    )
                },
                trailingContent = {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VerticalDivider(
                            modifier = Modifier.padding(16.dp, 0.dp),
                            thickness = 2.dp,
                        )

                        IconButton(
                            onClick = { onDisassociate(device) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_delete_outline_24),
                                stringResource(R.string.content_description_delete_icon)
                            )
                        }
                    }
                },
                onClick = { onConnect(device) }
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            // Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}


@Composable
fun SettingsRowIcon(painter: Painter, iconColor: Color? = null, backgroundColor: Color? = null) {
    var mod = Modifier.size(40.dp)
    backgroundColor?.let {
        mod = mod.background(backgroundColor, CircleShape)
    }

    Box(
        modifier = mod,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor ?: LocalContentColor.current
        )
    }
}

@Composable
fun DeviceRainbowIcon(painter: Painter, address: String) {
    val iconFgColors = LocalResources.current.getIntArray(R.array.bt_icon_fg_colors)
    val iconBgColors = LocalResources.current.getIntArray(R.array.bt_icon_bg_colors)
    val index = abs(address.uppercase().hashCode() % iconBgColors.size)
    SettingsRowIcon(painter, Color(iconFgColors[index]), Color(iconBgColors[index]))
}

@Preview
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
private fun AssociatedDevicesListPreview() {
    AlphaCompanionTheme(darkTheme = true, dynamicColor = true, customFont = false) {
        Scaffold {
            AssociatedDevicesList(
                associatedDevices = listOf(
                    AssociatedDeviceCompat(
                        id = 2,
                        address = "E8:BB:CC:DD:EE:FF",
                        name = "Green Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 6,
                        address = "AA:BB:CC:DD:EE:FF",
                        name = "Purple Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 5,
                        address = "AA:BB:CC:DD:EE:BC",
                        name = "Pink Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 3,
                        address = "AA:BB:CC:DD:ED:00",
                        name = "Yellow Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 4,
                        address = "11:AA:BB:CC:DD:EE",
                        name = "Orange Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 7,
                        address = "11:AA:BB:CC:DD:EA",
                        name = "Teal Device",
                        device = null
                    ),
                    AssociatedDeviceCompat(
                        id = 1,
                        address = "A1:BC:DB:6C:5D:EA",
                        name = "Read Device",
                        device = null
                    ),
                ),
                onConnect = {},
                onDisassociate = {},
                deviceManager = null,
                onRequestDeviceAssociation = {
                    IntentSender.readIntentSenderOrNullFromParcel(null)
                },
                onDeviceAssociated = {}
            )
        }
    }
}