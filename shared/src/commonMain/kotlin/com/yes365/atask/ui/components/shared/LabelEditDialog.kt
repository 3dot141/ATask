package com.yes365.atask.ui.components.shared

import com.yes365.atask.ui.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yes365.atask.domain.model.Label
import com.yes365.atask.util.PRESET_COLORS
import com.yes365.atask.util.parseHexColor

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LabelEditDialog(
    label: Label? = null,
    onSave: (name: String, hexColor: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = label != null
    var name by remember { mutableStateOf(label?.title ?: "") }
    var selectedColor by remember {
        mutableStateOf(
            label?.hexColor?.ifBlank { null } ?: PRESET_COLORS.first()
        )
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
                if (isEdit) t("Edit Label") else t("Create Label"),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(t("Name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = t("Color"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESET_COLORS.forEach { hex ->
                    val color = parseHexColor(hex) ?: Color.Gray
                    val normalizedSelected = selectedColor.let {
                        if (it.startsWith("#")) it else "#$it"
                    }
                    val isSelected = hex == normalizedSelected

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape,
                                )
                                else Modifier
                            )
                            .clickable { selectedColor = hex },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(t("Cancel")) }
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name.trim(), selectedColor)
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text(if (isEdit) t("Save") else t("Create"))
                }
            }
        }
    }
}
