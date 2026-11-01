package com.yes365.atask.ui.components.shared

import com.yes365.atask.ui.i18n.t
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yes365.atask.domain.model.BottomBarSlot
import com.yes365.atask.domain.model.BottomBarSlotType
import com.yes365.atask.domain.model.CustomList
import com.yes365.atask.domain.model.Project
import com.yes365.atask.util.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBarSlotEditor(
    currentSlot: BottomBarSlot,
    slotIndex: Int,
    projects: List<Project>,
    customLists: List<CustomList>,
    onSave: (BottomBarSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(currentSlot.type) }
    var selectedReferenceId by remember { mutableStateOf(currentSlot.referenceId) }
    var selectedIconKey by remember { mutableStateOf(currentSlot.iconKey) }
    var selectedColorHex by remember { mutableStateOf(currentSlot.colorHex) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val typeOptions = listOf(
        BottomBarSlotType.TODAY to t("Today"),
        BottomBarSlotType.UPCOMING to t("Upcoming"),
        BottomBarSlotType.CALENDAR to t("Calendar"),
        BottomBarSlotType.ANYTIME to t("Anytime"),
        BottomBarSlotType.PROJECT to t("Project"),
        BottomBarSlotType.CUSTOM_LIST to t("Custom List"),
    )

    val isValid = when (selectedType) {
        BottomBarSlotType.TODAY, BottomBarSlotType.UPCOMING, BottomBarSlotType.CALENDAR,
        BottomBarSlotType.ANYTIME -> true
        BottomBarSlotType.PROJECT -> selectedReferenceId.isNotEmpty() && projects.any { it.id.toString() == selectedReferenceId }
        BottomBarSlotType.CUSTOM_LIST -> selectedReferenceId.isNotEmpty() && customLists.any { it.id == selectedReferenceId }
    }

    // A sheet from the bottom edge, full width, like the project editor.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "${t("Bottom Bar Slot")} ${slotIndex + 1}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            // The lists inside have a fixed maximum height, so a short screen scrolls the whole sheet.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Type dropdown
                Text(
                    t("View type"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it },
                ) {
                    TextField(
                        value = typeOptions.first { it.first == selectedType }.second,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                    ) {
                        typeOptions.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    if (type != selectedType) {
                                        selectedType = type
                                        selectedReferenceId = ""
                                        selectedIconKey = ""
                                        selectedColorHex = ""
                                    }
                                    typeDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                // Reference picker for project/custom list
                if (selectedType == BottomBarSlotType.PROJECT && projects.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        t("Select project"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        items(projects, key = { it.id }, contentType = { "project" }) { project ->
                            val isSelected = selectedReferenceId == project.id.toString()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReferenceId = project.id.toString() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    project.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = t("Selected"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedType == BottomBarSlotType.CUSTOM_LIST && customLists.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        t("Select list"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        items(customLists, key = { it.id }, contentType = { "list" }) { list ->
                            val isSelected = selectedReferenceId == list.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReferenceId = list.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    list.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = t("Selected"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Icon and colour (only for project/custom list: a smart list keeps its own)
                if (selectedType == BottomBarSlotType.PROJECT || selectedType == BottomBarSlotType.CUSTOM_LIST) {
                    val chosenColor = parseHexColor(selectedColorHex)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        t("Choose icon"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    IconPicker(
                        selectedKey = selectedIconKey,
                        onSelect = { selectedIconKey = it },
                        selectedTint = chosenColor,
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        t("Choose colour"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    IconColorPicker(
                        selectedHex = selectedColorHex,
                        onSelect = { selectedColorHex = it },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(t("Cancel")) }
                TextButton(
                    onClick = {
                        onSave(
                            BottomBarSlot(
                                type = selectedType,
                                referenceId = when (selectedType) {
                                    BottomBarSlotType.TODAY, BottomBarSlotType.UPCOMING, BottomBarSlotType.ANYTIME -> ""
                                    else -> selectedReferenceId
                                },
                                iconKey = when (selectedType) {
                                    BottomBarSlotType.TODAY, BottomBarSlotType.UPCOMING, BottomBarSlotType.ANYTIME -> ""
                                    else -> selectedIconKey
                                },
                                colorHex = when (selectedType) {
                                    BottomBarSlotType.TODAY, BottomBarSlotType.UPCOMING, BottomBarSlotType.ANYTIME -> ""
                                    else -> selectedColorHex
                                },
                            ),
                        )
                    },
                    enabled = isValid,
                ) {
                    Text(t("Save"))
                }
            }
        }
    }
}
