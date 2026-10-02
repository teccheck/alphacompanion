package xyz.teccheck.alphacompanion.ui.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import xyz.teccheck.alphacompanion.R
import xyz.teccheck.alphacompanion.model.AssociatedDeviceCompat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssociatedDevicesList(
    associatedDevices: List<AssociatedDeviceCompat>,
    onConnect: (AssociatedDeviceCompat) -> Unit,
    onDisassociate: (AssociatedDeviceCompat) -> Unit,
) {
    val colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val count = associatedDevices.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
        }

        itemsIndexed(associatedDevices) { index, device ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                colors = colors,
                content = { Text(text = device.name) },
                supportingContent = { Text(text = device.address.uppercase()) },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.baseline_linked_camera_24),
                        contentDescription = null
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onDisassociate(device) }) {
                        Icon(
                            painterResource(R.drawable.baseline_delete_outline_24),
                            contentDescription = null
                        )
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
