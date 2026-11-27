package com.xywy.signalmap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xywy.signalmap.ImportDialogUiState
import com.xywy.signalmap.ImportFieldKey
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode

@Composable
fun ImportConfigDialog(
    state: ImportDialogUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSheetSelected: (String) -> Unit,
    onRenderModeSelected: (ImportedLayerRenderMode) -> Unit,
    onFieldSelected: (ImportFieldKey, String?) -> Unit,
    onToggleDetailHeader: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("导入配置", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    ),
                ) {
                    Text(
                        text = state.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                SectionLabel("工作表")
                ImportDropdownField(
                    label = "选择工作表",
                    value = state.selectedSheetName,
                    options = state.sheets.map { it.name },
                    onSelected = { value -> value?.let(onSheetSelected) },
                    allowEmpty = false,
                )

                SectionLabel("渲染模式")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImportedLayerRenderMode.entries
                        .filterNot { it == ImportedLayerRenderMode.USER }
                        .forEach { mode ->
                            val isSelected = state.renderMode == mode
                            val modeLabel = when (mode) {
                                ImportedLayerRenderMode.AUTO -> "自动"
                                ImportedLayerRenderMode.POINT -> "图标点"
                                ImportedLayerRenderMode.SECTOR -> "扇形小区"
                                ImportedLayerRenderMode.USER -> "手动"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onRenderModeSelected(mode) },
                                label = {
                                    Text(
                                        modeLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    selectedBorderWidth = 1.5.dp,
                                ),
                            )
                        }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SectionLabel("字段映射")
                ImportDropdownField("经度列", state.fieldMapping.longitudeHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.Longitude, it) }, false)
                ImportDropdownField("纬度列", state.fieldMapping.latitudeHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.Latitude, it) }, false)
                ImportDropdownField("方位角列", state.fieldMapping.azimuthHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.Azimuth, it) })
                ImportDropdownField("名称列", state.fieldMapping.nameHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.Name, it) })
                ImportDropdownField("基站ID列", state.fieldMapping.siteIdHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.SiteId, it) })
                ImportDropdownField("小区ID列", state.fieldMapping.cellIdHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.CellId, it) })
                ImportDropdownField("PCI列", state.fieldMapping.pciHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.Pci, it) })
                ImportDropdownField("站高列", state.fieldMapping.antennaHeightHeader, state.availableHeaders, { onFieldSelected(ImportFieldKey.AntennaHeight, it) })

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SectionLabel("详情优先字段")
                Text(
                    "勾选后，长按查看详情时这些字段会排在前面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.availableHeaders.forEach { header ->
                    val checked = header in state.detailHeaders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleDetailHeader(header) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            header,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.canConfirm) {
                Text("导入", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDropdownField(
    label: String,
    value: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
    allowEmpty: Boolean = true,
) {
    var expanded by remember(label, value, options) { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            singleLine = true,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (allowEmpty) {
                DropdownMenuItem(
                    text = { Text("不设置", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        expanded = false
                        onSelected(null)
                    },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}
