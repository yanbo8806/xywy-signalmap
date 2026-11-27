package com.xywy.signalmap.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xywy.signalmap.MainUiState
import com.xywy.signalmap.EngineeringLayerOptionUiState
import com.xywy.signalmap.SimPreferenceUiState
import com.xywy.signalmap.domain.model.EngineeringLayerBinding
import com.xywy.signalmap.domain.model.EngineeringLayerBindingOptionPolicy
import com.xywy.signalmap.domain.model.EngineeringLayerBindingMode
import com.xywy.signalmap.domain.model.LayerPointIconCatalog
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.TestLogOrigin
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.isRaster
import com.xywy.signalmap.ui.ImportUiText
import com.xywy.signalmap.ui.ProfileUiText
import com.xywy.signalmap.ui.components.CountBadge
import com.xywy.signalmap.ui.components.IconTile
import com.xywy.signalmap.ui.components.SectionHeader
import com.xywy.signalmap.ui.components.StatusDot
import com.xywy.signalmap.ui.components.TagChip
import com.xywy.signalmap.ui.components.XyCard
import com.xywy.signalmap.ui.theme.SignalColors
import com.xywy.signalmap.ui.theme.xywy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    state: MainUiState,
    paddingValues: PaddingValues,
    onRequestPermissions: () -> Unit,
    importGcj02ToWgs84: Boolean,
    isImporting: Boolean,
    importingFileName: String?,
    onImportGcj02ToWgs84Changed: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onToggleLayerVisibility: (String, Boolean) -> Unit,
    onSetLayerAlwaysRenderAllRecords: (String, Boolean) -> Unit,
    onNearbyRecordLimitChanged: (Int) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onUpdateLayerStyle: (String, Boolean, String?, String?, LayerPointIconPreset, String?, Float?) -> Unit,
    onExportLayerCsv: (String) -> Unit,
    onExportLayerKml: (String) -> Unit,
    onUpdateSimEngineeringBinding: (Int, RadioTechnology, EngineeringLayerBinding) -> Unit,
    onToggleTestLogVisibility: (String, Boolean) -> Unit,
    onDeleteTestLog: (String) -> Unit,
    onShareTestLog: (String) -> Unit,
    onReplayTestLog: (String) -> Unit,
    onTilePresetSelected: (String) -> Unit,
    onSaveCustomTileSource: (String?, String, String, Int, TileCoordinateSystem) -> Unit,
    onDeleteCustomTileSource: (String) -> Unit,
) {
    var editingLayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingLayer = state.layers.firstOrNull { it.id == editingLayerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProfileHeader()
        }

        item {
            PermissionImportCard(
                locationGranted = state.locationPermissionGranted,
                phoneGranted = state.phonePermissionGranted,
                importGcj02ToWgs84 = importGcj02ToWgs84,
                isImporting = isImporting,
                importingFileName = importingFileName,
                onImportGcj02ToWgs84Changed = onImportGcj02ToWgs84Changed,
                onRequestPermissions = onRequestPermissions,
                onImportClick = onImportClick,
            )
        }

        item {
            TilePresetChooser(
                selected = state.tilePreset,
                presets = state.tilePresets,
                onTilePresetSelected = onTilePresetSelected,
                onSaveCustomTileSource = onSaveCustomTileSource,
                onDeleteCustomTileSource = onDeleteCustomTileSource,
            )
        }

        item {
            SimEngineeringBindingCard(
                simPreferences = state.simPreferenceStates,
                options = state.engineeringLayerOptions,
                onUpdateBinding = onUpdateSimEngineeringBinding,
            )
        }

        item {
            SectionHeader(
                title = ProfileUiText.importedLayersTitle,
                icon = Icons.Filled.Layers,
                accent = SignalColors.NR,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                trailing = { CountBadge(count = state.layers.size, color = SignalColors.NR) },
            )
        }

        item {
            NearbyRecordLimitCard(state.nearbyRecordLimit, onNearbyRecordLimitChanged)
        }

        if (state.layers.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "还没有导入的图层，点击上方“导入文件”开始。",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.layers, key = { it.id }) { layer ->
                LayerCard(
                    layer = layer,
                    nearbyRecordLimit = state.nearbyRecordLimit,
                    onToggleAlwaysRenderAll = { onSetLayerAlwaysRenderAllRecords(layer.id, it) },
                    onToggleVisibility = { onToggleLayerVisibility(layer.id, it) },
                    onEdit = { editingLayerId = layer.id },
                    onDelete = { onDeleteLayer(layer.id) },
                    onExportCsv = { onExportLayerCsv(layer.id) },
                    onExportKml = { onExportLayerKml(layer.id) },
                )
            }
        }

        item {
            SectionHeader(
                title = "测试记录",
                icon = Icons.Filled.Map,
                accent = SignalColors.LTE,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                trailing = { CountBadge(count = state.testLogs.size, color = SignalColors.LTE) },
            )
        }

        if (state.testLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "还没有测试记录，地图页开始测试后会在这里显示。",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.testLogs, key = { it.id }) { log ->
                TestLogCard(
                    log = log,
                    onToggleVisibility = { onToggleTestLogVisibility(log.id, it) },
                    onDelete = { onDeleteTestLog(log.id) },
                    onShare = { onShareTestLog(log.id) },
                    onReplay = { onReplayTestLog(log.id) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    editingLayer?.let { layer ->
        LayerAppearanceDialog(
            layer = layer,
            onDismiss = { editingLayerId = null },
            onConfirm = { showLabels, labelTextColor, labelHaloColor, pointIconPreset, rasterColorHex, rasterOpacity ->
                onUpdateLayerStyle(
                    layer.id,
                    showLabels,
                    labelTextColor,
                    labelHaloColor,
                    pointIconPreset,
                    rasterColorHex,
                    rasterOpacity,
                )
                editingLayerId = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TestLogCard(
    log: TestLogMetadata,
    onToggleVisibility: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onReplay: () -> Unit,
) {
    XyCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = Icons.Filled.Map,
                    tint = if (log.isVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    tileSize = 36.dp,
                    iconSize = 19.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        log.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${log.sampleCount} 个点 · ${TEST_LOG_TIME_FORMAT.format(Date(log.startedAtMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TagChip(
                    text = if (log.isVisible) "显示中" else "已隐藏",
                    tint = if (log.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildString {
                        append(if (log.origin == TestLogOrigin.IMPORTED) "来源：导入" else "来源：录制")
                        log.sourceFileName?.takeIf { it.isNotBlank() }?.let {
                            append(" · ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }

            TextButton(onClick = onReplay) { Text("回放 / 事件 / 统计") }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    StatusDot(
                        color = if (log.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (log.isVisible) "地图显示中" else "地图已隐藏",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = log.isVisible,
                        onCheckedChange = onToggleVisibility,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "分享测试记录",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除测试记录",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    val xywy = MaterialTheme.xywy
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(brush = xywy.heroBrush)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(17.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                        tint = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "网优小区图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = xywy.heroText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    ProfileUiText.appSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = xywy.heroSubText,
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            ) {
                Text(
                    "PRO",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionImportCard(
    locationGranted: Boolean,
    phoneGranted: Boolean,
    importGcj02ToWgs84: Boolean,
    isImporting: Boolean,
    importingFileName: String?,
    onImportGcj02ToWgs84Changed: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onImportClick: () -> Unit,
) {
    val xywy = MaterialTheme.xywy
    XyCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = ProfileUiText.permissionImportTitle,
                icon = Icons.Filled.Security,
                accent = MaterialTheme.colorScheme.secondary,
            )

            PermissionRow(
                icon = Icons.Filled.Key,
                label = ProfileUiText.locationPermissionLabel,
                granted = locationGranted,
            )
            PermissionRow(
                icon = Icons.Filled.Security,
                label = ProfileUiText.phonePermissionLabel,
                granted = phoneGranted,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalButton(
                    onClick = onRequestPermissions,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ProfileUiText.requestPermissionAction, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onImportClick,
                    shape = MaterialTheme.shapes.small,
                    enabled = !isImporting,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ImportUiText.actionLabel(isImporting), fontWeight = FontWeight.Medium)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = if (isImporting) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isImporting) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isImporting) Icons.Filled.FileUpload else Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isImporting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = if (isImporting) {
                                ImportUiText.loadingHint(importingFileName)
                            } else {
                                ImportUiText.idleHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isImporting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isImporting) FontWeight.Medium else FontWeight.Normal,
                        )
                        if (isImporting) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ProfileUiText.importCoordinateToggleTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            ProfileUiText.importCoordinateToggleSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = importGcj02ToWgs84,
                        onCheckedChange = onImportGcj02ToWgs84Changed,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimEngineeringBindingCard(
    simPreferences: List<SimPreferenceUiState>,
    options: List<EngineeringLayerOptionUiState>,
    onUpdateBinding: (Int, RadioTechnology, EngineeringLayerBinding) -> Unit,
) {
    XyCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = ProfileUiText.simBindingTitle,
                subtitle = ProfileUiText.simBindingHint,
                icon = Icons.Filled.Layers,
                accent = MaterialTheme.colorScheme.primary,
            )

            if (options.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = ProfileUiText.noEngineeringLayerHint,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            simPreferences.forEach { preference ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = preference.label.ifBlank { "SIM${preference.slotIndex + 1}" },
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        BindingDropdownField(
                            label = ProfileUiText.nrBindingLabel,
                            current = preference.nrBinding,
                            options = EngineeringLayerBindingOptionPolicy.optionsFor(
                                options = options,
                                technology = RadioTechnology.NR,
                            ),
                            onSelected = { binding ->
                                onUpdateBinding(preference.slotIndex, RadioTechnology.NR, binding)
                            },
                        )
                        BindingDropdownField(
                            label = ProfileUiText.lteBindingLabel,
                            current = preference.lteBinding,
                            options = EngineeringLayerBindingOptionPolicy.optionsFor(
                                options = options,
                                technology = RadioTechnology.LTE,
                            ),
                            onSelected = { binding ->
                                onUpdateBinding(preference.slotIndex, RadioTechnology.LTE, binding)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BindingDropdownField(
    label: String,
    current: EngineeringLayerBinding,
    options: List<EngineeringLayerOptionUiState>,
    onSelected: (EngineeringLayerBinding) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(
                    type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                )
                .fillMaxWidth(),
            value = bindingLabel(current, options),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(ProfileUiText.autoLayerBindingLabel) },
                onClick = {
                    expanded = false
                    onSelected(EngineeringLayerBinding.Auto)
                },
            )
            DropdownMenuItem(
                text = { Text(ProfileUiText.disabledLayerBindingLabel) },
                onClick = {
                    expanded = false
                    onSelected(EngineeringLayerBinding.Disabled)
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelected(EngineeringLayerBinding.specificLayer(option.layerId))
                    },
                )
            }
        }
    }
}

private fun bindingLabel(
    binding: EngineeringLayerBinding,
    options: List<EngineeringLayerOptionUiState>,
): String = when (binding.mode) {
    EngineeringLayerBindingMode.AUTO -> ProfileUiText.autoLayerBindingLabel
    EngineeringLayerBindingMode.DISABLED -> ProfileUiText.disabledLayerBindingLabel
    EngineeringLayerBindingMode.SPECIFIC_LAYER -> options.firstOrNull { it.layerId == binding.layerId }?.name
        ?: ProfileUiText.autoLayerBindingLabel
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
) {
    val xywy = MaterialTheme.xywy
    val tint = if (granted) xywy.success else MaterialTheme.colorScheme.error
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = tint.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint,
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = tint,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (granted) "已授权" else "未授权",
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun NearbyRecordLimitCard(limit: Int, onSave: (Int) -> Unit) {
    var input by rememberSaveable(limit) { mutableStateOf(limit.toString()) }
    val parsed = input.toIntOrNull()?.takeIf { it > 0 }
    XyCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("中心周边显示数量", style = MaterialTheme.typography.titleSmall)
            Text("所有点、面及工参图层共用；每个图层各显示此数量，默认 200 条。开启该图层“全部显示”后不受限制。TIFF 栅格不按条数限制。",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("每个图层的记录数") },
                singleLine = true, isError = parsed == null,
                supportingText = { Text(if (parsed == null) "请输入大于 0 的整数" else "当前已保存：每层 $limit 条") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(onClick = { parsed?.let(onSave) }, enabled = parsed != null && parsed != limit) {
                Text("保存并应用到所有图层")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LayerCard(
    layer: com.xywy.signalmap.domain.model.LayerMetadata,
    nearbyRecordLimit: Int = 200,
    onToggleAlwaysRenderAll: (Boolean) -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportCsv: () -> Unit,
    onExportKml: () -> Unit,
) {
    XyCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = Icons.Filled.Layers,
                    tint = if (layer.isVisible) SignalColors.NR else MaterialTheme.colorScheme.onSurfaceVariant,
                    tileSize = 36.dp,
                    iconSize = 19.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        layer.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (layer.isRaster) {
                            "栅格 ${layer.rasterInfo?.width}×${layer.rasterInfo?.height} · ${layer.sourceFileName}"
                        } else {
                            "${layer.recordCount}${ProfileUiText.layerRecordSummarySeparator}${layer.sourceFileName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                TagChip(
                    text = if (layer.isVisible) "显示中" else "已隐藏",
                    tint = if (layer.isVisible) SignalColors.NR else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    StatusDot(
                        color = if (layer.isVisible) SignalColors.NR else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (layer.isVisible) "地图显示中" else "地图已隐藏",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = layer.isVisible,
                        onCheckedChange = onToggleVisibility,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (layer.userEditable) {
                        TextButton(onClick = onExportCsv, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("CSV", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onExportKml, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("KML", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "编辑图层样式",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除图层",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (!layer.isRaster) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "全部显示",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                if (layer.alwaysRenderAllRecords) "显示本图层全部记录，数据较多时可能卡顿。"
                                else "仅显示地图中心最近的 $nearbyRecordLimit 条，移动地图后更新。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = layer.alwaysRenderAllRecords, onCheckedChange = onToggleAlwaysRenderAll)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "TIFF 栅格图层",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "当前颜色：${layer.rasterInfo?.colorHex.orEmpty()} · 透明度：${((layer.rasterInfo?.opacity ?: 0f) * 100).toInt()}% · 有效格点：${layer.rasterInfo?.validPixelCount ?: layer.recordCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TilePresetChooser(
    selected: TilePreset,
    presets: List<TilePreset>,
    onTilePresetSelected: (String) -> Unit,
    onSaveCustomTileSource: (String?, String, String, Int, TileCoordinateSystem) -> Unit,
    onDeleteCustomTileSource: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<TilePreset?>(null) }
    var showTileSourceDialog by remember { mutableStateOf(false) }
    val selectablePresets = presets.ifEmpty { listOf(selected) }
    val selectedPreset = selectablePresets.firstOrNull { it.key == selected.key } ?: selectablePresets.first()

    XyCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = ProfileUiText.tilePresetTitle,
                subtitle = "内置 3 种常用底图，也可以新增或编辑自定义瓦片服务。",
                icon = Icons.Filled.Map,
                accent = MaterialTheme.colorScheme.tertiary,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            editingPreset = null
                            showTileSourceDialog = true
                        },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("新增", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(
                            type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                            enabled = true,
                        )
                        .fillMaxWidth(),
                    value = selectedPreset.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(ProfileUiText.tilePresetFieldLabel) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    selectablePresets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.title)
                                    if (preset.isCustom) {
                                        Text(
                                            "坐标系 ${preset.coordinateSystem.name} · ${preset.tileSize}px",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                onTilePresetSelected(preset.key)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }

            selectedPreset.note?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val customPresets = selectablePresets.filter { it.isCustom }
            if (customPresets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "自定义底图",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    customPresets.forEach { preset ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.title, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        preset.tiles.firstOrNull().orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                                IconButton(onClick = {
                                    editingPreset = preset
                                    showTileSourceDialog = true
                                }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "编辑",
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { onDeleteCustomTileSource(preset.key) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTileSourceDialog) {
        CustomTileSourceDialog(
            preset = editingPreset,
            onDismiss = {
                editingPreset = null
                showTileSourceDialog = false
            },
            onSave = { key, title, url, tileSize, coordinateSystem ->
                onSaveCustomTileSource(key, title, url, tileSize, coordinateSystem)
                editingPreset = null
                showTileSourceDialog = false
            },
        )
    }
}

@Composable
private fun CustomTileSourceDialog(
    preset: TilePreset?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, Int, TileCoordinateSystem) -> Unit,
) {
    var title by rememberSaveable(preset?.key) { mutableStateOf(preset?.title.orEmpty()) }
    var url by rememberSaveable(preset?.key) { mutableStateOf(preset?.tiles?.firstOrNull().orEmpty()) }
    var tileSize512 by rememberSaveable(preset?.key) { mutableStateOf(preset?.tileSize == 512) }
    var useGcj02 by rememberSaveable(preset?.key) { mutableStateOf(preset?.coordinateSystem != TileCoordinateSystem.WGS84) }
    val normalizedUrl = url.trim()
    val urlHasSchema = normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")
    val urlHasPlaceholders = listOf("{z}", "{x}", "{y}").all { it in normalizedUrl }
    val canSave = urlHasSchema && urlHasPlaceholders

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset == null) "新增自定义底图" else "编辑自定义底图") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("名称") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("瓦片 URL 模板（需包含 {z}/{x}/{y}）") },
                    supportingText = {
                        if (url.isNotBlank() && !canSave) {
                            Text("URL 需以 http(s) 开头，并包含 {z}、{x}、{y}")
                        }
                    },
                    isError = url.isNotBlank() && !canSave,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                ToggleSettingRow(
                    title = "512 瓦片",
                    checked = tileSize512,
                    onCheckedChange = { tileSize512 = it },
                )
                ToggleSettingRow(
                    title = "GCJ02 坐标",
                    summary = "关闭后按 WGS84 处理。",
                    checked = useGcj02,
                    onCheckedChange = { useGcj02 = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        preset?.key,
                        title,
                        url,
                        if (tileSize512) 512 else 256,
                        if (useGcj02) TileCoordinateSystem.GCJ02 else TileCoordinateSystem.WGS84,
                    )
                },
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ToggleSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                summary?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

private val TEST_LOG_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
