package com.shuaiqiu.fuckets100.component.material

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@Composable
fun expressiveTopAppBarColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor: Color = containerColor,
): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = containerColor,
    scrolledContainerColor = scrolledContainerColor,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    when {
        onLongClick != null -> Card(
            modifier = modifier
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                ),
            shape = shape,
            colors = colors,
            content = content,
        )

        onClick != null -> Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            content = content,
        )

        else -> Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            content = content,
        )
    }
}

@Composable
fun TopBarBackButton(
    onClick: () -> Unit,
    contentDescription: String = "返回",
) {
    IconButton(
        onClick = onClick,
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription)
    }
}

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun ExpressiveToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = AlertDialogDefaults.containerColor,
    )
}

@Composable
fun SegmentedColumn(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    if (items.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            items.forEach { it() }
        }
    }
}

@Composable
fun ExpressiveSegmentedListItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = expressiveSegmentedColors(),
) {
    SegmentedListItem(
        onClick = onClick ?: {},
        enabled = enabled,
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = modifier.fillMaxWidth(),
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        content = headlineContent,
    )
}

@Composable
fun ExpressiveSegmentedSwitchItem(
    index: Int,
    count: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    ExpressiveSegmentedListItem(
        index = index,
        count = count,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun ExpressiveSegmentedDropdownItem(
    index: Int,
    count: Int,
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    androidx.compose.foundation.layout.Box(modifier) {
        ExpressiveSegmentedListItem(
            index = index,
            count = count,
            onClick = { expanded = true },
            enabled = enabled && items.isNotEmpty(),
            headlineContent = { Text(title) },
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = {
                Text(
                    text = items.getOrNull(safeIndex).orEmpty(),
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEachIndexed { itemIndex, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onItemSelected(itemIndex)
                        expanded = false
                    },
                    leadingIcon = if (itemIndex == safeIndex) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun expressiveSegmentedColors(): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
