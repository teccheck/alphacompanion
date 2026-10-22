package xyz.teccheck.alphacompanion.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.teccheck.alphacompanion.R
import xyz.teccheck.alphacompanion.db.SonyCamera
import kotlin.math.abs

@Composable
fun PairedCamerasList(
    cameras: List<SonyCamera>,
    onPairNew: () -> Unit,
    onClick: (SonyCamera) -> Unit,
    onRemove: (SonyCamera) -> Unit
) {
    val count = cameras.size
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
                onClick = onPairNew,
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

        itemsIndexed(cameras) { index, camera ->
            CameraListItem(colors, index, count, camera, onClick, onRemove)
        }

        item {
            Spacer(Modifier.height(16.dp))
            // Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

@Composable
fun CameraListItem(
    colors: ListItemColors,
    index: Int,
    count: Int,
    camera: SonyCamera,
    onClick: (SonyCamera) -> Unit,
    onRemove: (SonyCamera) -> Unit,
) {
    val content = String.format("%s", camera.name)
    val supportingContent = String.format(
        "%s | %s | %s",
        camera.macAddress.uppercase(),
        camera.modelVersion.toHexString(),
        camera.modeCode
    )

    SegmentedListItem(
        modifier = Modifier.defaultMinSize(Dp.Unspecified, 72.dp),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        content = { Text(text = content) },
        supportingContent = { Text(text = supportingContent) },
        leadingContent = {
            DeviceRainbowIcon(
                painterResource(R.drawable.baseline_linked_camera_24),
                camera.macAddress
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
                    onClick = { onRemove(camera) },
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
        onClick = { onClick(camera) }
    )
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
fun PairedCamerasListPreview() {
    val cameras = listOf(
        SonyCamera("E8:BB:CC:DD:EE:FF", 1, "Green Camera", 0x6400, "E1"),
        SonyCamera("AA:BB:CC:DD:EE:FF", 2, "Purple Camera", 0x6500, "--"),
        SonyCamera("AA:BB:CC:DD:ED:00", 3, "Yellow Camera", 0x6600, "E1"),
        SonyCamera("11:AA:BB:CC:DD:EE", 4, "Orange Camera", 0x6700, "E1"),
        SonyCamera("11:AA:BB:CC:DD:EA", 5, "Teal Camera", 0x6000, "--"),
        SonyCamera("A1:BC:DB:6C:5D:EA", 6, "Red Camera", 0x6200, "E1"),
    )

    PairedCamerasList(cameras, {}, {}, {})
}
